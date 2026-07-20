package uk.ac.ebi.uniprot.urml.engine.drools.compiler;

import org.drools.compiler.lang.descr.ImportDescr;
import org.drools.compiler.lang.descr.PackageDescr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.uniprot.urml.rules.*;
import uk.ac.ebi.uniprot.urml.core.model.facts.reflection.FactModelReflectionException;
import uk.ac.ebi.uniprot.urml.core.xml.readers.URMLRuleReader;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.ac.ebi.uniprot.urml.engine.drools.compiler.DrlAssertions.*;

/**
 * Unit tests for {@link URMLToDroolsTranspiler}.
 *
 * <p>Tests build the URML rule model programmatically, transpile it, and compare the
 * generated DRL against an expected fragment using the assertions in {@link DrlAssertions}.</p>
 *
 * @author Vishal Joshi, Muhammad Aditya Hilmy
 */
class URMLToDroolsTranspilerTest {

    private static final String FACT_NAMESPACE = "http://uniprot.org/urml/facts";
    private static final QName PROTEIN = new QName(FACT_NAMESPACE, "Protein", "fact");
    private static final QName ORGANISM = new QName(FACT_NAMESPACE, "Organism", "fact");
    private static final QName PROTEIN_SIGNATURE = new QName(FACT_NAMESPACE, "ProteinSignature", "fact");
    private static final QName PROTEIN_ANNOTATION = new QName(FACT_NAMESPACE, "ProteinAnnotation", "fact");
    private static final String PROCEDURES_URI = "java://uk.ac.ebi.uniprot.procedures.TestProcedure";

    @TempDir
    Path outputDirectory;
    private URMLToDroolsTranspiler transpiler;

    /**
     * Transpiling a real URML file (ARBA00037344) whose field values contain an escape
     * sequence applies the sanitizer to every fact field value.
     */
    @Test
    void shouldBeAbleToCorrectRuleWithEscapeSequence() throws JAXBException, IOException {
        // Given that the URML rule ARBA00037344 contains a field value with an escape sequence
        // ('Thor\d4EBP') which the sanitizer rewrites ('Thord4EBP')
        Path transpiled_rules = outputDirectory.resolve("transpiled_rules.drl");
        OutputStream outputStream = new FileOutputStream(transpiled_rules.toFile());
        URMLRuleReader ruleReader = new URMLRuleReader();
        Rules rules = ruleReader.read(this.getClass().getResourceAsStream("/rules/ARBA00037344_urml.xml"));
        CompositeSanitizer mockedSanitizer = mock(CompositeSanitizer.class);
        when(mockedSanitizer.sanitize("comment.function")).thenReturn("comment.function");
        when(mockedSanitizer.sanitize("Consistently activates both the downstream target Thor\\d4EBP and" +
                " the feedback control target InR"))
                .thenReturn("Consistently activates both the downstream target Thord4EBP" +
                        " and the feedback control target InR");
        transpiler = new URMLToDroolsTranspiler(outputStream, mockedSanitizer);

        // When the rule is transpiled to DRL
        transpiler.translate(rules);

        // Then the sanitizer must have been applied exactly twice (once per fact field value)
        verify(mockedSanitizer, times(2)).sanitize(anyString());
    }

    /**
     * A rules document without rules produces only the package declaration and the imports.
     */
    @Test
    void shouldGenerateHeaderAndImports() {
        // Given that a rules document named "org.uniprot.test" contains no rules:
        // <rules name="org.uniprot.test"/>
        Rules rules = rules();

        // When the document is transpiled to DRL
        String drl = transpile(rules);

        // Then the output must consist of only the package declaration and the two import statements
        PackageDescr parsed = parseDrl(drl);
        assertEquals("org.uniprot.test", parsed.getName());
        assertEquals(List.of("org.uniprot.urml.facts.*", "java.util.List"),
                parsed.getImports().stream().map(ImportDescr::getTarget).collect(Collectors.toList()));
        assertTrue(parsed.getRules().isEmpty());
    }

    /**
     * A minimal rule (one bound condition, one create action) is transpiled end-to-end,
     * pinning the condition and consequence of a full transpile (the package/import header
     * is pinned by {@link #shouldGenerateHeaderAndImports()}).
     */
    @Test
    void shouldTranspileSimpleRuleEndToEnd() {
        /*
         * Given that a rule binds a Protein fact and creates a ProteinAnnotation wired to that protein,
         * an inline evidence literal and two field values:
         * <rule id="RULE_1">
         *   <conditions><AND><condition on="fact:Protein" bind="protein"/></AND></conditions>
         *   <actions>
         *     <action type="create" with="protein evidence:'RULE_1'">
         *       <fact type="fact:ProteinAnnotation">
         *         <field attribute="type">keyword</field>
         *         <field attribute="value">DNA-binding</field>
         *       </fact>
         *     </action>
         *   </actions>
         * </rule>
         */
        Condition protein = condition(PROTEIN, "protein");
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation",
                field("type", "keyword"), field("value", "DNA-binding"));
        Action create = action(ActionType.CREATE, List.of("protein", "evidence:'RULE_1'"), annotation);
        Rule rule = rule("RULE_1", and(protein), actions(create));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule));

        // Then the bound condition must use ':=', the created fact must be inserted logically and
        // built from the '$'-prefixed protein reference, the single-quoted evidence literal must
        // become double-quoted, and field values must be quoted strings
        assertConditionsEquivalent("$protein := Protein()", drl);
        assertConsequencesEquivalent("""
                ProteinAnnotation $annotation = ProteinAnnotation.builder().withProtein($protein).withEvidence("RULE_1").withType("keyword").withValue("DNA-binding").build();
                insertLogical($annotation);
                """, drl);
    }

    /**
     * A rule extending another rule emits 'extends "..."', and a procedural rule carries
     * 'salience -10' while a plain rule does not.
     */
    @Test
    void shouldTranspileExtendsAndProceduralRule() {
        // Given that a procedural rule RULE_2 extends another rule PARENT_RULE, while RULE_3 is plain:
        // <rule id="RULE_2" extends="PARENT_RULE" procedural="true"> ... </rule>
        // <rule id="RULE_3"> ... </rule>
        Rule parent = rule("PARENT_RULE", and(condition(PROTEIN, "protein")), actions());
        Rule extending = rule("RULE_2", and(condition(PROTEIN, "protein")), actions());
        extending.setExtends(parent);
        extending.setProcedural(true);
        Rule plain = rule("RULE_3", and(condition(PROTEIN, "protein")), actions());

        // When both rules are transpiled to DRL
        String drl = transpile(rules(extending, plain));

        // Then RULE_2 must declare 'extends "PARENT_RULE"' and carry 'salience -10' (procedural rules
        // fire last), while RULE_3 must have no salience attribute
        assertRulesEquivalent("""
                rule "RULE_2" extends "PARENT_RULE"
                salience -10
                when
                  $protein := Protein()
                then
                end

                rule "RULE_3"
                when
                  $protein := Protein()
                then
                end
                """, drl);
    }

    /**
     * Multiple AND condition sets are joined by the 'or' keyword, each set parenthesised.
     */
    @Test
    void shouldTranspileDisjunctiveConditionSetsWithOr() {
        /*
         * Given that the conditions of a rule contain two AND sets (a disjunction):
         * <conditions>
         *   <AND><condition on="fact:Protein" bind="protein"/></AND>
         *   <AND><condition on="fact:Organism" bind="organism"/></AND>
         * </conditions>
         */
        DisjunctiveConditionSet conditions = or(
                and(condition(PROTEIN, "protein")),
                and(condition(ORGANISM, "organism")));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", conditions, actions())));

        // Then the two AND sets must be joined by the 'or' keyword (parenthesisation is presentational;
        // the comparison pins the or-structure, not the redundant outer parentheses)
        assertConditionsEquivalent("$protein := Protein() or $organism := Organism()", drl);
    }

    /**
     * Multiple conditions within one AND set are joined by the 'and' keyword.
     */
    @Test
    void shouldTranspileConjunctiveConditionsWithAnd() {
        /*
         * Given that one AND set contains two conditions:
         * <AND>
         *   <condition on="fact:Protein" bind="protein"/>
         *   <condition on="fact:Organism" bind="organism"/>
         * </AND>
         */
        ConjunctiveConditionSet and = and(condition(PROTEIN, "protein"), condition(ORGANISM, "organism"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and, actions())));

        // Then the conditions must be joined by the 'and' keyword inside a single group
        assertConditionsEquivalent("$protein := Protein() and $organism := Organism()", drl);
    }

    /**
     * A condition without a 'bind' attribute and default exists="true" becomes an
     * 'exists' pattern.
     */
    @Test
    void shouldTranspileUnboundConditionAsExists() {
        // Given that a condition has no 'bind' attribute and 'exists' defaults to true:
        // <condition on="fact:Protein"/>
        Condition condition = condition(PROTEIN, null);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition), actions())));

        // Then the condition must be rendered with the 'exists' quantifier
        assertConditionsEquivalent("exists Protein()", drl);
    }

    /**
     * A condition without a 'bind' attribute and exists="false" becomes a 'not' pattern.
     */
    @Test
    void shouldTranspileNegativeConditionAsNot() {
        // Given that a condition has no 'bind' attribute and exists="false":
        // <condition on="fact:Protein" exists="false"/>
        Condition notProtein = condition(PROTEIN, null);
        notProtein.setExists(false);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(notProtein), actions())));

        // Then the condition must be rendered with the 'not' quantifier
        assertConditionsEquivalent("not Protein()", drl);
    }

    /**
     * A collect condition binds a List accumulated via 'List() from collect (...)'.
     */
    @Test
    void shouldTranspileCollectCondition() {
        // Given that a condition collects all matching facts into a list:
        // <condition on="fact:Protein" bind="proteins" collect="true"/>
        Condition collect = condition(PROTEIN, "proteins");
        collect.setCollect(true);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(collect), actions())));

        // Then the binding must be rendered as a List obtained from a 'collect' accumulation
        assertConditionsEquivalent("$proteins := List() from collect (Protein())", drl);
    }

    /**
     * 'with' and 'of' condition bindings support plain and 'attribute:bindingId' forms,
     * rendered as attribute equalities and 'this' comparisons respectively.
     */
    @Test
    void shouldTranspileWithAndOfBindings() {
        // Given that a condition wires attributes to previously bound facts in both plain and
        // 'attribute:bindingId' forms, and relates the fact to itself via 'of':
        // <condition on="fact:ProteinSignature" bind="sig" with="protein id:p1" of="protein organism:prot"/>
        Condition sig = condition(PROTEIN_SIGNATURE, "sig");
        sig.getWith().addAll(List.of("protein", "id:p1"));
        sig.getOf().addAll(List.of("protein", "organism:prot"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(sig), actions())));

        // Then plain 'with' entries constrain the same-named attribute, colon forms map attribute to
        // binding id, and 'of' entries compare 'this' against the bound fact's attribute (defaulting to
        // the lower-cased fact name when no attribute is given)
        assertConditionsEquivalent("""
                $sig := ProteinSignature(protein == $protein, id == $p1,
                    this == $protein.proteinsignature, this == $prot.organism)
                """, drl);
    }

    /**
     * A 'contains' filter with operator="any" combines values with '||' and applies
     * null-safe navigation to nested attributes.
     */
    @Test
    void shouldTranspileContainsFilterWithAnyOperator() {
        // Given that a condition filters organism lineage ids with a 'contains' constraint using
        // operator="any":
        // <filter on="lineage.ids"><contains operator="any"><value>2759</value><value>10239</value></contains></filter>
        Condition organism = condition(ORGANISM, "organism",
                containsFilter("lineage.ids", LogicalOperator.ANY, "2759", "10239"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(organism), actions())));

        // Then the values must be combined with '||', the nested attribute must use null-safe
        // navigation ('lineage!.ids'), and the whole constraint must be parenthesised
        assertConditionsEquivalent("$organism := Organism((lineage!.ids contains 2759 || lineage!.ids contains 10239))", drl);
    }

    /**
     * A single-value 'contains' filter emits no logical operator separator.
     */
    @Test
    void shouldTranspileContainsFilterWithSingleValue() {
        // Given that a 'contains' constraint holds a single value:
        // <filter on="lineage.ids"><contains operator="all"><value>2759</value></contains></filter>
        Condition organism = condition(ORGANISM, "organism",
                containsFilter("lineage.ids", LogicalOperator.ALL, "2759"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(organism), actions())));

        // Then no logical operator separator must be emitted around the single constraint
        assertConditionsEquivalent("$organism := Organism((lineage!.ids contains 2759))", drl);
    }

    /**
     * A 'contains' filter with operator="all" combines values with '&&'.
     */
    @Test
    void shouldTranspileContainsFilterWithAllOperator() {
        // Given that a 'contains' constraint uses operator="all":
        // <filter on="lineage.ids"><contains operator="all"><value>2759</value><value>10239</value></contains></filter>
        Condition organism = condition(ORGANISM, "organism",
                containsFilter("lineage.ids", LogicalOperator.ALL, "2759", "10239"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(organism), actions())));

        // Then the values must be combined with '&&'
        assertConditionsEquivalent("$organism := Organism((lineage!.ids contains 2759 && lineage!.ids contains 10239))", drl);
    }

    /**
     * A negated 'contains' filter uses the 'not contains' comparator.
     */
    @Test
    void shouldTranspileNegativeContainsFilter() {
        // Given that a 'contains' filter is negated:
        // <filter on="lineage.ids" negative="true"><contains operator="any">...</contains></filter>
        Filter contains = containsFilter("lineage.ids", LogicalOperator.ANY, "2759", "10239");
        contains.setNegative(true);
        Condition organism = condition(ORGANISM, "organism", contains);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(organism), actions())));

        // Then the 'not contains' comparator must be used for every value
        assertConditionsEquivalent("$organism := Organism((lineage!.ids not contains 2759 || lineage!.ids not contains 10239))", drl);
    }

    /**
     * An 'in' filter with operator="any" compares each value with '==' combined by '||',
     * quoting string literals.
     */
    @Test
    void shouldTranspileInFilterWithAnyOperator() {
        // Given that an 'in' constraint on the organism scientific name uses operator="any":
        // <filter on="scientificName"><in operator="any">
        //   <value>Saccharomyces cerevisiae</value><value>Homo sapiens</value></in></filter>
        Condition organism = condition(ORGANISM, "organism",
                inFilter("scientificName", LogicalOperator.ANY, "Saccharomyces cerevisiae", "Homo sapiens"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(organism), actions())));

        // Then each value must be compared with '==' and combined with '||', and string literals must
        // be double-quoted with their internal spaces preserved verbatim
        assertConditionsEquivalent("""
                $organism := Organism((scientificName == "Saccharomyces cerevisiae"
                    || scientificName == "Homo sapiens"))
                """, drl);
    }

    /**
     * A negated 'in' filter with operator="all" compares each value with '!=' combined
     * by '&&'.
     */
    @Test
    void shouldTranspileNegativeInFilterWithAllOperator() {
        // Given that an 'in' constraint is negated and uses operator="all":
        // <filter on="scientificName" negative="true"><in operator="all">...</in></filter>
        Filter in = inFilter("scientificName", LogicalOperator.ALL, "Saccharomyces cerevisiae", "Homo sapiens");
        in.setNegative(true);
        Condition organism = condition(ORGANISM, "organism", in);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(organism), actions())));

        // Then each value must be compared with '!=' and combined with '&&'
        assertConditionsEquivalent("""
                $organism := Organism((scientificName != "Saccharomyces cerevisiae"
                    && scientificName != "Homo sapiens"))
                """, drl);
    }

    /**
     * A range filter with only a start becomes a '>=' comparison with an unquoted number.
     */
    @Test
    void shouldTranspileRangeFilterWithStartOnly() {
        // Given that a range constraint on the numeric 'frequency' attribute has only a start:
        // <filter on="frequency"><range start="5"/></filter>
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", rangeFilter("frequency", 5, null));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(sig), actions())));

        // Then the constraint must be rendered as a '>=' comparison with an unquoted number
        assertConditionsEquivalent("$sig := ProteinSignature(frequency >= 5)", drl);
    }

    /**
     * A range filter with only an end becomes a '<=' comparison with an unquoted number.
     */
    @Test
    void shouldTranspileRangeFilterWithEndOnly() {
        // Given that a range constraint on the numeric 'frequency' attribute has only an end:
        // <filter on="frequency"><range end="10"/></filter>
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", rangeFilter("frequency", null, 10));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(sig), actions())));

        // Then the constraint must be rendered as a '<=' comparison with an unquoted number
        assertConditionsEquivalent("$sig := ProteinSignature(frequency <= 10)", drl);
    }

    /**
     * A range filter with both start and end emits both bounds joined by a comma.
     */
    @Test
    void shouldTranspileRangeFilterWithStartAndEnd() {
        // Given that a range constraint has both start and end:
        // <filter on="frequency"><range start="5" end="10"/></filter>
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", rangeFilter("frequency", 5, 10));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(sig), actions())));

        // Then both bounds must be emitted as separate comparisons joined by a comma
        assertConditionsEquivalent("$sig := ProteinSignature(frequency >= 5, frequency <= 10)", drl);
    }

    /**
     * Negated range filters invert the comparators ('<' for start, '>' for end).
     */
    @Test
    void shouldTranspileNegativeRangeFilters() {
        // Given that two range constraints are negated, one with a start and one with an end:
        // <filter on="frequency" negative="true"><range start="5"/></filter>
        // <filter on="frequency" negative="true"><range end="10"/></filter>
        Filter below = rangeFilter("frequency", 5, null);
        below.setNegative(true);
        Filter above = rangeFilter("frequency", null, 10);
        above.setNegative(true);

        // When the rules are transpiled to DRL
        String drl = transpile(rules(rule("RULE_1",
                and(condition(PROTEIN_SIGNATURE, "sig1", below), condition(PROTEIN_SIGNATURE, "sig2", above)),
                actions())));

        // Then the negated comparators must be '<' for the start bound and '>' for the end bound
        assertConditionsEquivalent("""
                $sig1 := ProteinSignature(frequency < 5) and $sig2 := ProteinSignature(frequency > 10)
                """, drl);
    }

    /**
     * A field-list filter renders enum values via fromValue, strings quoted and reference
     * fields '$'-prefixed, with null-safe navigation and comma separators.
     */
    @Test
    void shouldTranspileFieldListFilter() {
        /*
         * Given that a filter on the nested 'signature' attribute constrains several sub-fields,
         * one of which is a reference to another bound fact:
         * <filter on="signature">
         *   <field attribute="type">PROSITE</field>
         *   <field attribute="value">PS50061</field>
         *   <field attribute="value" isReference="true">other</field>
         * </filter>
         */
        Filter signature = filter("signature");
        signature.getField().add(field("type", "PROSITE"));
        signature.getField().add(field("value", "PS50061"));
        signature.getField().add(referenceField("value", "other"));
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", signature);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(sig), actions())));

        // Then the enum-typed 'type' field must use SignatureType.fromValue(...), the string field must
        // be double-quoted, the reference field must be '$'-prefixed, all with null-safe navigation
        // on 'signature' and comma separators between fields
        assertConditionsEquivalent("""
                $sig := ProteinSignature(signature!.type == SignatureType.fromValue("PROSITE"),
                    signature!.value == "PS50061", signature!.value == $other)
                """, drl);
    }

    /**
     * A simple-value filter uses '==' with the appropriately rendered literal, and '!='
     * when negated.
     */
    @Test
    void shouldTranspileSimpleValueFilter() {
        // Given that two filters constrain the 'id' attribute with a plain value, one negated:
        // <filter on="id"><value>P12345</value></filter>
        // <filter on="id" negative="true"><value>P12345</value></filter>
        Filter negative = valueFilter("id", "P12345");
        negative.setNegative(true);

        // When the rules are transpiled to DRL
        String drl = transpile(rules(rule("RULE_1",
                and(condition(PROTEIN, "p1", valueFilter("id", "P12345")), condition(PROTEIN, "p2", negative)),
                actions())));

        // Then the positive filter must use '==' and the negative one '!=', with a quoted string literal
        assertConditionsEquivalent("""
                $p1 := Protein(id == "P12345") and $p2 := Protein(id != "P12345")
                """, drl);
    }

    /**
     * A reference filter compares the attribute against a '$'-prefixed binding id.
     */
    @Test
    void shouldTranspileReferenceFilter() {
        // Given that a filter compares the 'protein' attribute against a previously bound fact:
        // <filter on="protein"><ref>protein</ref></filter>
        Filter ref = filter("protein");
        ref.setRef("protein");
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", ref);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(sig), actions())));

        // Then the comparison target must be the '$'-prefixed binding id, without quotes
        assertConditionsEquivalent("$sig := ProteinSignature(protein == $protein)", drl);
    }

    /**
     * A startsWith filter becomes a 'matches' comparison against the prefix suffixed
     * with '.*'.
     */
    @Test
    void shouldTranspileStartsWithFilter() {
        // Given that a filter requires the 'id' attribute to start with a given prefix:
        // <filter on="id"><startsWith>P0</startsWith></filter>
        Filter startsWith = filter("id");
        StartsWith value = new StartsWith();
        value.setValue("P0");
        startsWith.setStartsWith(value);
        Condition protein = condition(PROTEIN, "protein", startsWith);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(protein), actions())));

        // Then the constraint must become a 'matches' comparison against the prefix suffixed with '.*'
        assertConditionsEquivalent("""
                $protein := Protein(id matches "P0.*")
                """, drl);
    }

    /**
     * A matches filter uses the 'matches' operator, or 'not matches' when negated.
     */
    @Test
    void shouldTranspileMatchesFilter() {
        // Given that two filters match the 'id' attribute against a regular expression, one negated:
        // <filter on="id"><matches>P0.*</matches></filter>
        // <filter on="id" negative="true"><matches>P0.*</matches></filter>
        Filter matches = filter("id");
        Matches matchesValue = new Matches();
        matchesValue.setValue("P0.*");
        matches.setMatches(matchesValue);
        Filter negativeMatches = filter("id");
        Matches negativeValue = new Matches();
        negativeValue.setValue("P0.*");
        negativeMatches.setMatches(negativeValue);
        negativeMatches.setNegative(true);

        // When the rules are transpiled to DRL
        String drl = transpile(rules(rule("RULE_1",
                and(condition(PROTEIN, "p1", matches), condition(PROTEIN, "p2", negativeMatches)), actions())));

        // Then the positive filter must use 'matches' and the negative one 'not matches'
        assertConditionsEquivalent("""
                $p1 := Protein(id matches "P0.*") and $p2 := Protein(id not matches "P0.*")
                """, drl);
    }

    /**
     * A filter without constraint content becomes a boolean self-check ('attr == true/false')
     * and, unlike other filters, does not apply null-safe navigation.
     */
    @Test
    void shouldTranspileEmptyFilterAsBooleanSelfCheck() {
        // Given that two filters carry no constraint content at all, one negated:
        // <filter on="sequence.isFragment"/>
        // <filter on="sequence.isFragment" negative="true"/>
        Filter isFragment = filter("sequence.isFragment");
        Filter notFragment = filter("sequence.isFragment");
        notFragment.setNegative(true);

        // When the rules are transpiled to DRL
        String drl = transpile(rules(rule("RULE_1",
                and(condition(PROTEIN, "p1", isFragment), condition(PROTEIN, "p2", notFragment)), actions())));

        // Then the attribute itself must be compared against true (or false when negated); unlike other
        // filters this branch must NOT apply null-safe navigation, which the structural comparison pins
        assertConditionsEquivalent("""
                $p1 := Protein(sequence.isFragment == true) and $p2 := Protein(sequence.isFragment == false)
                """, drl);
    }

    /**
     * Deeply nested attributes get null-safe navigation ('!.') on every segment but the last.
     */
    @Test
    void shouldTranspileDeeplyNestedAttributeWithNullSafeNavigation() {
        // Given that a filter targets an attribute three levels deep:
        // <filter on="organism.lineage.ids"><contains operator="any"><value>10239</value></contains></filter>
        Condition protein = condition(PROTEIN, "protein",
                containsFilter("organism.lineage.ids", LogicalOperator.ANY, "10239"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(protein), actions())));

        // Then every segment but the last must be guarded with null-safe navigation ('!.'):
        // (organism!.lineage!.ids contains 10239)
        assertConditionsEquivalent("$protein := Protein((organism!.lineage!.ids contains 10239))", drl);
    }

    /**
     * Filter values are rendered according to the Java type of the attribute: numbers and
     * booleans unquoted, the string 'null' as the null literal.
     */
    @Test
    void shouldRenderValuesAccordingToTheirJavaType() {
        /*
         * Given that simple-value filters target a numeric attribute ('frequency'), a boolean
         * attribute ('sequence.isFragment') and a string attribute set to the literal 'null':
         * <filter on="frequency"><value>5</value></filter>
         * <filter on="sequence.isFragment"><value>true</value></filter>
         * <filter on="id"><value>null</value></filter>
         */
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", valueFilter("frequency", "5"));
        Condition protein = condition(PROTEIN, "protein", valueFilter("sequence.isFragment", "true"));
        Condition nullId = condition(PROTEIN, "other", valueFilter("id", "null"));

        // When the rules are transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(sig, protein, nullId), actions())));

        // Then numbers and booleans must be emitted unquoted, and the string 'null' must become the
        // null literal (not a quoted string)
        assertConditionsEquivalent("""
                $sig := ProteinSignature(frequency == 5)
                    and $protein := Protein(sequence!.isFragment == true)
                    and $other := Protein(id == null)
                """, drl);
    }

    /**
     * A filter value on a fact-typed attribute (not renderable as a literal) raises an
     * IllegalArgumentException naming the value and type.
     */
    @Test
    void shouldThrowOnUnsupportedValueType() {
        // Given that a simple-value filter targets 'signature', which is a fact-typed attribute and
        // therefore cannot be rendered as a comparison literal:
        // <filter on="signature"><value>foo</value></filter>
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", valueFilter("signature", "foo"));
        Rules rules = rules(rule("RULE_1", and(sig), actions()));

        // When the rule is transpiled, Then an IllegalArgumentException must be raised whose message
        // names the offending value and its Java type
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> transpile(rules));
        assertTrue(exception.getMessage().contains("Unsupported value foo of type class org.uniprot.urml.facts.Signature"),
                exception.getMessage());
    }

    /**
     * A non-numeric value on a numeric attribute raises an IllegalArgumentException.
     */
    @Test
    void shouldThrowOnNonNumericValueForNumericAttribute() {
        // Given that a simple-value filter assigns the non-numeric value 'abc' to the numeric
        // 'frequency' attribute:
        // <filter on="frequency"><value>abc</value></filter>
        Condition sig = condition(PROTEIN_SIGNATURE, "sig", valueFilter("frequency", "abc"));
        Rules rules = rules(rule("RULE_1", and(sig), actions()));

        // When the rule is transpiled, Then an IllegalArgumentException must be raised because the
        // value is neither numeric nor renderable as another supported type
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> transpile(rules));
        assertTrue(exception.getMessage().contains("Unsupported value abc"), exception.getMessage());
    }

    /**
     * A filter on an attribute that does not exist on the fact raises an
     * IllegalArgumentException caused by FactModelReflectionException.
     */
    @Test
    void shouldThrowOnUnknownFactAttribute() {
        // Given that a filter targets the attribute 'bogus', which does not exist on the Protein fact:
        // <filter on="bogus"><value>x</value></filter>
        Condition protein = condition(PROTEIN, "protein", valueFilter("bogus", "x"));
        Rules rules = rules(rule("RULE_1", and(protein), actions()));

        // When the rule is transpiled, Then an IllegalArgumentException must be raised with the
        // FactModelReflectionException as its cause
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> transpile(rules));
        assertInstanceOf(FactModelReflectionException.class, exception.getCause());
    }

    /**
     * A declare action assigns the built fact to a typed RHS variable via the builder.
     */
    @Test
    void shouldTranspileDeclareActionWithBuildStatement() {
        /*
         * Given that a declare action defines a new ProteinAnnotation variable built from a wired
         * protein reference and a field value:
         * <action type="declare" with="protein">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <field attribute="type">keyword</field>
         *   </fact>
         * </action>
         */
        Action declare = action(ActionType.DECLARE, List.of("protein"),
                fact(PROTEIN_ANNOTATION, "annotation", field("type", "keyword")));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(declare))));

        // Then the fact must be assigned to a typed variable built via the builder, and the declared
        // id must be registered as an RHS variable for subsequent actions
        assertConsequencesEquivalent("""
                ProteinAnnotation annotation = ProteinAnnotation.builder().withProtein($protein).withType("keyword").build()
                """, drl);
    }

    /**
     * A declare action with a procedural call assigns the procedure result, qualified by
     * the URI host, to the RHS variable.
     */
    @Test
    void shouldTranspileDeclareActionWithProceduralCall() {
        /*
         * Given that a declare action obtains its fact from a procedural attachment call whose
         * argument is a reference:
         * <action type="declare">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <call uri="java://uk.ac.ebi.uniprot.procedures.TestProcedure" procedure="createAnnotation">
         *       <arguments><argument isReference="true">protein</argument></arguments>
         *     </call>
         *   </fact>
         * </action>
         */
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation");
        annotation.setCall(call("createAnnotation", arg("protein", true)));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.DECLARE, annotation)))));

        // Then the variable must be assigned the result of the procedure, qualified by the URI host,
        // and the reference argument must be '$'-prefixed
        assertConsequencesEquivalent(
                "ProteinAnnotation annotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein)", drl);
    }

    /**
     * A procedural call argument that references an RHS-declared variable is emitted
     * without the '$' prefix.
     */
    @Test
    void shouldTranspileProceduralCallArgumentReferencingRhsDeclaredVariable() {
        /*
         * Given that a declare action uses a procedural call whose argument references another
         * fact declared earlier in the same actions block:
         * <action type="declare">... declare 'otherAnnotation' ...</action>
         * <action type="declare">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <call uri="java://uk.ac.ebi.uniprot.procedures.TestProcedure" procedure="combine">
         *       <arguments><argument isReference="true">otherAnnotation</argument></arguments>
         *     </call>
         *   </fact>
         * </action>
         */
        RuleFact otherDeclared = fact(PROTEIN_ANNOTATION, "otherAnnotation");
        otherDeclared.setCall(call("createAnnotation", arg("protein", true)));
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation");
        annotation.setCall(call("combine", arg("otherAnnotation", true)));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.DECLARE, otherDeclared), action(ActionType.DECLARE, annotation)))));

        // Then the reference argument must be emitted WITHOUT the '$' prefix because it is an
        // RHS-declared variable
        assertConsequencesEquivalent("""
                ProteinAnnotation otherAnnotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein)
                ProteinAnnotation annotation = uk.ac.ebi.uniprot.procedures.TestProcedure.combine(otherAnnotation)
                """, drl);
    }

    /**
     * A create action with a procedural call inserts the call result logically, with
     * reference arguments '$'-prefixed and plain arguments emitted as-is.
     */
    @Test
    void shouldTranspileCreateActionWithProceduralCall() {
        /*
         * Given that a create action obtains its fact from a procedural attachment call with a
         * reference argument and a plain value argument:
         * <call uri="java://uk.ac.ebi.uniprot.procedures.TestProcedure" procedure="createAnnotation">
         *   <arguments>
         *     <argument isReference="true">protein</argument>
         *     <argument>5</argument>
         *   </arguments>
         * </call>
         */
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation");
        annotation.setCall(call("createAnnotation", arg("protein", true), arg("5", false)));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.CREATE, annotation)))));

        // Then the call result must be inserted logically, the reference argument '$'-prefixed, the
        // plain argument emitted as-is, and both separated by a comma
        assertConsequencesEquivalent("""
                ProteinAnnotation $annotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein, 5);
                insertLogical($annotation);
                """, drl);
    }

    /**
     * A create action builder field that references an RHS-declared variable omits the '$' prefix.
     */
    @Test
    void shouldTranspileCreateBuilderFieldReferencingRhsDeclaredVariable() {
        /*
         * Given that a create action sets a builder field by referencing a fact declared earlier
         * in the same actions block:
         * <action type="declare">... declare 'otherAnnotation' ...</action>
         * <action type="create">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
     *     <field attribute="evidence" isReference="true">otherAnnotation</field>
         *   </fact>
         * </action>
         */
        RuleFact otherDeclared = fact(PROTEIN_ANNOTATION, "otherAnnotation");
        otherDeclared.setCall(call("createAnnotation", arg("protein", true)));
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation", referenceField("evidence", "otherAnnotation"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.DECLARE, otherDeclared), action(ActionType.CREATE, annotation)))));

        // Then the builder field reference must be emitted WITHOUT the '$' prefix because it is an
        // RHS-declared variable
        assertConsequencesEquivalent("""
                ProteinAnnotation otherAnnotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein)
                ProteinAnnotation $annotation = ProteinAnnotation.builder().withEvidence(otherAnnotation).build();
                insertLogical($annotation);
                """, drl);
    }

    /**
     * An update action without a call emits 'update($id)' followed by one setter per field,
     * pinning the current behaviour of the single '$' prefix.
     */
    @Test
    void shouldTranspileUpdateActionWithSetStatements() {
        /*
         * Given that an update action sets two fields on a fact, one of them by reference:
         * <action type="update">
         *   <fact type="fact:ProteinAnnotation" id="proteinAnnotation">
         *     <field attribute="type">keyword</field>
         *     <field attribute="evidence" isReference="true">otherAnnotation</field>
         *   </fact>
         * </action>
         */
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "proteinAnnotation",
                field("type", "keyword"), referenceField("evidence", "otherAnnotation"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.UPDATE, annotation)))));

        /*
         * Then the fact must be updated via 'update($id)' followed by one setter call per field, with
         * the reference field '$'-prefixed.
         */
        assertConsequencesEquivalent("""
                update($proteinAnnotation)
                $proteinAnnotation.setType("keyword")
                $proteinAnnotation.setEvidence($otherAnnotation)
                """, drl);
    }

    /**
     * An update on a fact declared earlier in the same actions block references the
     * declared variable without the '$' prefix in set statements.
     */
    @Test
    void shouldTranspileUpdateOnFactDeclaredEarlierInSameActions() {
        /*
         * Given that an update action targets a fact that was declared earlier in the same actions
         * block (making it an RHS variable):
         * <action type="declare">... declare 'annotation' ...</action>
         * <action type="update">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <field attribute="value">x</field>
         *   </fact>
         * </action>
         */
        RuleFact declared = fact(PROTEIN_ANNOTATION, "annotation");
        declared.setCall(call("createAnnotation", arg("protein", true)));
        RuleFact updated = fact(PROTEIN_ANNOTATION, "annotation", field("value", "x"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.DECLARE, declared), action(ActionType.UPDATE, updated)))));

        // Then the set statement must reference the declared variable WITHOUT the '$' prefix, because
        // it is already a local RHS variable rather than a fact binding from the 'when' part
        assertConsequencesEquivalent("""
                ProteinAnnotation annotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein)
                update(annotation)
                annotation.setValue("x")
                """, drl);
    }

    /**
     * An update action field reference that targets an RHS-declared variable omits the '$' prefix
     * from the reference value.
     */
    @Test
    void shouldTranspileUpdateFieldReferenceReferencingRhsDeclaredVariable() {
        /*
         * Given that an update action sets a field by referencing a fact declared earlier in the
         * same actions block:
         * <action type="declare">... declare 'otherAnnotation' ...</action>
         * <action type="update">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <field attribute="evidence" isReference="true">otherAnnotation</field>
         *   </fact>
         * </action>
         */
        RuleFact otherDeclared = fact(PROTEIN_ANNOTATION, "otherAnnotation");
        otherDeclared.setCall(call("createAnnotation", arg("protein", true)));
        RuleFact updated = fact(PROTEIN_ANNOTATION, "annotation", referenceField("evidence", "otherAnnotation"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.DECLARE, otherDeclared), action(ActionType.UPDATE, updated)))));

        // Then the field reference must be emitted WITHOUT the '$' prefix because it is an
        // RHS-declared variable, while the update target keeps '$' because it is LHS-bound
        assertConsequencesEquivalent("""
                ProteinAnnotation otherAnnotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein)
                update($annotation)
                $annotation.setEvidence(otherAnnotation)
                """, drl);
    }

    /**
     * An update action field reference that targets a property of an RHS-declared variable omits
     * the '$' prefix from the root variable name only.
     */
    @Test
    void shouldTranspileUpdateFieldReferenceReferencingPropertyOfRhsDeclaredVariable() {
        /*
         * Given that an update action sets a field by referencing a property of a fact declared
         * earlier in the same actions block:
         * <action type="declare">... declare 'newPositionalMapping' ...</action>
         * <action type="update">
         *   <fact type="fact:PositionalMapping" id="positionalMapping">
         *     <field attribute="mappedStart" isReference="true">newPositionalMapping.mappedStart</field>
         *   </fact>
         * </action>
         */
        RuleFact newMapping = fact(new QName(FACT_NAMESPACE, "PositionalMapping", "fact"), "newPositionalMapping");
        newMapping.setCall(call("map", arg("positionalMapping", true)));
        RuleFact updated = fact(new QName(FACT_NAMESPACE, "PositionalMapping", "fact"), "positionalMapping",
                referenceField("mappedStart", "newPositionalMapping.mappedStart"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1",
                and(condition(new QName(FACT_NAMESPACE, "PositionalMapping", "fact"), "positionalMapping")),
                actions(action(ActionType.DECLARE, newMapping), action(ActionType.UPDATE, updated)))));

        // Then the root of the dotted reference must be emitted WITHOUT the '$' prefix because it is
        // an RHS-declared variable, while the LHS-bound update target keeps '$'
        assertConsequencesEquivalent("""
                PositionalMapping newPositionalMapping = uk.ac.ebi.uniprot.procedures.TestProcedure.map($positionalMapping)
                update($positionalMapping)
                $positionalMapping.setMappedStart(newPositionalMapping.mappedStart)
                """, drl);
    }

    /**
     * An update action with a procedural call emits 'update($id)' followed by the call.
     */
    @Test
    void shouldTranspileUpdateActionWithProceduralCall() {
        /*
         * Given that an update action delegates to a procedural attachment call:
         * <action type="update">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <call uri="java://uk.ac.ebi.uniprot.procedures.TestProcedure" procedure="annotate">
         *       <arguments><argument isReference="true">protein</argument></arguments>
         *     </call>
         *   </fact>
         * </action>
         */
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation");
        annotation.setCall(call("annotate", arg("protein", true)));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.UPDATE, annotation)))));

        // Then the update must be followed by the procedure call on the next line
        assertConsequencesEquivalent("""
                update($annotation)
                uk.ac.ebi.uniprot.procedures.TestProcedure.annotate($protein)
                """, drl);
    }

    /**
     * A remove action retracts the fact by its '$'-prefixed id.
     */
    @Test
    void shouldTranspileRemoveAction() {
        // Given that a remove action targets a bound fact:
        // <action type="remove"><fact type="fact:ProteinAnnotation" id="annotation"/></action>
        Action remove = action(ActionType.REMOVE, fact(PROTEIN_ANNOTATION, "annotation"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(remove))));

        // Then the fact must be retracted from the working memory by its '$'-prefixed id
        assertConsequencesEquivalent("retract($annotation)", drl);
    }

    /**
     * Wired references in build statements omit the '$' prefix for facts declared earlier
     * in the same actions block, and single-quoted literals are rewritten with double quotes.
     */
    @Test
    void shouldTranspileWiredReferencesAndLiteralsInBuildStatement() {
        /*
         * Given that a create action wires the fact declared earlier in the same actions block, plus
         * an inline single-quoted string literal:
         * <action type="declare">... declare 'annotation' ...</action>
         * <action type="create" with="type:'keyword'">
         *   <fact type="fact:ProteinAnnotation" id="annotation2" with="protein:annotation"/>
         * </action>
         */
        RuleFact declared = fact(PROTEIN_ANNOTATION, "annotation");
        declared.setCall(call("createAnnotation", arg("protein", true)));
        RuleFact created = fact(PROTEIN_ANNOTATION, "annotation2");
        created.getWith().add("protein:annotation");

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.DECLARE, declared),
                        action(ActionType.CREATE, List.of("type:'keyword'"), created)))));

        // Then the declared fact must be wired WITHOUT the '$' prefix (it is a known RHS variable),
        // and the single-quoted literal must be rewritten with double quotes
        assertConsequencesEquivalent("""
                ProteinAnnotation annotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein);
                ProteinAnnotation $annotation2 = ProteinAnnotation.builder().withProtein(annotation).withType("keyword").build();
                insertLogical($annotation2);
                """, drl);
    }

    /**
     * A reference field in a build statement is emitted as a '$'-prefixed variable.
     */
    @Test
    void shouldTranspileReferenceFieldInBuildStatement() {
        /*
         * Given that a create action builds a fact with a field that references another bound fact:
         * <action type="create">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <field attribute="protein" isReference="true">otherProtein</field>
         *   </fact>
         * </action>
         */
        Action create = action(ActionType.CREATE,
                fact(PROTEIN_ANNOTATION, "annotation", referenceField("protein", "otherProtein")));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(create))));

        // Then the reference field must be emitted as a '$'-prefixed variable, not a quoted literal
        assertConsequencesEquivalent("""
                ProteinAnnotation $annotation = ProteinAnnotation.builder().withProtein($otherProtein).build();
                insertLogical($annotation);
                """, drl);
    }

    /**
     * A CREATE action without an explicit fact id inserts the builder expression inline.
     */
    @Test
    void shouldInlineInsertWhenCreateFactIdIsEmpty() {
        /*
         * Given that a create action has no id:
         * <action type="create">
         *   <fact type="fact:ProteinAnnotation">
         *     <field attribute="type">keyword</field>
         *   </fact>
         * </action>
         */
        Action create = action(ActionType.CREATE, fact(PROTEIN_ANNOTATION, null, field("type", "keyword")));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(create))));

        // Then the fact must be inserted directly without declaring a local variable
        assertConsequencesEquivalent("""
                insertLogical(ProteinAnnotation.builder().withType("keyword").build());
                """, drl);
    }

    /**
     * A CREATE action without an explicit fact id inserts a procedural call inline.
     */
    @Test
    void shouldInlineInsertForProceduralCallWhenCreateFactIdIsEmpty() {
        /*
         * Given that a create action uses a procedural attachment and has no id:
         * <action type="create">
         *   <fact type="fact:ProteinAnnotation">
         *     <call uri="java://..." procedure="createAnnotation">
         *       <arguments><argument isReference="true">protein</argument></arguments>
         *     </call>
         *   </fact>
         * </action>
         */
        RuleFact annotation = fact(PROTEIN_ANNOTATION, null);
        annotation.setCall(call("createAnnotation", arg("protein", true)));
        Action create = action(ActionType.CREATE, annotation);

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(create))));

        // Then the procedure call must be inserted directly without declaring a local variable
        assertConsequencesEquivalent("""
                insertLogical(uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein));
                """, drl);
    }

    /**
     * A wired value that starts but does not end with a single quote is not treated as a
     * string literal and falls through to the '$'-prefixed reference branch.
     */
    @Test
    void shouldNotTreatPartiallyQuotedWiredValueAsStringLiteral() {
        // Given that a wired value starts with a single quote but does NOT end with one (a malformed
        // string literal edge case):
        // <action type="create" with="note:'unterminated"> ... </action>
        Action create = action(ActionType.CREATE, List.of("note:'unterminated"),
                fact(PROTEIN_ANNOTATION, "annotation"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(create))));

        // Then the value must NOT be treated as a string literal; it falls through to the reference
        // branch and is emitted with a '$' prefix
        assertDrlContains(".withNote($'unterminated)", drl);
    }

    /**
     * A procedural attachment with a malformed URI raises an IllegalArgumentException
     * caused by URISyntaxException.
     */
    @Test
    void shouldThrowOnMalformedProcedureUri() {
        // Given that a procedural attachment declares a malformed URI:
        // <call uri="invalid uri" procedure="createAnnotation"/>
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation");
        ProceduralAttachment badCall = call("createAnnotation");
        badCall.setUri("invalid uri");
        annotation.setCall(badCall);
        Rules rules = rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.CREATE, annotation))));

        // When the rule is transpiled, Then an IllegalArgumentException must be raised with the
        // URISyntaxException as its cause
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> transpile(rules));
        assertInstanceOf(URISyntaxException.class, exception.getCause());
    }

    /**
     * Field values in build statements are passed through the sanitizer, and the sanitized
     * value appears in the DRL.
     */
    @Test
    void shouldApplySanitizerToBuildStatementFieldValues() {
        // Given that a create action builds a fact whose field value needs sanitising:
        // <field attribute="value">DNA-binding</field>
        CompositeSanitizer sanitizer = mock(CompositeSanitizer.class);
        when(sanitizer.sanitize("DNA-binding")).thenReturn("SANITIZED");
        Action create = action(ActionType.CREATE, fact(PROTEIN_ANNOTATION, "annotation", field("value", "DNA-binding")));
        Rules rules = rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(create)));

        // When the rule is transpiled to DRL
        String drl = transpile(rules, sanitizer);

        // Then the sanitized value must appear in the build statement, and the sanitizer must have
        // been invoked exactly once with the raw field value
        assertConsequencesEquivalent("""
                ProteinAnnotation $annotation = ProteinAnnotation.builder().withValue("SANITIZED").build();
                insertLogical($annotation);
                """, drl);
        verify(sanitizer, times(1)).sanitize("DNA-binding");
    }

    /**
     * Field values in update set-statements are used raw; the sanitizer is never invoked
     * (sanitization applies to build statements only).
     */
    @Test
    void shouldNotApplySanitizerToUpdateSetStatements() {
        /*
         * Given that an update action sets a field value:
         * <action type="update">
         *   <fact type="fact:ProteinAnnotation" id="annotation">
         *     <field attribute="type">keyword</field>
         *   </fact>
         * </action>
         */
        CompositeSanitizer sanitizer = mock(CompositeSanitizer.class);
        Action update = action(ActionType.UPDATE, fact(PROTEIN_ANNOTATION, "annotation", field("type", "keyword")));
        Rules rules = rules(rule("RULE_1", and(condition(PROTEIN, "protein")), actions(update)));

        // When the rule is transpiled to DRL
        String drl = transpile(rules, sanitizer);

        // Then the raw field value must be used in the set statement and the sanitizer must never be
        // invoked (sanitization applies to build statements only)
        assertConsequencesEquivalent("""
                update($annotation)
                $annotation.setType("keyword")
                """, drl);
        verifyNoInteractions(sanitizer);
    }

    /**
     * An update action whose fact carries 'with' entries emits one setter call per wired
     * reference, after the field setters: colon forms map attribute to value, single-quoted
     * literals are rewritten with double quotes, and plain forms bind the same-named attribute
     * to a '$'-prefixed reference.
     */
    @Test
    void shouldTranspileUpdateActionWithWiredSetters() {
        /*
         * Given that an update action fact wires two values:
         * <action type="update">
         *   <fact type="fact:ProteinAnnotation" id="annotation" with="value:'Updated' protein"/>
         * </action>
         */
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation");
        annotation.getWith().addAll(List.of("value:'Updated'", "protein"));

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.UPDATE, annotation)))));

        // Then each wired entry must become a separate setter call: the colon form sets 'value' to
        // the double-quoted literal, and the plain form sets the same-named attribute to the
        // '$'-prefixed reference
        assertConsequencesEquivalent("""
                update($annotation);
                $annotation.setValue("Updated");
                $annotation.setProtein($protein);
                """, drl);
    }

    /**
     * Wired setters on a fact declared earlier in the same actions block omit the '$' prefix
     * on both the setter target and wired values referencing other RHS-declared variables.
     */
    @Test
    void shouldTranspileWiredSetterOnFactDeclaredEarlierInSameActions() {
        /*
         * Given that an update action targets a fact declared earlier in the same actions block,
         * and wires a value from another declared fact:
         * <action type="declare">... declare 'annotation' ...</action>
         * <action type="declare">... declare 'otherAnnotation' ...</action>
         * <action type="update">
         *   <fact type="fact:ProteinAnnotation" id="annotation" with="value:otherAnnotation"/>
         * </action>
         */
        RuleFact declared = fact(PROTEIN_ANNOTATION, "annotation");
        declared.setCall(call("createAnnotation", arg("protein", true)));
        RuleFact otherDeclared = fact(PROTEIN_ANNOTATION, "otherAnnotation");
        otherDeclared.setCall(call("createAnnotation", arg("protein", true)));
        RuleFact updated = fact(PROTEIN_ANNOTATION, "annotation");
        updated.getWith().add("value:otherAnnotation");

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.DECLARE, declared), action(ActionType.DECLARE, otherDeclared),
                        action(ActionType.UPDATE, updated)))));

        // Then the setter target and the wired value must both be emitted WITHOUT the '$' prefix,
        // because both are local RHS variables rather than fact bindings from the 'when' part
        assertConsequencesEquivalent("""
                ProteinAnnotation annotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein);
                ProteinAnnotation otherAnnotation = uk.ac.ebi.uniprot.procedures.TestProcedure.createAnnotation($protein);
                update(annotation);
                annotation.setValue(otherAnnotation);
                """, drl);
    }

    /**
     * A wired setter value that starts but does not end with a single quote is not treated as
     * a string literal and falls through to the '$'-prefixed reference branch.
     */
    @Test
    void shouldNotTreatPartiallyQuotedWiredSetterValueAsStringLiteral() {
        // Given that a wired setter value starts with a single quote but does NOT end with one:
        // <action type="update">
        //   <fact type="fact:ProteinAnnotation" id="annotation" with="note:'unterminated"/>
        // </action>
        RuleFact annotation = fact(PROTEIN_ANNOTATION, "annotation");
        annotation.getWith().add("note:'unterminated");

        // When the rule is transpiled to DRL
        String drl = transpile(rules(rule("RULE_1", and(condition(PROTEIN, "protein")),
                actions(action(ActionType.UPDATE, annotation)))));

        // Then the value must NOT be treated as a string literal; it falls through to the
        // reference branch and is emitted with a '$' prefix
        assertDrlContains("$annotation.setNote($'unterminated)", drl);
    }

    private String transpile(Rules rules) {
        return transpile(rules, new CompositeSanitizer(Collections.emptyList()));
    }

    private String transpile(Rules rules, CompositeSanitizer sanitizer) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new URMLToDroolsTranspiler(outputStream, sanitizer).translate(rules);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static Rules rules(Rule... rules) {
        Rules result = new Rules();
        result.setName("org.uniprot.test");
        Collections.addAll(result.getRule(), rules);
        return result;
    }

    private static Rule rule(String id, ConjunctiveConditionSet and, Actions actions) {
        return rule(id, or(and), actions);
    }

    private static Rule rule(String id, DisjunctiveConditionSet conditions, Actions actions) {
        Rule rule = new Rule();
        rule.setId(id);
        rule.setConditions(conditions);
        rule.setActions(actions);
        return rule;
    }

    private static DisjunctiveConditionSet or(ConjunctiveConditionSet... ands) {
        DisjunctiveConditionSet or = new DisjunctiveConditionSet();
        Collections.addAll(or.getAND(), ands);
        return or;
    }

    private static ConjunctiveConditionSet and(Condition... conditions) {
        ConjunctiveConditionSet and = new ConjunctiveConditionSet();
        Collections.addAll(and.getCondition(), conditions);
        return and;
    }

    private static Condition condition(QName on, String bind, Filter... filters) {
        Condition condition = new Condition();
        condition.setOn(on);
        condition.setBind(bind);
        Collections.addAll(condition.getFilter(), filters);
        return condition;
    }

    private static Filter filter(String on) {
        Filter filter = new Filter();
        filter.setOn(on);
        return filter;
    }

    private static Filter valueFilter(String on, String value) {
        Filter filter = filter(on);
        SimpleValue simpleValue = new SimpleValue();
        simpleValue.setValue(value);
        filter.setValue(simpleValue);
        return filter;
    }

    private static Filter containsFilter(String on, LogicalOperator operator, String... values) {
        Filter filter = filter(on);
        filter.setContains(multiValue(operator, values));
        return filter;
    }

    private static Filter inFilter(String on, LogicalOperator operator, String... values) {
        Filter filter = filter(on);
        filter.setIn(multiValue(operator, values));
        return filter;
    }

    private static MultiValue multiValue(LogicalOperator operator, String... values) {
        MultiValue multiValue = new MultiValue();
        multiValue.setOperator(operator);
        for (String value : values) {
            SimpleValue simpleValue = new SimpleValue();
            simpleValue.setValue(value);
            multiValue.getValue().add(simpleValue);
        }
        return multiValue;
    }

    private static Filter rangeFilter(String on, Integer start, Integer end) {
        Filter filter = filter(on);
        Range range = new Range();
        if (start != null) {
            range.setStart(start);
        }
        if (end != null) {
            range.setEnd(end);
        }
        filter.setRange(range);
        return filter;
    }

    private static Field field(String attribute, String value) {
        Field field = new Field();
        field.setAttribute(attribute);
        field.setValue(value);
        return field;
    }

    private static Field referenceField(String attribute, String value) {
        Field field = field(attribute, value);
        field.setIsReference(true);
        return field;
    }

    private static RuleFact fact(QName type, String id, Field... fields) {
        RuleFact fact = new RuleFact();
        fact.setType(type);
        fact.setId(id);
        Collections.addAll(fact.getField(), fields);
        return fact;
    }

    private static Action action(ActionType type, RuleFact... facts) {
        Action action = new Action();
        action.setType(type);
        Collections.addAll(action.getFact(), facts);
        return action;
    }

    private static Action action(ActionType type, List<String> with, RuleFact... facts) {
        Action action = action(type, facts);
        action.getWith().addAll(with);
        return action;
    }

    private static Actions actions(Action... actions) {
        Actions result = new Actions();
        Collections.addAll(result.getAction(), actions);
        return result;
    }

    private static ProceduralAttachment call(String procedure, ProcedureArgument... arguments) {
        ProceduralAttachment call = new ProceduralAttachment();
        call.setUri(PROCEDURES_URI);
        call.setProcedure(procedure);
        ProceduralAttachment.Arguments args = new ProceduralAttachment.Arguments();
        Collections.addAll(args.getArgument(), arguments);
        call.setArguments(args);
        return call;
    }

    private static ProcedureArgument arg(String value, boolean isReference) {
        ProcedureArgument argument = new ProcedureArgument();
        argument.setValue(value);
        argument.setIsReference(isReference);
        return argument;
    }
}
