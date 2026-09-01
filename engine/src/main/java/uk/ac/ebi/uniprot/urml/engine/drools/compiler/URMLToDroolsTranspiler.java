/*
 *  Copyright (c) 2018 European Molecular Biology Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package uk.ac.ebi.uniprot.urml.engine.drools.compiler;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.urml.facts.*;
import org.uniprot.urml.rules.*;
import uk.ac.ebi.uniprot.urml.core.model.facts.reflection.FactModelHelper;
import uk.ac.ebi.uniprot.urml.core.model.facts.reflection.FactModelReflectionException;
import uk.ac.ebi.uniprot.urml.core.xml.schema.URMLConstants;

import javax.xml.namespace.QName;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Transpiles URML formatted rules to the Drools Rule Language (DRL)
 *
 * @author Alexandre Renaux
 */
public class URMLToDroolsTranspiler {

    private static final Logger logger = LoggerFactory.getLogger(URMLToDroolsTranspiler.class);

    enum DroolsComparator {
        EQUALS("==", "!="),
        GREATER_THAN_OR_EQ(">=", "<"),
        LESS_THAN_OR_EQ("<=", ">"),
        MATCHES("matches", "not matches"),
        CONTAINS("contains", "not contains");

        private final String positive;
        private final String negative;

        DroolsComparator(String positive, String negative) {
            this.positive = positive;
            this.negative = negative;
        }

        String getValue(boolean isNegative) {
            return isNegative ? negative : positive;
        }
    }

    private static final String RULE_START = "rule";
    private static final String RULE_END = "end";
    private static final String OR = "or";
    private static final String LHS_KEYWORD = "when";
    private static final String RHS_KEYWORD = "then";
    private static final String QUOTE = "\"";
    private static final String INDENT_UNIT = "  ";
    private static final String NEW_LINE = "\n";
    private static final String UNIFICATION = ":=";

    private final PrintWriter writer;
    private final Sanitizer sanitizer;

    public URMLToDroolsTranspiler(OutputStream outputStream, CompositeSanitizer sanitizer) {
        this.writer = new PrintWriter(outputStream);
        this.sanitizer = sanitizer;
    }

    /**
     * Transpiles a URML rule set into Drools Rule Language (DRL).
     *
     * <p>Output starts with the package, fact imports, and a {@code @classReactive}
     * declaration for each tracked fact class, followed by the generated rules.</p>
     */
    public void translate(Rules rules) {
        logger.debug("Transpiling URML to Drools...");
        writer.write("package " + rules.getName() + ";");
        newLines(2);
        writer.write("import " + URMLConstants.URML_FACT_MODEL_PKG + ".*;");
        newLines(1);
        writer.write("import java.util.List;");
        newLines(2);
        declareFactClassReactivity();
        newLines(1);
        rules.getRule().forEach(this::translateRule);
        writer.close();
    }

    private void declareFactClassReactivity() {
        var factClasses = List.of(
                GeneInformation.class,
                Organism.class,
                PositionalFeatureTag.class,
                PositionalMapping.class,
                PositionalProteinSignature.class,
                Protein.class,
                ProteinAnnotation.class,
                ProteinSignature.class,
                TemplateProtein.class,
                TemplateProteinSignature.class
        );
        for (Class<?> factClass : factClasses) {
            writer.write(String.format("""
                    declare %s
                        @classReactive
                    end
                    """, factClass.getName()));
            newLines(1);
        }
    }

    /**
     * Transpiles a single URML rule into a DRL rule.
     *
     * <p>Each rule is emitted with {@code dialect "mvel"} and {@code no-loop true}.
     * Procedural rules additionally receive a low salience so they run after
     * regular rules.</p>
     *
     * <p>Example output:</p>
     * <pre>
     * rule "UR_UPDATE"
     *   dialect "mvel"
     *   no-loop true
     *   when
     *     ...
     *   then
     *     ...
     *   end
     * </pre>
     */
    private void translateRule(Rule rule) {
        writer.write(RULE_START + " ");
        writer.write(enquote(rule.getId()));
        if (rule.getExtends() != null) {
            writer.write(" " + "extends" + " ");
            writer.write(enquote(rule.getExtends().getId()));
        }
        newLines(1);
        writer.write("dialect \"mvel\"");
        newLines(1);
        writer.write("no-loop true");
        newLines(1);
        if (rule.getProcedural()) {
            writer.write("salience -10");
            newLines(1);
        }
        writer.write(LHS_KEYWORD);
        newLines(1);
        translateDisjunctiveConditions(rule.getConditions());
        newLines(1);
        writer.write(RHS_KEYWORD);
        newLines(1);
        translateActions(rule.getActions());
        writer.write(RULE_END);
        newLines(2);
    }

    /**
     * Emits the OR branches of a disjunctive condition set.
     *
     * <p>Each non-first branch is separated by an {@code or} keyword.</p>
     */
    private void translateDisjunctiveConditions(DisjunctiveConditionSet disjunctiveConditionSet) {
        boolean firstIteration = true;
        for (ConjunctiveConditionSet conjunctiveConditionSet : disjunctiveConditionSet.getAND()) {
            if (!firstIteration) {
                writer.write(NEW_LINE);
                indent(1);
                writer.write(OR);
                writer.write(NEW_LINE);
            }
            indent(1);
            openParenthesis();
            writer.write(NEW_LINE);
            translateConjunctiveConditions(conjunctiveConditionSet);
            indent(1);
            closeParenthesis();
            firstIteration = false;
        }
    }

    /**
     * Emits the AND conditions of a conjunctive condition set.
     *
     * <p>Each non-first condition is separated by an {@code and} keyword.</p>
     */
    private void translateConjunctiveConditions(ConjunctiveConditionSet conjunctiveConditionSet) {
        boolean firstIteration = true;
        for (Condition condition : conjunctiveConditionSet.getCondition()) {
            if (!firstIteration) {
                indent(2);
                writer.write("and");
                newLines(1);
            }
            translateCondition(condition);
            firstIteration = false;
        }
    }

    /**
     * Transpiles all actions of a rule, tracking RHS-declared variables so that
     * subsequent references to them are not prefixed with {@code $}.
     */
    private void translateActions(Actions actions) {
        Set<String> rhsDeclaredVariables = new HashSet<>();
        actions.getAction().forEach(a -> translateAction(a, rhsDeclaredVariables));
    }

    private void translateAction(Action action, Set<String> rhsDeclaredVariables) {
        action.getFact().forEach(f -> translateActionFact(f, action, rhsDeclaredVariables));
    }

    private String extractPackage(String uri) {
        try {
            return new URI(uri).getHost();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Dispatches a URML action fact to the handler for its action type.
     */
    private void translateActionFact(RuleFact ruleFact, Action action, Set<String> rhsDeclaredVariables) {
        indent(2);
        switch (action.getType()) {
            case DECLARE -> translateDeclareAction(ruleFact, action, rhsDeclaredVariables);
            case CREATE -> translateCreateAction(ruleFact, action, rhsDeclaredVariables);
            case UPDATE -> translateUpdateAction(ruleFact, action, rhsDeclaredVariables);
            case REMOVE -> translateRemoveAction(ruleFact, action, rhsDeclaredVariables);
            default -> throw new IllegalStateException("Unhandled action type " + action.getType());
        }
        writer.write(";");
        newLines(1);
    }

    /**
     * Transpiles a DECLARE action into a local RHS variable assignment.
     *
     * <p>DECLARE variables can be referenced by later actions in the same rule,
     * so {@code ruleFact.getId()} is recorded in {@code rhsDeclaredVariables}.</p>
     *
     * <p>Example:</p>
     * <pre>
     * &lt;action type="DECLARE"&gt;
     *   &lt;fact type="ProteinAnnotation" id="declared"&gt;
     *     &lt;field attribute="type" value="keyword"/&gt;
     *     &lt;field attribute="value" value="Declared"/&gt;
     *   &lt;/fact&gt;
     * &lt;/action&gt;
     * </pre>
     * becomes:
     * <pre>
     * ProteinAnnotation declared = ProteinAnnotation.builder().withType("keyword").withValue("Declared").build();
     * </pre>
     */
    private void translateDeclareAction(RuleFact ruleFact, Action action, Set<String> rhsDeclaredVariables) {
        writer.write(ruleFact.getType().getLocalPart());
        writer.write(enspace(ruleFact.getId()));
        writer.write("= ");
        if (ruleFact.getCall() == null) {
            translateToBuildStatement(ruleFact, action.getWith(), rhsDeclaredVariables);
        } else {
            translateProceduralCall(ruleFact.getCall(), rhsDeclaredVariables);
        }
        rhsDeclaredVariables.add(ruleFact.getId());
    }

    /**
     * Transpiles a CREATE action into a builder expression and an {@code insertLogical} call.
     *
     * <p>When the fact carries an explicit {@code id}, a local variable is declared and then
     * inserted. When no {@code id} is present, the builder/procedural call is passed directly
     * to {@code insertLogical(...)}.</p>
     *
     * <p>Example with id:</p>
     * <pre>
     * &lt;action type="CREATE"\u0026gt;
     *   &lt;fact type="ProteinAnnotation" id="annotation"\u0026gt;
     *     &lt;field attribute="type" value="keyword"/\u0026gt;
     *   &lt;/fact&gt;
     * &lt;/action&gt;
     * </pre>
     * becomes:
     * <pre>
     * ProteinAnnotation $annotation = ProteinAnnotation.builder().withType("keyword").build();
     * insertLogical($annotation);
     * </pre>
     *
     * <p>Example without id:</p>
     * <pre>
     * &lt;action type="CREATE"\u0026gt;
     *   &lt;fact type="ProteinAnnotation"\u0026gt;
     *     &lt;field attribute="type" value="keyword"/\u0026gt;
     *   &lt;/fact&gt;
     * &lt;/action&gt;
     * </pre>
     * becomes:
     * <pre>
     * insertLogical(ProteinAnnotation.builder().withType("keyword").build());
     * </pre>
     */
    private void translateCreateAction(RuleFact ruleFact, Action action, Set<String> rhsDeclaredVariables) {
        var factId = ruleFact.getId();
        if (StringUtils.isNotEmpty(factId)) {
            writer.write(ruleFact.getType().getLocalPart());
            writer.write(" ");
            writeVariableReference(factId, rhsDeclaredVariables);
            writer.write(" = ");
            translateCreateValue(ruleFact, action, rhsDeclaredVariables);
            writer.write(";");
            newLines(1);
            indent(2);
        }
        writer.write("insertLogical");
        openParenthesis();
        if (StringUtils.isNotEmpty(factId)) {
            writeVariableReference(factId, rhsDeclaredVariables);
        } else {
            translateCreateValue(ruleFact, action, rhsDeclaredVariables);
        }
        closeParenthesis();
    }

    private void translateCreateValue(RuleFact ruleFact, Action action, Set<String> rhsDeclaredVariables) {
        if (ruleFact.getCall() == null) {
            translateToBuildStatement(ruleFact, action.getWith(), rhsDeclaredVariables);
        } else {
            translateProceduralCall(ruleFact.getCall(), rhsDeclaredVariables);
        }
    }

    /**
     * Transpiles an UPDATE action into an {@code update(...)} call followed by setter calls.
     *
     * <p>Example:</p>
     * <pre>
     * &lt;action type="UPDATE"&gt;
     *   &lt;fact type="ProteinAnnotation" id="ann"&gt;
     *     &lt;field attribute="value" value="Updated"/&gt;
     *   &lt;/fact&gt;
     * &lt;/action&gt;
     * </pre>
     * becomes:
     * <pre>
     * update($ann);
     * $ann.setValue("Updated");
     * </pre>
     */
    private void translateUpdateAction(RuleFact ruleFact, Action action, Set<String> rhsDeclaredVariables) {
        writer.write("update");
        openParenthesis();
        writeVariableReference(ruleFact.getId(), rhsDeclaredVariables);
        closeParenthesis();
        writer.write(";");
        newLines(1);
        indent(2);
        if (ruleFact.getCall() == null) {
            translateToSetStatements(ruleFact, rhsDeclaredVariables);
        } else {
            translateProceduralCall(ruleFact.getCall(), rhsDeclaredVariables);
        }
    }

    /**
     * Transpiles a REMOVE action into a {@code retract(...)} statement.
     *
     * <p>Example: {@code &lt;action type="REMOVE"&gt;&lt;fact type="ProteinAnnotation" id="ann"/&gt;&lt;/action&gt;}
     * becomes {@code retract($ann);}.</p>
     */
    private void translateRemoveAction(RuleFact ruleFact, Action action, Set<String> rhsDeclaredVariables) {
        writer.write("retract");
        openParenthesis();
        writeVariableReference(ruleFact.getId(), rhsDeclaredVariables);
        closeParenthesis();
    }

    /**
     * Emits the setter calls for every field and wired value of an UPDATE action.
     *
     * <p>Multiple fields are emitted as separate setter calls, e.g.:</p>
     * <pre>
     * $ann.setValue("Updated");
     * $ann.setEvidence("UpdatedEvidence");
     * </pre>
     */
    private void translateToSetStatements(RuleFact ruleFact, Set<String> rhsDeclaredVariables) {
        for (Field field : ruleFact.getField()) {
            writeVariableReference(ruleFact.getId(), rhsDeclaredVariables);
            writer.write(".set");
            writer.write(capitalizeFirstLetter(field.getAttribute()));
            openParenthesis();
            if (field.getIsReference()) {
                writeVariableReference(field.getValue(), rhsDeclaredVariables);
            } else {
                writeValue(field.getValue(), getJavaType(ruleFact.getType(), field.getAttribute()));
            }
            closeParenthesis();
            writer.write(";");
            newLines(1);
        }

        // Update wired value
        for (String wired : ruleFact.getWith()) {
            translateSetter(ruleFact.getId(), wired, rhsDeclaredVariables);
            writer.write(";");
            newLines(1);
        }
    }

    /**
     * Writes a variable name, prefixing it with {@code $} unless it was declared
     * on the RHS (e.g., by a DECLARE action).
     *
     * <p>Dotted references such as {@code newPositionalMapping.mappedStart} are handled by
     * checking only the root variable name before the first dot.</p>
     */
    private void writeVariableReference(String variable, Set<String> rhsDeclaredVariables) {
        String rootVariable = variable.contains(".") ? variable.substring(0, variable.indexOf('.')) : variable;
        if (!rhsDeclaredVariables.contains(rootVariable)) {
            writer.write("$");
        }
        writer.write(variable);
    }

    /**
     * Emits a builder chain for a fact being created or declared.
     *
     * <p>Combines wired fields (from the action and the fact) and literal fields
     * into a single expression, e.g.:</p>
     * <pre>
     * ProteinAnnotation.builder().withType("keyword").withValue("Nucleus").build()
     * </pre>
     */
    private void translateToBuildStatement(RuleFact ruleFact, List<String> wiredFields, Set<String> rhsDeclaredVariables) {
        writer.write(ruleFact.getType().getLocalPart());
        writer.write(".builder()");
        for (String wired : ruleFact.getWith()) {
            translateWiredReference(wired, rhsDeclaredVariables);
        }
        for (String wired : wiredFields) {
            translateWiredReference(wired, rhsDeclaredVariables);
        }
        ruleFact.getField().forEach(f -> translateBuilderField(f, ruleFact, rhsDeclaredVariables));
        writer.write(".build()");
    }

    /**
     * Emits a {@code .withAttribute(value)} builder fragment for a wired reference.
     *
     * <p>Example: {@code "type:keyword"} → {@code .withType("keyword")}.</p>
     */
    private void translateWiredReference(String wiredReference, Set<String> rhsDeclaredVariables) {
        AttributeValue ref = parseAttributeValue(wiredReference);
        writer.write(".with");
        writer.write(capitalizeFirstLetter(ref.attribute()));
        openParenthesis();
        writeWiredValue(ref.value(), rhsDeclaredVariables);
        closeParenthesis();
    }

    /**
     * Emits a wired value, either as a quoted literal or as a variable reference.
     */
    private void writeWiredValue(String value, Set<String> rhsDeclaredVariables) {
        boolean isSimpleString = value.startsWith("'") && value.endsWith("'");
        if (isSimpleString) {
            writer.write(value.replace("'", "\""));
        } else {
            writeVariableReference(value, rhsDeclaredVariables);
        }
    }

    /** A colon-separated attribute/value pair such as {@code "type:keyword"}. */
    private record AttributeValue(String attribute, String value) {
    }

    /**
     * Parses a wired reference into its attribute and value halves.
     *
     * <p>When no separator is present, both halves default to the whole input,
     * e.g. {@code "protein"} → {@code attribute=protein, value=protein}.</p>
     */
    private AttributeValue parseAttributeValue(String wiredReference) {
        int separatorIndex = wiredReference.indexOf(':');
        if (separatorIndex == -1) {
            return new AttributeValue(wiredReference, wiredReference);
        }
        return new AttributeValue(
                wiredReference.substring(0, separatorIndex),
                wiredReference.substring(separatorIndex + 1));
    }

    /**
     * Emits a setter call for a wired value, e.g. {@code $ann.setValue("Updated")}.
     */
    private void translateSetter(String variable, String wiredReference, Set<String> rhsDeclaredVariables) {
        AttributeValue ref = parseAttributeValue(wiredReference);
        writeVariableReference(variable, rhsDeclaredVariables);
        writer.write(".set");
        writer.write(capitalizeFirstLetter(ref.attribute()));
        openParenthesis();
        writeWiredValue(ref.value(), rhsDeclaredVariables);
        closeParenthesis();
    }

    /**
     * Emits a procedural helper call in the RHS.
     *
     * <p>Example: {@code java://com.example.Helper#createAnnotation($protein)}
     * → {@code com.example.Helper.createAnnotation($protein)}.
     * Literal arguments are emitted without a {@code $} prefix.</p>
     *
     * <p>Reference arguments that name RHS-declared variables (e.g., from DECLARE)
     * are emitted without the {@code $} prefix.</p>
     */
    private void translateProceduralCall(ProceduralAttachment call, Set<String> rhsDeclaredVariables) {
        writer.write(extractPackage(call.getUri()));
        writer.write("." + call.getProcedure());
        openParenthesis();
        boolean firstIteration = true;
        for (ProcedureArgument procedureArgument : call.getArguments().getArgument()) {
            if (!firstIteration) {
                writer.write(", ");
            }
            if (procedureArgument.getIsReference()) {
                writeVariableReference(procedureArgument.getValue(), rhsDeclaredVariables);
            } else {
                writer.write(procedureArgument.getValue());
            }
            firstIteration = false;
        }
        closeParenthesis();
    }

    /**
     * Emits a builder field fragment from a literal or reference field.
     *
     * <p>Example: {@code <field attribute="type" value="keyword"/>} → {@code .withType("keyword")}.
     * References are emitted as variables, e.g. {@code value="protein" isReference="true"}
     * → {@code .withProtein($protein)}. References to RHS-declared variables are emitted
     * without the {@code $} prefix.</p>
     */
    private void translateBuilderField(Field field, RuleFact ruleFact, Set<String> rhsDeclaredVariables) {
        writer.write(".with");
        writer.write(capitalizeFirstLetter(field.getAttribute()));
        openParenthesis();
        if (field.getIsReference()) {
            writeVariableReference(field.getValue(), rhsDeclaredVariables);
        } else {
            writeValue(sanitizer.sanitize(field.getValue()), getJavaType(ruleFact.getType(), field.getAttribute()));
        }
        closeParenthesis();
    }

    /**
     * Transpiles a URML condition into a Drools LHS pattern.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Bind: {@code <condition on="Protein" bind="protein"/>}
     *       → {@code $protein : Protein()}</li>
     *   <li>With: {@code <condition on="ProteinSignature" with="protein"/>}
     *       → {@code ProteinSignature(protein == $protein)}</li>
     *   <li>Of: {@code <condition on="Organism" of="protein"/>}
     *       → {@code Organism(this == $protein.organism)}</li>
     *   <li>Exists: {@code <condition on="Organism" exists="true" of="protein"/>}
     *       → {@code exists Organism(this == $protein.organism)}</li>
     *   <li>Collect: {@code <condition on="ProteinSignature" bind="sigs" collect="true" with="protein"/>}
     *       → {@code $sigs := List() from collect (ProteinSignature(protein == $protein))}</li>
     * </ul>
     */
    private void translateCondition(Condition condition) {
        indent(3);
        if (condition.getBind() != null) {
            writer.write("$" + condition.getBind());
            writer.write(enspace(UNIFICATION));
        } else if (condition.getExists()) {
            writer.write("exists ");
        } else {
            writer.write("not ");
        }
        if (condition.getCollect()) {
            writer.write("List() from collect (");
        }
        writer.write(condition.getOn().getLocalPart());
        openParenthesis();
        boolean firstIteration = true;
        for (String bindingSpec : condition.getWith()) {
            if (!firstIteration) {
                writer.write(", ");
            }
            AttributeValue ref = parseAttributeValue(bindingSpec);
            writer.write(ref.attribute());
            writer.write(enspace(DroolsComparator.EQUALS.positive));
            writer.write("$" + ref.value());
            firstIteration = false;
        }
        for (String bindingSpec : condition.getOf()) {
            if (!firstIteration) {
                writer.write(", ");
            }
            AttributeValue ref = parseAttributeValue(bindingSpec);
            String attribute = ref.attribute().equals(ref.value())
                    ? condition.getOn().getLocalPart().toLowerCase()
                    : ref.attribute();
            writer.write("this");
            writer.write(enspace(DroolsComparator.EQUALS.positive));
            writer.write("$" + ref.value() + "." + attribute);
            firstIteration = false;
        }
        for (Filter filter : condition.getFilter()) {
            if (!firstIteration) {
                writer.write(", ");
            }
            translateFilter(condition.getOn(), filter);
            firstIteration = false;
        }

        closeParenthesis();

        if (condition.getCollect()) {
            closeParenthesis();
        }
        newLines(1);
    }

    /**
     * Dispatches a URML filter to the appropriate comparator translation.
     *
     * <p>Supported filters include: contains, in, range, nested field filters,
     * simple value, reference, startsWith, matches, and boolean existence.</p>
     */
    private void translateFilter(QName on, Filter filter) {
        if (filter.getContains() != null) {
            translateMultiValueFilter(on, filter.getContains(), filter, DroolsComparator.CONTAINS);
        } else if (filter.getIn() != null) {
            translateMultiValueFilter(on, filter.getIn(), filter, DroolsComparator.EQUALS);
        } else if (filter.getRange() != null) {
            translateRangeFilter(on, filter.getRange(), filter);
        } else if (!filter.getField().isEmpty()) {
            translateFieldFilter(on, filter.getField(), filter);
        } else if (filter.getValue() != null) {
            translateSimpleValueFilter(on, filter.getValue(), filter);
        } else if (filter.getRef() != null) {
            translateStringFilter(on, "$" + filter.getRef(), filter);
        } else if (filter.getStartsWith() != null) {
            translateStartsWithFilter(on, filter.getStartsWith(), filter);
        } else if (filter.getMatches() != null) {
            translateMatchesFilter(on, filter.getMatches(), filter);
        } else {
            writer.write(filter.getOn());
            writer.write(enspace(DroolsComparator.EQUALS.positive));
            writer.write(String.valueOf(!filter.getNegative()));
        }
    }

    private void translateFilterConstraint(QName on, Filter constraint, String attribute, DroolsComparator comparator, String value) {
        String fullAttribute = constraint.getOn();
        if (attribute != null) {
            fullAttribute = fullAttribute + "." + attribute;
        }
        translateNullSafeAttribute(fullAttribute);
        writer.write(enspace(comparator.getValue(constraint.getNegative())));
        writeValue(value, getJavaType(on, fullAttribute));
    }

    private void translateNullSafeAttribute(String fullAttribute) {
        StringBuilder attrBuilder = new StringBuilder();
        String[] splittedAttributes = fullAttribute.split("\\.");
        for (int i = 0; i < splittedAttributes.length - 1; i++) {
            attrBuilder.append(splittedAttributes[i]).append("!").append(".");
        }
        attrBuilder.append(splittedAttributes[splittedAttributes.length - 1]);
        writer.write(attrBuilder.toString());
    }

    private void translateFilterConstraint(QName on, Filter constraint, DroolsComparator comparator, String value) {
        translateFilterConstraint(on, constraint, null, comparator, value);
    }

    private void translateStartsWithFilter(QName on, StartsWith startsWith, Filter constraint) {
        translateFilterConstraint(on, constraint, DroolsComparator.MATCHES, startsWith.getValue() + ".*");
    }

    private void translateMatchesFilter(QName on, Matches matches, Filter constraint) {
        translateFilterConstraint(on, constraint, DroolsComparator.MATCHES, matches.getValue());
    }

    private void translateStringFilter(QName on, String value, Filter constraint) {
        translateFilterConstraint(on, constraint, DroolsComparator.EQUALS, value);
    }

    private void translateSimpleValueFilter(QName on, SimpleValue value, Filter constraint) {
        translateFilterConstraint(on, constraint, DroolsComparator.EQUALS, value.getValue());
    }

    private void translateFieldFilter(QName on, List<Field> fields, Filter constraint) {
        boolean firstIteration = true;
        for (Field field : fields) {
            if (!firstIteration) {
                writer.write(", ");
            }
            String value = field.getIsReference() ? "$" + field.getValue() : field.getValue();
            translateFilterConstraint(on, constraint, field.getAttribute(), DroolsComparator.EQUALS, value);
            firstIteration = false;
        }
    }

    private void translateRangeFilter(QName on, Range range, Filter constraint) {
        if (range.isSetStart()) {
            translateFilterConstraint(on, constraint, DroolsComparator.GREATER_THAN_OR_EQ, String.valueOf(range.getStart()));
        }
        if (range.isSetStart() && range.isSetEnd()) {
            writer.write(", ");
        }
        if (range.isSetEnd()) {
            translateFilterConstraint(on, constraint, DroolsComparator.LESS_THAN_OR_EQ, String.valueOf(range.getEnd()));
        }
    }

    /**
     * Transpiles a multi-value filter into a parenthesized boolean expression.
     *
     * <p>Example with {@code ANY}: {@code values=[a,b]} → {@code (this == a || this == b)}.
     * Example with {@code ALL}: {@code values=[a,b]} → {@code (this == a && this == b)}.</p>
     */
    private void translateMultiValueFilter(QName on, MultiValue multiValue, Filter constraint, DroolsComparator comparator) {
        openParenthesis();
        boolean firstIteration = true;
        for (SimpleValue value : multiValue.getValue()) {
            if (!firstIteration) {
                boolean orRelation = LogicalOperator.ANY.equals(multiValue.getOperator());
                writer.write(enspace(orRelation ? "||" : "&&"));
            }
            translateFilterConstraint(on, constraint, comparator, value.getValue());
            firstIteration = false;
        }
        closeParenthesis();
    }

    private void writeValue(String value, Class<?> javaType) {
        if (value.startsWith("$") || "null".equals(value)) {
            writer.write(value);
        } else if (Enum.class.isAssignableFrom(javaType)) {
            writer.write(javaType.getSimpleName() + ".fromValue(\"" + value + "\")");
        } else if (ClassUtils.isAssignable(javaType, Number.class) && StringUtils.isNumeric(value)) {
            writer.write(value);
        } else if (ClassUtils.isAssignable(javaType, Boolean.class)) {
            writer.write(value);
        } else if (String.class.isAssignableFrom(javaType)) {
            writer.write(enquote(value));
        } else {
            throw new IllegalArgumentException("Unsupported value " + value + " of type " + javaType);
        }
    }

    private String enquote(String input) {
        return wrap(input, QUOTE);
    }

    private String enspace(String input) {
        return wrap(input, " ");
    }

    private String wrap(String input, String wrapping) {
        return wrapping + input + wrapping;
    }

    private void indent(int times) {
        writer.write(StringUtils.repeat(INDENT_UNIT, times));
    }

    private void newLines(int times) {
        writer.write(StringUtils.repeat(NEW_LINE, times));
    }

    private void openParenthesis() {
        writer.write('(');
    }

    private void closeParenthesis() {
        writer.write(')');
    }

    private String capitalizeFirstLetter(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private Class<?> getJavaType(QName on, String fullAttribute) {
        try {
            return FactModelHelper.getFactAttribute(on, fullAttribute.split("\\.")).getAttributeType();
        } catch (FactModelReflectionException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
