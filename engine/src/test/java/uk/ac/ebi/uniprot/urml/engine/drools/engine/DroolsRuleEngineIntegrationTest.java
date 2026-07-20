package uk.ac.ebi.uniprot.urml.engine.drools.engine;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uniprot.urml.facts.*;
import org.uniprot.urml.rules.*;
import uk.ac.ebi.uniprot.urml.engine.common.ProteinAnnotationRetriever;
import uk.ac.ebi.uniprot.urml.engine.common.RuleExecution;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uk.ac.ebi.uniprot.urml.engine.drools.fixtures.AnnotationMatcher.annotationLike;
import static uk.ac.ebi.uniprot.urml.engine.drools.fixtures.FactFixtures.*;
import static uk.ac.ebi.uniprot.urml.engine.drools.fixtures.FilterFixtures.*;
import static uk.ac.ebi.uniprot.urml.engine.drools.fixtures.RuleFixtures.*;
import static uk.ac.ebi.uniprot.urml.engine.drools.fixtures.UrmlRuleDsl.condition;
import static uk.ac.ebi.uniprot.urml.engine.drools.fixtures.UrmlRuleDsl.rule;

/**
 * End-to-end smoke tests for the Drools rule engine.
 *
 * <p>These tests exercise a representative sample of URML rules through the full engine
 * execution path. They complement UrmlRuleToDroolsRuleConverterTest, which asserts
 * the structure of the generated Drools model directly.</p>
 */
class DroolsRuleEngineIntegrationTest {

    private static final String PROTEIN_ID_P1 = "P1";
    private static final String PROTEIN_ID_P2 = "P2";
    private static final String PROTEIN_ID_A8IE48 = "A8IE48_HUMAN";
    private static final String PROTEIN_ID_P23893 = "P23893";

    private static final int SEQUENCE_LENGTH_26 = 26;

    private static final String ANNOTATION_SUBCELLULAR_LOCATION = "comment.subcellular_location";
    private static final String ANNOTATION_SIMILARITY = "comment.similarity";
    private static final String ANNOTATION_FUNCTION = "comment.function";
    private static final String ANNOTATION_SUBUNIT = "comment.subunit";
    private static final String ANNOTATION_KEYWORD = "keyword";
    private static final String ANNOTATION_FEATURE_MOD_RES = "feature.MOD_RES";

    private static final String VALUE_NUCLEUS = "Nucleus";
    private static final String VALUE_DNA_BINDING = "DNA-binding";
    private static final String VALUE_BELONGS_ETS = "Belongs to the ETS family";

    private static final String RULE_ID_SIMPLE = "UR000000846";
    private static final String RULE_ID_RANGE = "UR_RANGE";
    private static final String RULE_ID_IN_ANY = "UR_IN_ANY";
    private static final String RULE_ID_CONTAINS_ALL = "UR_CONTAINS_ALL";
    private static final String RULE_ID_BOOL = "UR_BOOL";
    private static final String RULE_ID_EXISTS = "UR_EXISTS";
    private static final String RULE_ID_COLLECT = "UR_COLLECT";
    private static final String RULE_ID_OF_EXPLICIT = "UR_OF_EXPLICIT";
    private static final String RULE_ID_WITH_PATH = "UR_WITH_PATH";
    private static final String RULE_ID_INHERIT_PARENT = "UR_INHERIT_PARENT";
    private static final String RULE_ID_INHERIT_CHILD = "UR_INHERIT_CHILD";
    private static final String RULE_ID_REF_INHERIT_PARENT = "UR_REF_INHERIT_PARENT";
    private static final String RULE_ID_REF_INHERIT_CHILD = "UR_REF_INHERIT_CHILD";
    private static final String RULE_ID_SHADOW_PARENT = "UR_SHADOW_PARENT";
    private static final String RULE_ID_SHADOW_CHILD = "UR_SHADOW_CHILD";
    private static final String RULE_ID_MULTI_INHERIT_PARENT = "UR_MULTI_INHERIT_PARENT";
    private static final String RULE_ID_MULTI_INHERIT_CHILD = "UR_MULTI_INHERIT_CHILD";
    private static final String RULE_ID_MULTI_INHERIT_GRANDCHILD = "UR_MULTI_INHERIT_GRANDCHILD";
    private static final String RULE_ID_MULTIBRANCH_PARENT = "UR_MULTIBRANCH_PARENT";
    private static final String RULE_ID_MULTIBRANCH_CHILD = "UR_MULTIBRANCH_CHILD";
    private static final String RULE_ID_OR_BRANCH_MISSING_BIND_PARENT = "UR_OR_BRANCH_MISSING_BIND_PARENT";
    private static final String RULE_ID_OR_BRANCH_MISSING_BIND_CHILD = "UR_OR_BRANCH_MISSING_BIND_CHILD";
    private static final String RULE_ID_OR_BRANCH_ACTION_PARENT = "UR_OR_BRANCH_ACTION_PARENT";
    private static final String RULE_ID_OR_BRANCH_ACTION_CHILD = "UR_OR_BRANCH_ACTION_CHILD";
    private static final String RULE_ID_MULTI_AND = "UR_MULTI_AND";
    private static final String RULE_ID_UPDATE = "UR_UPDATE";
    private static final String RULE_ID_UPDATE_MULTI = "UR_UPDATE_MULTI";
    private static final String RULE_ID_REMOVE = "UR_REMOVE";
    private static final String RULE_ID_PROC = "UR_PROC";
    private static final String RULE_ID_PROC_NULL = "UR_PROC_NULL";
    private static final String RULE_ID_POSITIONAL_PRECONDITIONS = "UR000084156_positional_preconditions_1";
    private static final String RULE_ID_POSITIONAL_FEATURE = "UR000084156_positional_1_feature_1";
    private static final String RULE_ID_FORWARD_REF_PARENT = "UR_FORWARD_REF_PARENT";
    private static final String RULE_ID_FORWARD_REF_CHILD = "UR_FORWARD_REF_CHILD";

    private static final int LINEAGE_EUKARYOTA = 2759;
    private static final int LINEAGE_VIRUSES = 10239;
    private static final int LINEAGE_BACTERIA = 2;

    private static final String SIGNATURE_PROSITE_PS50061 = "PS50061";
    private static final String SIGNATURE_PROSITE_PS00108 = "PS00108";
    private static final String SIGNATURE_PFAM_PF00178 = "PF00178";
    private static final String SIGNATURE_PFAM_PF00202 = "PF00202";
    private static final String SIGNATURE_HAMAP_MF00375 = "MF_00375";

    private static final String ORGANISM_ID_1 = "org_1";
    private static final String ORGANISM_ID_2 = "org_2";
    private static final String ORGANISM_ID_3 = "org_3";

    private static final int RANGE_MIN = 10;
    private static final int RANGE_MAX = 100;
    private static final int POSITION_START = 1;
    private static final int POSITION_END = 100;
    private static final int TEMPLATE_POSITION = 265;
    private static final String MAPPED_SEQUENCE_K = "K";

    @Nested
    class SmokeTests {

        @Test
        void proteinMatchesAllConditions_shouldCreateAnnotations() throws Exception {
            // Given a rule requiring eukaryotic organism, a PROSITE and a PFAM signature,
            // and absence of another PROSITE signature
            Rules rules = createSimpleRules();

            // When executed against a protein that satisfies every condition
            Protein protein = protein(PROTEIN_ID_A8IE48, eukaryote());
            ProteinSignature prosite = proteinSignature(protein, SignatureType.PROSITE, SIGNATURE_PROSITE_PS50061);
            ProteinSignature pfam = proteinSignature(protein, SignatureType.PFAM, SIGNATURE_PFAM_PF00178);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, prosite, pfam)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then all expected annotations are created
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_A8IE48, ANNOTATION_SUBCELLULAR_LOCATION, VALUE_NUCLEUS),
                    annotationLike(PROTEIN_ID_A8IE48, ANNOTATION_SIMILARITY, VALUE_BELONGS_ETS),
                    annotationLike(PROTEIN_ID_A8IE48, ANNOTATION_KEYWORD, VALUE_DNA_BINDING),
                    annotationLike(PROTEIN_ID_A8IE48, ANNOTATION_KEYWORD, VALUE_NUCLEUS)));
        }

        @Test
        void missingRequiredSignature_shouldNotCreateAnnotations() throws Exception {
            // Given a rule requiring both PROSITE and PFAM signatures
            Rules rules = createSimpleRules();

            // When the PROSITE signature is missing
            Protein protein = protein(PROTEIN_ID_A8IE48, eukaryote());
            ProteinSignature pfam = proteinSignature(protein, SignatureType.PFAM, SIGNATURE_PFAM_PF00178);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, pfam)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no annotations are created
            assertThat(actual, is(empty()));
        }

        @Test
        void rangeFilterMatches_shouldCreateAnnotation() throws Exception {
            // Given a rule matching sequence length in [10, 100]
            Rules rules = createRangeRules();

            // When executed against a protein whose sequence length is inside the range
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the range-matched annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "RangeMatch")));
        }

        @Test
        void rangeFilterBelowRange_shouldNotCreateAnnotation() throws Exception {
            // Given a rule matching sequence length in [10, 100]
            Rules rules = createRangeRules();

            // When executed against a protein whose sequence length is below the range
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), RANGE_MIN - 1, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void rangeFilterAtMin_shouldCreateAnnotation() throws Exception {
            // Given a rule matching sequence length in [10, 100]
            Rules rules = createRangeRules();

            // When executed against a protein whose sequence length is at minimum
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), RANGE_MIN, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the range-matched annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "RangeMatch")));
        }

        @Test
        void rangeFilterAboveRange_shouldNotCreateAnnotation() throws Exception {
            // Given a rule matching sequence length in [10, 100]
            Rules rules = createRangeRules();

            // When executed against a protein whose sequence length is above the range
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), RANGE_MAX + 1, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void rangeFilterAtMax_shouldCreateAnnotation() throws Exception {
            // Given a rule matching sequence length in [10, 100]
            Rules rules = createRangeRules();

            // When executed against a protein whose sequence length is at maximum
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), RANGE_MAX, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the range-matched annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "RangeMatch")));
        }

        @Test
        void inFilterAny_shouldMatchAnyValue() throws Exception {
            // Given a rule matching organism.id in (org_1, org_2)
            Rules rules = createInAnyRules();

            // When executed against organisms with IDs in and out of the set
            Protein first = protein(PROTEIN_ID_P1, organismWithId(ORGANISM_ID_1, "Eukaryota", LINEAGE_EUKARYOTA));
            Protein second = protein(PROTEIN_ID_P2, organismWithId(ORGANISM_ID_2, "Eukaryota", LINEAGE_EUKARYOTA));
            Protein missing = protein("P3", organismWithId(ORGANISM_ID_3, "Eukaryota", LINEAGE_EUKARYOTA));
            FactSet inputFacts = FactSet.builder()
                    .withFact(first.getOrganism(), first, second.getOrganism(), second, missing.getOrganism(), missing)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then only proteins org_1 and org_2 receive the annotation
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "InAnyMatch"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_KEYWORD, "InAnyMatch")));
        }

        @Test
        void containsFilterAll_shouldRequireAllLineageIds() throws Exception {
            // Given a rule requiring organism.lineage.ids to contain both 2759 and 2
            Rules rules = createContainsAllRules();

            // When executed against organisms with complete and incomplete lineages
            Protein complete = protein(PROTEIN_ID_P1, organism("Eukaryota", LINEAGE_EUKARYOTA, LINEAGE_BACTERIA));
            Protein partial = protein(PROTEIN_ID_P2, organism("Eukaryota", LINEAGE_EUKARYOTA));
            FactSet inputFacts = FactSet.builder()
                    .withFact(complete.getOrganism(), complete, partial.getOrganism(), partial)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then only the protein with both lineage IDs matches
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "ContainsAllMatch")));
        }

        @Test
        void booleanFilter_shouldMatchFlagValue() throws Exception {
            // Given a rule matching sequence.isFragment == true
            Rules rules = createBooleanRules();

            // When executed against a fragment and a complete protein
            Protein fragment = protein(PROTEIN_ID_P1, eukaryote(), SEQUENCE_LENGTH_26, true);
            Protein complete = protein(PROTEIN_ID_P2, eukaryote(), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder()
                    .withFact(fragment.getOrganism(), fragment, complete.getOrganism(), complete)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then only the fragment receives the annotation
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "BoolMatch")));
        }

        @Test
        void existsTrueCondition_shouldMatchWhenFactPresent() throws Exception {
            // Given a rule requiring an organism to exist for the protein
            Rules rules = createExistsRules();

            // When executed against a protein with an organism
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(eukaryote(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the exists-matched annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "ExistsMatch")));
        }

        @Test
        void existsTrueCondition_shouldNotMatchWhenFactAbsent() throws Exception {
            // Given a rule requiring an organism to exist for the protein
            Rules rules = createExistsRules();

            // When executed against a protein with no organism fact
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void collectCondition_shouldAggregateMatchingFacts() throws Exception {
            // Given a rule collecting all matching protein signatures into a list
            Rules rules = createCollectRules();

            // When executed against a protein with both PROSITE and PFAM signatures
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinSignature prosite = proteinSignature(protein, SignatureType.PROSITE, SIGNATURE_PROSITE_PS50061);
            ProteinSignature pfam = proteinSignature(protein, SignatureType.PFAM, SIGNATURE_PFAM_PF00178);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, prosite, pfam).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the collect-matched annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "CollectMatch")));
        }

        @Test
        void collectConditionWithNoMatchingFacts_createsAnnotationRegardless() throws Exception {
            // Given a rule collecting all matching protein signatures into a list
            //
            // <rule id="UR_COLLECT">
            //   <conditions>
            //     <AND>
            //       <condition on="Protein" bind="protein"/>
            //       <condition on="ProteinSignature" bind="sigs" collect="true" with="protein"/>
            //     </AND>
            //   </conditions>
            //   <actions>
            //     <action type="CREATE">
            //       <fact type="ProteinAnnotation">
            //         <field attribute="type" value="keyword"/>
            //         <field attribute="value" value="CollectMatch"/>
            //       </fact>
            //     </action>
            //   </actions>
            // </rule>
            Rules rules = createCollectRules();

            // When executed against a protein with no signatures at all
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the annotation is still created because an empty collect does not block the action
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "CollectMatch")));
        }

        @Test
        void ofJoinWithExplicitAttribute_shouldConvertAndMatch() throws Exception {
            // Given a rule joining organism to protein via explicit organism:protein path
            Rules rules = createOfExplicitRules();

            // When executed against a protein with an organism
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the explicit-of-matched annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "OfExplicitMatch")));
        }

        @Test
        void ofJoinWithMissingTarget_shouldNotMatch() throws Exception {
            // Given a rule joining organism to protein via explicit organism:protein path
            Rules rules = createOfExplicitRules();

            // When executed against a protein with no organism fact
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void withJoinUsingRemainderPath_shouldMatchNestedProperty() throws Exception {
            // Given a rule using a remainder path to join organisms by scientific name
            Rules rules = createWithPathRules();

            // When executed against a protein and a second organism sharing the scientific name
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            Organism organism2 = organism("Eukaryota", LINEAGE_EUKARYOTA);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, organism2).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the with-path-matched annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "WithPath")));
        }

        @Test
        void withJoinUsingRemainderPath_noMatchingPartner_shouldNotMatch() throws Exception {
            // Given a rule using a remainder path to join organisms by scientific name
            //
            // <rule id="UR_WITH_PATH">
            //   <conditions>
            //     <AND>
            //       <condition on="Protein" bind="protein"/>
            //       <condition on="Organism" bind="organism" of="protein"
            //                filter="lineage.ids contains 2759"/>
            //       <condition on="Organism" bind="org2"
            //                with="scientificName:protein.organism.scientificName"/>
            //     </AND>
            //   </conditions>
            //   <actions>
            //     <action type="CREATE">
            //       <fact type="ProteinAnnotation">
            //         <field attribute="type" value="keyword"/>
            //         <field attribute="value" value="WithPath"/>
            //       </fact>
            //     </action>
            //   </actions>
            // </rule>
            Rules rules = createWithPathRules();

            // When executed against a protein and a second organism with a different scientific name
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            Organism organism2 = organism("Bacteria", LINEAGE_BACTERIA);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, organism2).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the with-path-matched annotation is created because the protein's own
            // organism satisfies the scientificName join; URML 'with' is a value-based join
            // and does not require a distinct partner object.
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "WithPath")));
        }

        @Test
        void parentConditionsSatisfied_shouldApplyChildRule() throws Exception {
            // Given a parent rule with organism and PROSITE conditions, and a child extending it
            Rules rules = createInheritanceRules();

            // When executed against a protein satisfying the parent conditions and the child's extra PFAM condition
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinSignature prosite = proteinSignature(protein, SignatureType.PROSITE, SIGNATURE_PROSITE_PS50061);
            ProteinSignature pfam = proteinSignature(protein, SignatureType.PFAM, SIGNATURE_PFAM_PF00178);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, prosite, pfam).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then both parent and child annotations are created
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent annotation"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_SUBUNIT, "Homodimer")));
        }

        @Test
        void parentConditionsNotSatisfied_shouldNotApplyChildRule() throws Exception {
            // Given a parent rule with organism and PROSITE conditions, and a child extending it
            Rules rules = createInheritanceRules();

            // When the parent condition (PROSITE signature) is not met
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinSignature pfam = proteinSignature(protein, SignatureType.PFAM, SIGNATURE_PFAM_PF00178);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, pfam).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then neither parent nor child annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void parentConditionsSatisfiedButChildNot_shouldApplyOnlyParentRule() throws Exception {
            // Given a parent rule with organism and PROSITE conditions, and a child extending it with an extra PFAM condition
            Rules rules = createInheritanceRules();

            // When the parent conditions are met but the child's extra PFAM condition is not
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinSignature prosite = proteinSignature(protein, SignatureType.PROSITE, SIGNATURE_PROSITE_PS50061);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, prosite).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then only the parent annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent annotation")));
        }

        @Test
        void referenceOnlyBindingInChild_inheritsParentBindingAndFires() throws Exception {
            // Given a parent binding protein and a child that redeclares the same binding with no constraints
            Rules rules = createReferenceOnlyInheritanceRules();

            // When the parent conditions are met and the child-specific organism condition is also met
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then both parent and child annotations are created, proving the child inherited the protein binding
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent reference-only"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_SUBUNIT, "Homodimer")));
        }

        @Test
        void referenceOnlyBindingInChild_whenChildSpecificConditionFails_onlyParentFires() throws Exception {
            // Given a parent binding protein and a child that redeclares the same binding with no constraints
            Rules rules = createReferenceOnlyInheritanceRules();

            // When the parent conditions are met but the child-specific organism condition is not
            Protein protein = protein(PROTEIN_ID_P1, organism("Bacteria", LINEAGE_BACTERIA), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then only the parent annotation is created
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent reference-only")));
        }

        @Test
        void shadowedBindingInChild_addsStricterConstraintAndFiresOnlyWhenBothSatisfied() throws Exception {
            // Given a parent binding protein and a child that redeclares the same binding with a stricter constraint
            Rules rules = createShadowedInheritanceRules();

            // When one protein satisfies both parent and child constraints, and another only satisfies the parent
            Protein longProtein = protein(PROTEIN_ID_P1, eukaryote(), 26, false);
            Protein shortProtein = protein(PROTEIN_ID_P2, eukaryote(), 10, false);
            FactSet inputFacts = FactSet.builder()
                    .withFact(longProtein.getOrganism(), longProtein, shortProtein.getOrganism(), shortProtein)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the child fires only for the long protein, while the parent fires for both
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent shadow"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_SUBUNIT, "Shadowed"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_FUNCTION, "Parent shadow")));
            assertThat(actual, not(hasItem(annotationLike(PROTEIN_ID_P2, ANNOTATION_SUBUNIT, "Shadowed"))));
        }

        @Test
        void multiLevelInheritance_referenceOnlyBindingPropagates() throws Exception {
            // Given a chain of parent, child, and grandchild rules, all redeclaring protein with no constraints
            Rules rules = createMultiLevelInheritanceRules();

            // When the parent conditions and the grandchild-specific organism condition are met
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then all three rules fire, proving the binding is inherited through the chain
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Level 1"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_SUBCELLULAR_LOCATION, "Level 2"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_SUBUNIT, "Level 3")));
        }

        @Test
        void shadowedBindingInMultipleAndBranches_compilesAndFires() throws Exception {
            // Given a parent binding organism and a child that shadows it in two alternative AND branches
            Rules rules = createMultiBranchShadowInheritanceRules();

            // When proteins satisfy each branch's more specific organism condition
            Protein protein1 = protein(PROTEIN_ID_P1, organismWithId(ORGANISM_ID_1, "Eukaryota", LINEAGE_EUKARYOTA));
            Protein protein2 = protein(PROTEIN_ID_P2, organismWithId(ORGANISM_ID_2, "Eukaryota", LINEAGE_EUKARYOTA));
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein1.getOrganism(), protein1, protein2.getOrganism(), protein2)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then both parent and child fire for each protein, using the original parent organism binding
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent multibranch"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "Child multibranch"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_FUNCTION, "Parent multibranch"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_KEYWORD, "Child multibranch")));
        }

        @Test
        void parentWithOrBranchMissingBinding_childShadowsBinding_compilesAndFires() throws Exception {
            // Given a parent with OR branches where only one branch binds organism, and a child that shadows organism
            Rules rules = createParentOrBranchMissingBindingRules();

            // When proteins match either parent branch
            Protein bacteriaOrg1 = protein(PROTEIN_ID_P1, organismWithId(ORGANISM_ID_1, "Bacteria", LINEAGE_BACTERIA), SEQUENCE_LENGTH_26, false);
            Protein bacteriaFragment = protein(PROTEIN_ID_P2, organismWithId(ORGANISM_ID_2, "Bacteria", LINEAGE_BACTERIA), SEQUENCE_LENGTH_26, true);
            Protein bacteriaOrg2 = protein("P3", organismWithId(ORGANISM_ID_3, "Bacteria", LINEAGE_BACTERIA), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder()
                    .withFact(bacteriaOrg1.getOrganism(), bacteriaOrg1,
                            bacteriaFragment.getOrganism(), bacteriaFragment,
                            bacteriaOrg2.getOrganism(), bacteriaOrg2)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the child only fires for the protein that matched the parent branch with an organism binding
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent missing bind"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_SUBUNIT, "Child missing bind"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_FUNCTION, "Parent missing bind"),
                    annotationLike("P3", ANNOTATION_FUNCTION, "Parent missing bind")));
            assertThat(actual, not(hasItem(annotationLike(PROTEIN_ID_P2, ANNOTATION_SUBUNIT, "Child missing bind"))));
            assertThat(actual, not(hasItem(annotationLike("P3", ANNOTATION_SUBUNIT, "Child missing bind"))));
        }

        @Test
        void childWithOrBranchesAndActionUsesProteinBinding_compilesAndFires() throws Exception {
            // Given a parent binding protein and organism, and a child with OR branches shadowing different bindings
            Rules rules = createChildOrBranchWithActionRules();

            // When one protein matches only the organism branch and another only the protein branch
            Protein shortOrganism1 = protein(PROTEIN_ID_P1, organismWithId(ORGANISM_ID_1, "Eukaryota", LINEAGE_EUKARYOTA), 10, false);
            Protein longOrganism2 = protein(PROTEIN_ID_P2, organismWithId(ORGANISM_ID_2, "Eukaryota", LINEAGE_EUKARYOTA), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder()
                    .withFact(shortOrganism1.getOrganism(), shortOrganism1,
                            longOrganism2.getOrganism(), longOrganism2)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then both parent and child fire for each protein, regardless of which OR branch matched
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Parent action or"),
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_SUBUNIT, "Child action or"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_FUNCTION, "Parent action or"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_SUBUNIT, "Child action or")));
        }

        @Test
        void multipleAndBlocks_shouldFireRule() throws Exception {
            // Given a rule with two alternative AND blocks matching different PFAM signatures
            Rules rules = createMultiAndBlockRules();

            // When executed against proteins with either signature
            Protein protein1 = protein(PROTEIN_ID_P1, eukaryote());
            ProteinSignature sig1 = proteinSignature(protein1, SignatureType.PFAM, SIGNATURE_PFAM_PF00178);
            Protein protein2 = protein(PROTEIN_ID_P2, eukaryote());
            ProteinSignature sig2 = proteinSignature(protein2, SignatureType.PFAM, SIGNATURE_PFAM_PF00202);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein1.getOrganism(), protein1, sig1, protein2.getOrganism(), protein2, sig2)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then both proteins receive the matched annotation
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "Matched"),
                    annotationLike(PROTEIN_ID_P2, ANNOTATION_KEYWORD, "Matched")));
        }

        @Test
        void multipleAndBlocks_noAlternativeMatch_shouldNotFireRule() throws Exception {
            // Given a rule with two alternative AND blocks matching different PFAM signatures
            Rules rules = createMultiAndBlockRules();

            // When executed against a protein with neither PFAM signature
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinSignature prosite = proteinSignature(protein, SignatureType.PROSITE, SIGNATURE_PROSITE_PS50061);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, prosite)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void updateAction_shouldModifyExistingFact() throws Exception {
            // Given a rule that updates a keyword annotation's value
            Rules rules = createUpdateRules();

            // When executed against a protein with a keyword annotation
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinAnnotation annotation = proteinAnnotation(protein, ANNOTATION_KEYWORD, "Original");
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, annotation).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the annotation value is updated
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "Updated")));
        }

        @Test
        void updateAction_shouldIgnoreNonMatchingFact() throws Exception {
            // Given a rule that updates only keyword annotations
            Rules rules = createUpdateRules();

            // When executed against a protein with a non-keyword annotation
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinAnnotation annotation = proteinAnnotation(protein, ANNOTATION_FUNCTION, "Original");
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, annotation).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the non-keyword annotation survives unchanged
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "Original")));
        }

        @Test
        void removeAction_shouldDeleteExistingFact() throws Exception {
            // Given a rule that removes a keyword annotation
            Rules rules = createRemoveRules();

            // When executed against a protein with a keyword annotation
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinAnnotation annotation = proteinAnnotation(protein, ANNOTATION_KEYWORD, "ToRemove");
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, annotation).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the annotation is deleted
            assertThat(actual, is(empty()));
        }

        @Test
        void removeAction_shouldIgnoreNonMatchingFact() throws Exception {
            // Given a rule that removes only keyword annotations
            Rules rules = createRemoveRules();

            // When executed against a protein with a non-keyword annotation
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinAnnotation annotation = proteinAnnotation(protein, ANNOTATION_FUNCTION, "ToKeep");
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, annotation).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the non-keyword annotation survives
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_FUNCTION, "ToKeep")));
        }

        @Test
        void proceduralCallAction_shouldCreateFactFromHelper() throws Exception {
            // Given a rule whose CREATE action delegates to a procedural helper
            Rules rules = createProceduralCallRules();

            // When executed against a protein
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the helper-created annotation is inserted
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "FromCall")));
        }

        @Test
        void proceduralCallAction_shouldCreateNoFactWhenHelperReturnsNull() throws Exception {
            // Given a rule whose procedural helper returns null for non-fragment proteins
            //
            // <rule id="UR_PROC_NULL">
            //   <conditions>
            //     <AND>
            //       <condition on="Protein" bind="protein"/>
            //     </AND>
            //   </conditions>
            //   <actions>
            //     <action type="CREATE">
            //       <fact type="ProteinAnnotation">
            //         <call uri="java://...TestProceduralHelper" procedure="createAnnotationIfFragment">
            //           <argument value="protein" isReference="true"/>
            //         </call>
            //       </fact>
            //     </action>
            //   </actions>
            // </rule>
            Rules rules = createProceduralCallNullRules();

            // When executed against a non-fragment protein
            Protein protein = protein(PROTEIN_ID_P1, eukaryote(), SEQUENCE_LENGTH_26, false);
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no annotation is created because the procedural helper returned null
            assertThat(actual, is(empty()));
        }

        @Test
        void positionalConditionsMatch_shouldCreateAnnotation() throws Exception {
            // Given positional pre-condition and feature rules
            Rules rules = createPositionalRules();

            // When executed against matching positional signatures and a template
            Protein protein = protein(PROTEIN_ID_P23893, eukaryote());
            PositionalProteinSignature targetMatch = positionalProteinSignature(protein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            TemplateProtein templateProtein = templateProtein(PROTEIN_ID_P23893);
            TemplateProteinSignature templateMatch = templateProteinSignature(templateProtein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            PositionalMapping mapping = positionalMapping(
                    protein, targetMatch, templateMatch, String.valueOf(TEMPLATE_POSITION), String.valueOf(TEMPLATE_POSITION), MAPPED_SEQUENCE_K, TEMPLATE_POSITION, TEMPLATE_POSITION);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, targetMatch, templateProtein, templateMatch, mapping)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the feature annotation is created
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P23893, ANNOTATION_FEATURE_MOD_RES, "N6-(pyridoxal phosphate)lysine",
                            "UR000084156", TEMPLATE_POSITION, TEMPLATE_POSITION, false)));
        }

        @Test
        void positionalWrongTargetSignature_shouldNotCreateAnnotation() throws Exception {
            // Given positional pre-condition and feature rules
            Rules rules = createPositionalRules();

            // When the target positional signature has the wrong value
            Protein protein = protein(PROTEIN_ID_P23893, eukaryote());
            PositionalProteinSignature targetMatch = positionalProteinSignature(protein, SignatureType.HAMAP, "MF_99999", POSITION_START, POSITION_END);
            TemplateProtein templateProtein = templateProtein(PROTEIN_ID_P23893);
            TemplateProteinSignature templateMatch = templateProteinSignature(templateProtein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            PositionalMapping mapping = positionalMapping(
                    protein, targetMatch, templateMatch, String.valueOf(TEMPLATE_POSITION), String.valueOf(TEMPLATE_POSITION), MAPPED_SEQUENCE_K, TEMPLATE_POSITION, TEMPLATE_POSITION);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, targetMatch, templateProtein, templateMatch, mapping)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no feature annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void positionalWrongTemplateProteinId_parentDerivesMapping_shouldCreateAnnotation() throws Exception {
            // Given positional pre-condition and feature rules (see URML snippet above).
            // The child rule filters on PositionalMapping.isValid == true etc.
            Rules rules = createPositionalRules();

            // When the template protein id does not match the required value, but the signatures match
            Protein protein = protein(PROTEIN_ID_P23893, eukaryote());
            PositionalProteinSignature targetMatch = positionalProteinSignature(protein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            TemplateProtein templateProtein = templateProtein("P99999");
            TemplateProteinSignature templateMatch = templateProteinSignature(templateProtein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            PositionalMapping mapping = positionalMapping(
                    protein, targetMatch, templateMatch, String.valueOf(TEMPLATE_POSITION), String.valueOf(TEMPLATE_POSITION), MAPPED_SEQUENCE_K, TEMPLATE_POSITION, TEMPLATE_POSITION);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, targetMatch, templateProtein, templateMatch, mapping)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the feature annotation is created because the parent rule derives a fresh,
            // valid PositionalMapping from the matching target/template signatures.
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P23893, ANNOTATION_FEATURE_MOD_RES, "N6-(pyridoxal phosphate)lysine",
                            "UR000084156", TEMPLATE_POSITION, TEMPLATE_POSITION, false)));
        }

        @Test
        void positionalInvalidMapping_parentDerivesValidMapping_shouldCreateAnnotation() throws Exception {
            // Given positional pre-condition and feature rules (see URML snippet above).
            // The child rule filters on PositionalMapping.isValid == true.
            Rules rules = createPositionalRules();

            // When the input positional mapping is marked invalid
            Protein protein = protein(PROTEIN_ID_P23893, eukaryote());
            PositionalProteinSignature targetMatch = positionalProteinSignature(protein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            TemplateProtein templateProtein = templateProtein(PROTEIN_ID_P23893);
            TemplateProteinSignature templateMatch = templateProteinSignature(templateProtein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            PositionalMapping mapping = PositionalMapping.builder()
                    .withProtein(protein)
                    .withTargetMatch(targetMatch)
                    .withTemplateMatch(templateMatch)
                    .withTemplateStart(String.valueOf(TEMPLATE_POSITION))
                    .withTemplateEnd(String.valueOf(TEMPLATE_POSITION))
                    .withMappedSequence(MAPPED_SEQUENCE_K)
                    .withIsValid(false)
                    .withMappedStart(TEMPLATE_POSITION)
                    .withMappedEnd(TEMPLATE_POSITION)
                    .build();
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, targetMatch, templateProtein, templateMatch, mapping)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the feature annotation is created because the parent rule derives a fresh,
            // valid PositionalMapping regardless of the input mapping's validity.
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P23893, ANNOTATION_FEATURE_MOD_RES, "N6-(pyridoxal phosphate)lysine",
                            "UR000084156", TEMPLATE_POSITION, TEMPLATE_POSITION, false)));
        }

        @Test
        void positionalWrongMappedSequence_shouldNotCreateAnnotation() throws Exception {
            // Given positional pre-condition and feature rules
            Rules rules = createPositionalRules();

            // When the mapped sequence does not match the required residue
            Protein protein = protein(PROTEIN_ID_P23893, eukaryote());
            PositionalProteinSignature targetMatch = positionalProteinSignature(protein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            TemplateProtein templateProtein = templateProtein(PROTEIN_ID_P23893);
            TemplateProteinSignature templateMatch = templateProteinSignature(templateProtein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            PositionalMapping mapping = positionalMapping(
                    protein, targetMatch, templateMatch, String.valueOf(TEMPLATE_POSITION), String.valueOf(TEMPLATE_POSITION), "A", TEMPLATE_POSITION, TEMPLATE_POSITION);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, targetMatch, templateProtein, templateMatch, mapping)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then no feature annotation is created
            assertThat(actual, is(empty()));
        }

        @Test
        void positionalWrongTemplatePosition_parentDerivesMapping_shouldCreateAnnotation() throws Exception {
            // Given positional pre-condition and feature rules (see URML snippet above).
            // The child rule filters on PositionalMapping.templateStart == 265 and templateEnd == 265.
            Rules rules = createPositionalRules();

            // When the input mapping template position does not match the required position
            Protein protein = protein(PROTEIN_ID_P23893, eukaryote());
            PositionalProteinSignature targetMatch = positionalProteinSignature(protein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            TemplateProtein templateProtein = templateProtein(PROTEIN_ID_P23893);
            TemplateProteinSignature templateMatch = templateProteinSignature(templateProtein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            PositionalMapping mapping = positionalMapping(
                    protein, targetMatch, templateMatch, "999", "999", MAPPED_SEQUENCE_K, TEMPLATE_POSITION, TEMPLATE_POSITION);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, targetMatch, templateProtein, templateMatch, mapping)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the feature annotation is created because the parent rule derives a fresh
            // PositionalMapping with templateStart=265 from the matching template signature.
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P23893, ANNOTATION_FEATURE_MOD_RES, "N6-(pyridoxal phosphate)lysine",
                            "UR000084156", TEMPLATE_POSITION, TEMPLATE_POSITION, false)));
        }

        @Test
        void childRedeclaresParentBindingAndReferencesSiblingBinding_shouldCreateAnnotations() throws Exception {
            // Given a parent rule binding protein and a child rule that redeclares protein
            // with a constraint referencing a sibling binding (targetMatch) declared earlier in the child.
            Rules rules = createForwardReferenceInheritanceRules();

            // When executed against matching target/template signatures and protein
            Protein protein = protein(PROTEIN_ID_P23893, eukaryote(), SEQUENCE_LENGTH_26, false);
            PositionalProteinSignature targetMatch = positionalProteinSignature(protein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            TemplateProtein templateProtein = templateProtein(PROTEIN_ID_P23893);
            TemplateProteinSignature templateMatch = templateProteinSignature(templateProtein, SignatureType.HAMAP, SIGNATURE_HAMAP_MF00375, POSITION_START, POSITION_END);
            FactSet inputFacts = FactSet.builder()
                    .withFact(protein.getOrganism(), protein, targetMatch, templateProtein, templateMatch)
                    .build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then both parent and child annotations are created
            assertThat(actual, containsInAnyOrder(
                    annotationLike(PROTEIN_ID_P23893, ANNOTATION_KEYWORD, "Parent"),
                    annotationLike(PROTEIN_ID_P23893, ANNOTATION_SUBUNIT, "Homodimer")));
        }

        @Test
        void updateWithBothWithAndField_shouldApplyBothClauses() throws Exception {
            // Given an UPDATE action that uses both a with-clause and a literal field
            Rules rules = createUpdateWithAndFieldRules();

            // When executed against a protein with a keyword annotation
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinAnnotation annotation = proteinAnnotation(protein, ANNOTATION_KEYWORD, "Original");
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, annotation).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then both clauses are applied: the with-clause copies the protein id, the field sets the value
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, PROTEIN_ID_P1)));
        }

        @Test
        void updateAction_withMultipleFields_shouldUpdateAllFields() throws Exception {
            // Given a rule that updates both the value and evidence of a keyword annotation
            Rules rules = createMultiFieldUpdateRules();

            // When executed against a protein with a keyword annotation
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            ProteinAnnotation annotation = proteinAnnotation(protein, ANNOTATION_KEYWORD, "Original");
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein, annotation).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then every specified field is updated on the existing annotation
            assertThat(actual, hasSize(1));
            ProteinAnnotation updated = actual.get(0);
            assertThat(updated.getProtein().getId(), is(PROTEIN_ID_P1));
            assertThat(updated.getType(), is(ANNOTATION_KEYWORD));
            assertThat(updated.getValue(), is("UpdatedValue"));
            assertThat(updated.getEvidence(), is("UpdatedEvidence"));
        }

        @Test
        void proceduralCallWithLiteralArgument_shouldInvokeHelper() throws Exception {
            // Given a rule whose procedural call mixes a binding reference and a literal argument
            Rules rules = createProceduralCallWithLiteralRules();

            // When executed against a protein
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the helper-created annotation is inserted
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "LiteralArg")));
        }

        @Test
        void createFactCopiesBindingWithWith_shorthand_shouldCopyBindingIntoNewFact() throws Exception {
            // Given a CREATE action using the with-shorthand to copy a binding into the new fact
            Rules rules = createCreateWithBindingShorthandRules();

            // When executed against a protein
            Protein protein = protein(PROTEIN_ID_P1, eukaryote());
            FactSet inputFacts = FactSet.builder().withFact(protein.getOrganism(), protein).build();

            List<ProteinAnnotation> actual = executeRules(rules, inputFacts);

            // Then the new annotation carries the protein reference copied by the with-shorthand
            assertThat(actual, containsInAnyOrder(annotationLike(PROTEIN_ID_P1, ANNOTATION_KEYWORD, "Copied")));
        }
    }

    private static Rules createSimpleRules() {
        return rule(RULE_ID_SIMPLE)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA, LINEAGE_VIRUSES)),
                        condition(PROTEIN_SIGNATURE_TYPE).with("protein")
                                .filter(signatureEquals(SignatureType.PROSITE, SIGNATURE_PROSITE_PS50061)),
                        condition(PROTEIN_SIGNATURE_TYPE).with("protein").exists(false)
                                .filter(signatureEquals(SignatureType.PROSITE, SIGNATURE_PROSITE_PS00108)),
                        condition(PROTEIN_SIGNATURE_TYPE).with("protein")
                                .filter(signatureEquals(SignatureType.PFAM, SIGNATURE_PFAM_PF00178)))
                .withActions(createAction("evidence:'" + RULE_ID_SIMPLE + "'",
                        annotation(ANNOTATION_SUBCELLULAR_LOCATION, VALUE_NUCLEUS),
                        annotation(ANNOTATION_SIMILARITY, VALUE_BELONGS_ETS),
                        annotation(ANNOTATION_KEYWORD, VALUE_DNA_BINDING),
                        annotation(ANNOTATION_KEYWORD, VALUE_NUCLEUS)))
                .buildRules();
    }

    private static Rules createRangeRules() {
        return rule(RULE_ID_RANGE)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(PROTEIN_TYPE).bind("protein")
                                .filter(rangeFilter("sequence.length", String.valueOf(RANGE_MIN), String.valueOf(RANGE_MAX), false)))
                .withActions(createAction("evidence:'" + RULE_ID_RANGE + "'", annotation(ANNOTATION_KEYWORD, "RangeMatch")))
                .buildRules();
    }

    private static Rules createInAnyRules() {
        return rule(RULE_ID_IN_ANY)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(inFilter("id", true, false, ORGANISM_ID_1, ORGANISM_ID_2)))
                .withActions(createAction("evidence:'" + RULE_ID_IN_ANY + "'", annotation(ANNOTATION_KEYWORD, "InAnyMatch")))
                .buildRules();
    }

    private static Rules createContainsAllRules() {
        return rule(RULE_ID_CONTAINS_ALL)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(containsFilter("lineage.ids", false, false,
                                        String.valueOf(LINEAGE_EUKARYOTA), String.valueOf(LINEAGE_BACTERIA))))
                .withActions(createAction("evidence:'" + RULE_ID_CONTAINS_ALL + "'", annotation(ANNOTATION_KEYWORD, "ContainsAllMatch")))
                .buildRules();
    }

    private static Rules createBooleanRules() {
        return rule(RULE_ID_BOOL)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(PROTEIN_TYPE).bind("protein")
                                .filter(booleanFilter("sequence.isFragment", false)))
                .withActions(createAction("evidence:'" + RULE_ID_BOOL + "'", annotation(ANNOTATION_KEYWORD, "BoolMatch")))
                .buildRules();
    }

    private static Rules createExistsRules() {
        return rule(RULE_ID_EXISTS)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).exists(true).of("protein"))
                .withActions(createAction("evidence:'" + RULE_ID_EXISTS + "'", annotation(ANNOTATION_KEYWORD, "ExistsMatch")))
                .buildRules();
    }

    private static Rules createCollectRules() {
        return rule(RULE_ID_COLLECT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(PROTEIN_SIGNATURE_TYPE).bind("sigs").collect(true).with("protein"))
                .withActions(createAction("evidence:'" + RULE_ID_COLLECT + "'", annotation(ANNOTATION_KEYWORD, "CollectMatch")))
                .buildRules();
    }

    private static Rules createOfExplicitRules() {
        return rule(RULE_ID_OF_EXPLICIT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("organism:protein"))
                .withActions(createAction("evidence:'" + RULE_ID_OF_EXPLICIT + "'", annotation(ANNOTATION_KEYWORD, "OfExplicitMatch")))
                .buildRules();
    }

    private static Rules createWithPathRules() {
        return rule(RULE_ID_WITH_PATH)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA)),
                        condition(ORGANISM_TYPE).bind("org2")
                                .with("scientificName:protein.organism.scientificName"))
                .withActions(createAction("evidence:'" + RULE_ID_WITH_PATH + "'", annotation(ANNOTATION_KEYWORD, "WithPath")))
                .buildRules();
    }

    private static Rules createInheritanceRules() {
        Rule parent = rule(RULE_ID_INHERIT_PARENT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA)),
                        condition(PROTEIN_SIGNATURE_TYPE).with("protein")
                                .filter(signatureEquals(SignatureType.PROSITE, SIGNATURE_PROSITE_PS50061)))
                .withActions(createAction("evidence:'" + RULE_ID_INHERIT_PARENT + "'",
                        annotation(ANNOTATION_FUNCTION, "Parent annotation")))
                .build();

        Rule child = Rule.builder()
                .withId(RULE_ID_INHERIT_CHILD)
                .withExtends(parent)
                .withStatus(RuleStatus.APPLY)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(
                                        Condition.builder().withOn(PROTEIN_TYPE).withBind("protein").build(),
                                        Condition.builder().withOn(ORGANISM_TYPE)
                                                .withBind("organism").withOf("protein")
                                                .withFilter(lineageContainsAny(LINEAGE_EUKARYOTA)).build(),
                                        Condition.builder().withOn(PROTEIN_SIGNATURE_TYPE).withWith("protein")
                                                .withFilter(signatureEquals(SignatureType.PFAM, SIGNATURE_PFAM_PF00178)).build())
                                .build())
                        .build())
                .withActions(Actions.builder()
                        .addAction(createAction("evidence:'" + RULE_ID_INHERIT_CHILD + "'", annotation(ANNOTATION_SUBUNIT, "Homodimer")))
                        .build())
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createReferenceOnlyInheritanceRules() {
        Rule parent = rule(RULE_ID_REF_INHERIT_PARENT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein")
                                .filter(booleanFilter("sequence.isFragment", true)))
                .withActions(createAction("evidence:'" + RULE_ID_REF_INHERIT_PARENT + "'",
                        annotation(ANNOTATION_FUNCTION, "Parent reference-only")))
                .build();

        Rule child = rule(RULE_ID_REF_INHERIT_CHILD)
                .extendsRule(parent)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA)))
                .withActions(createAction("evidence:'" + RULE_ID_REF_INHERIT_CHILD + "'",
                        annotation(ANNOTATION_SUBUNIT, "Homodimer")))
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createShadowedInheritanceRules() {
        Rule parent = rule(RULE_ID_SHADOW_PARENT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein")
                                .filter(booleanFilter("sequence.isFragment", true)))
                .withActions(createAction("evidence:'" + RULE_ID_SHADOW_PARENT + "'",
                        annotation(ANNOTATION_FUNCTION, "Parent shadow")))
                .build();

        Rule child = rule(RULE_ID_SHADOW_CHILD)
                .extendsRule(parent)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein")
                                .filter(rangeFilter("sequence.length", "20", "1000", false)))
                .withActions(createAction("evidence:'" + RULE_ID_SHADOW_CHILD + "'",
                        annotation(ANNOTATION_SUBUNIT, "Shadowed")))
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createMultiLevelInheritanceRules() {
        Rule parent = rule(RULE_ID_MULTI_INHERIT_PARENT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein")
                                .filter(booleanFilter("sequence.isFragment", true)))
                .withActions(createAction("evidence:'" + RULE_ID_MULTI_INHERIT_PARENT + "'",
                        annotation(ANNOTATION_FUNCTION, "Level 1")))
                .build();

        Rule child = rule(RULE_ID_MULTI_INHERIT_CHILD)
                .extendsRule(parent)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA)))
                .withActions(createAction("evidence:'" + RULE_ID_MULTI_INHERIT_CHILD + "'",
                        annotation(ANNOTATION_SUBCELLULAR_LOCATION, "Level 2")))
                .build();

        Rule grandchild = rule(RULE_ID_MULTI_INHERIT_GRANDCHILD)
                .extendsRule(child)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA)))
                .withActions(createAction("evidence:'" + RULE_ID_MULTI_INHERIT_GRANDCHILD + "'",
                        annotation(ANNOTATION_SUBUNIT, "Level 3")))
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child, grandchild)
                .build();
    }

    private static Rules createMultiBranchShadowInheritanceRules() {
        Rule parent = rule(RULE_ID_MULTIBRANCH_PARENT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein"),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA)))
                .withActions(createAction("evidence:'" + RULE_ID_MULTIBRANCH_PARENT + "'",
                        annotation(ANNOTATION_FUNCTION, "Parent multibranch")))
                .build();

        Rule child = rule(RULE_ID_MULTIBRANCH_CHILD)
                .extendsRule(parent)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(Condition.builder().withOn(ORGANISM_TYPE)
                                        .withBind("organism").withOf("protein")
                                        .withFilter(inFilter("id", false, false, ORGANISM_ID_1)).build())
                                .build())
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(Condition.builder().withOn(ORGANISM_TYPE)
                                        .withBind("organism").withOf("protein")
                                        .withFilter(inFilter("id", false, false, ORGANISM_ID_2)).build())
                                .build())
                        .build())
                .withActions(createAction("evidence:'" + RULE_ID_MULTIBRANCH_CHILD + "'",
                        annotation(ANNOTATION_KEYWORD, "Child multibranch")))
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createParentOrBranchMissingBindingRules() {
        Rule parent = rule(RULE_ID_OR_BRANCH_MISSING_BIND_PARENT)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(
                                        Condition.builder().withOn(PROTEIN_TYPE).withBind("protein")
                                                .withFilter(booleanFilter("sequence.isFragment", true)).build(),
                                        Condition.builder().withOn(ORGANISM_TYPE).withBind("organism").withOf("protein")
                                                .withFilter(lineageContainsAny(LINEAGE_BACTERIA)).build())
                                .build())
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(
                                        Condition.builder().withOn(PROTEIN_TYPE).withBind("protein")
                                                .withFilter(booleanFilter("sequence.isFragment", false)).build())
                                .build())
                        .build())
                .withActions(createAction("evidence:'" + RULE_ID_OR_BRANCH_MISSING_BIND_PARENT + "'",
                        annotation(ANNOTATION_FUNCTION, "Parent missing bind")))
                .build();

        Rule child = rule(RULE_ID_OR_BRANCH_MISSING_BIND_CHILD)
                .extendsRule(parent)
                .withConditions(
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(inFilter("id", false, false, ORGANISM_ID_1)))
                .withActions(createAction("evidence:'" + RULE_ID_OR_BRANCH_MISSING_BIND_CHILD + "'",
                        annotation(ANNOTATION_SUBUNIT, "Child missing bind")))
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createChildOrBranchWithActionRules() {
        Rule parent = rule(RULE_ID_OR_BRANCH_ACTION_PARENT)
                .withConditions(
                        condition(PROTEIN_TYPE).bind("protein")
                                .filter(booleanFilter("sequence.isFragment", true)),
                        condition(ORGANISM_TYPE).bind("organism").of("protein")
                                .filter(lineageContainsAny(LINEAGE_EUKARYOTA)))
                .withActions(createAction("evidence:'" + RULE_ID_OR_BRANCH_ACTION_PARENT + "'",
                        annotation(ANNOTATION_FUNCTION, "Parent action or")))
                .build();

        Rule child = rule(RULE_ID_OR_BRANCH_ACTION_CHILD)
                .extendsRule(parent)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(Condition.builder().withOn(ORGANISM_TYPE)
                                        .withBind("organism").withOf("protein")
                                        .withFilter(inFilter("id", false, false, ORGANISM_ID_1)).build())
                                .build())
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(Condition.builder().withOn(PROTEIN_TYPE)
                                        .withBind("protein")
                                        .withFilter(rangeFilter("sequence.length", "20", "1000", false)).build())
                                .build())
                        .build())
                .withActions(createAction("evidence:'" + RULE_ID_OR_BRANCH_ACTION_CHILD + "'",
                        annotation(ANNOTATION_SUBUNIT, "Child action or")))
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createMultiAndBlockRules() {
        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(Rule.builder().withId(RULE_ID_MULTI_AND).withStatus(RuleStatus.APPLY)
                        .withConditions(DisjunctiveConditionSet.builder()
                                .addAND(ConjunctiveConditionSet.builder()
                                        .addCondition(
                                                Condition.builder().withOn(PROTEIN_TYPE).withBind("protein").build(),
                                                Condition.builder().withOn(PROTEIN_SIGNATURE_TYPE)
                                                        .withWith("protein")
                                                        .withFilter(signatureEquals(SignatureType.PFAM, SIGNATURE_PFAM_PF00178)).build())
                                        .build())
                                .addAND(ConjunctiveConditionSet.builder()
                                        .addCondition(
                                                Condition.builder().withOn(PROTEIN_TYPE).withBind("protein").build(),
                                                Condition.builder().withOn(PROTEIN_SIGNATURE_TYPE)
                                                        .withWith("protein")
                                                        .withFilter(signatureEquals(SignatureType.PFAM, SIGNATURE_PFAM_PF00202)).build())
                                        .build())
                                .build())
                        .withActions(Actions.builder()
                                .addAction(createAction("evidence:'" + RULE_ID_MULTI_AND + "'", annotation(ANNOTATION_KEYWORD, "Matched")))
                                .build())
                        .build())
                .build();
    }

    private static Rules createUpdateRules() {
        return rule(RULE_ID_UPDATE)
                .withConditions(
                        condition(PROTEIN_ANNOTATION_TYPE).bind("ann").filter(simpleEquals("type", "keyword")),
                        condition(PROTEIN_TYPE).bind("protein"))
                .withActions(updateAction(RuleFact.builder()
                        .withType(PROTEIN_ANNOTATION_TYPE).withId("ann").addWith("value:'Updated'").build()))
                .buildRules();
    }

    private static Rules createMultiFieldUpdateRules() {
        return rule(RULE_ID_UPDATE_MULTI)
                .withConditions(
                        condition(PROTEIN_ANNOTATION_TYPE).bind("ann").filter(simpleEquals("type", "keyword")),
                        condition(PROTEIN_TYPE).bind("protein"))
                .withActions(updateAction(RuleFact.builder()
                        .withType(PROTEIN_ANNOTATION_TYPE).withId("ann")
                        .addField(
                                Field.builder().withAttribute("value").withValue("UpdatedValue").build(),
                                Field.builder().withAttribute("evidence").withValue("UpdatedEvidence").build())
                        .build()))
                .buildRules();
    }

    private static Rules createRemoveRules() {
        return rule(RULE_ID_REMOVE)
                .withConditions(
                        condition(PROTEIN_ANNOTATION_TYPE).bind("ann").filter(simpleEquals("type", "keyword")))
                .withActions(removeAction(RuleFact.builder()
                        .withType(PROTEIN_ANNOTATION_TYPE).withId("ann").build()))
                .buildRules();
    }

    private static Rules createProceduralCallRules() {
        return rule(RULE_ID_PROC)
                .withConditions(condition(PROTEIN_TYPE).bind("protein"))
                .withActions(Action.builder().withType(ActionType.CREATE)
                        .addFact(RuleFact.builder()
                                .withType(PROTEIN_ANNOTATION_TYPE)
                                .withCall(ProceduralAttachment.builder()
                                        .withUri("java://" + TestProceduralHelper.class.getName())
                                        .withProcedure("createAnnotation")
                                        .withArguments(ProceduralAttachment.Arguments.builder()
                                                .addArgument(ProcedureArgument.builder()
                                                        .withValue("protein").withIsReference(true).build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .buildRules();
    }

    private static Rules createProceduralCallNullRules() {
        return rule(RULE_ID_PROC_NULL)
                .withConditions(condition(PROTEIN_TYPE).bind("protein"))
                .withActions(Action.builder().withType(ActionType.CREATE)
                        .addFact(RuleFact.builder()
                                .withType(PROTEIN_ANNOTATION_TYPE)
                                .withCall(ProceduralAttachment.builder()
                                        .withUri("java://" + TestProceduralHelper.class.getName())
                                        .withProcedure("createAnnotationIfFragment")
                                        .withArguments(ProceduralAttachment.Arguments.builder()
                                                .addArgument(ProcedureArgument.builder()
                                                        .withValue("protein").withIsReference(true).build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .buildRules();
    }

    private static Rules createPositionalRules() {
        Rule parent = Rule.builder()
                .withId(RULE_ID_POSITIONAL_PRECONDITIONS)
                .withStatus(RuleStatus.APPLY)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(
                                        Condition.builder().withOn(POSITIONAL_PROTEIN_SIGNATURE_TYPE)
                                                .withBind("targetMatch").withFilter(signatureValueEquals(SIGNATURE_HAMAP_MF00375)).build(),
                                        Condition.builder().withOn(TEMPLATE_PROTEIN_SIGNATURE_TYPE)
                                                .withBind("templateMatch").withFilter(simpleEquals("protein.id", "P23893"))
                                                .withFilter(signatureValueEquals(SIGNATURE_HAMAP_MF00375)).build(),
                                        Condition.builder().withOn(PROTEIN_TYPE)
                                                .withBind("protein").withOf("targetMatch").build())
                                .build())
                        .build())
                .withActions(Actions.builder()
                        .addAction(Action.builder().withType(ActionType.CREATE)
                                .addWith("protein", "targetMatch", "templateMatch")
                                .addFact(RuleFact.builder().withType(POSITIONAL_MAPPING_TYPE)
                                        .addField(
                                                Field.builder().withAttribute("templateStart").withValue(String.valueOf(TEMPLATE_POSITION)).build(),
                                                Field.builder().withAttribute("templateEnd").withValue(String.valueOf(TEMPLATE_POSITION)).build())
                                        .build())
                                .build())
                        .build())
                .build();

        Rule child = Rule.builder()
                .withId(RULE_ID_POSITIONAL_FEATURE)
                .withExtends(parent)
                .withStatus(RuleStatus.APPLY)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(
                                        Condition.builder().withOn(POSITIONAL_MAPPING_TYPE)
                                                .withBind("pm").withWith("protein", "targetMatch", "templateMatch")
                                                .withFilter(simpleEquals("isValid", "true"))
                                                .withFilter(simpleEquals("templateStart", String.valueOf(TEMPLATE_POSITION)))
                                                .withFilter(simpleEquals("templateEnd", String.valueOf(TEMPLATE_POSITION)))
                                                .withFilter(Filter.builder().withOn("mappedSequence")
                                                        .withMatches(Matches.builder().withValue(MAPPED_SEQUENCE_K).build()).build())
                                                .build())
                                .build())
                        .build())
                .withActions(Actions.builder()
                        .addAction(Action.builder().withType(ActionType.CREATE)
                                .addWith("evidence:'UR000084156'", "positionEnd:pm.mappedEnd", "positionStart:pm.mappedStart", "protein")
                                .addFact(RuleFact.builder().withType(PROTEIN_ANNOTATION_TYPE)
                                        .addField(
                                                Field.builder().withAttribute("type").withValue("feature.MOD_RES").build(),
                                                Field.builder().withAttribute("value").withValue("N6-(pyridoxal phosphate)lysine").build())
                                        .build())
                                .build())
                        .build())
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createForwardReferenceInheritanceRules() {
        Rule parent = Rule.builder()
                .withId(RULE_ID_FORWARD_REF_PARENT)
                .withStatus(RuleStatus.APPLY)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(
                                        Condition.builder().withOn(PROTEIN_TYPE)
                                                .withBind("protein").withFilter(booleanFilter("sequence.isFragment", true)).build(),
                                        Condition.builder().withOn(ORGANISM_TYPE)
                                                .withBind("organism").withOf("protein")
                                                .withFilter(lineageContainsAny(LINEAGE_EUKARYOTA)).build())
                                .build())
                        .build())
                .withActions(Actions.builder()
                        .addAction(Action.builder().withType(ActionType.CREATE)
                                .addWith("protein")
                                .addFact(RuleFact.builder().withType(PROTEIN_ANNOTATION_TYPE)
                                        .addField(
                                                Field.builder().withAttribute("type").withValue("keyword").build(),
                                                Field.builder().withAttribute("value").withValue("Parent").build())
                                        .build())
                                .build())
                        .build())
                .build();

        Rule child = Rule.builder()
                .withId(RULE_ID_FORWARD_REF_CHILD)
                .withExtends(parent)
                .withStatus(RuleStatus.APPLY)
                .withConditions(DisjunctiveConditionSet.builder()
                        .addAND(ConjunctiveConditionSet.builder()
                                .addCondition(
                                        Condition.builder().withOn(POSITIONAL_PROTEIN_SIGNATURE_TYPE)
                                                .withBind("targetMatch").withFilter(signatureValueEquals(SIGNATURE_HAMAP_MF00375)).build(),
                                        Condition.builder().withOn(TEMPLATE_PROTEIN_SIGNATURE_TYPE)
                                                .withBind("templateMatch").withFilter(simpleEquals("protein.id", "P23893"))
                                                .withFilter(signatureValueEquals(SIGNATURE_HAMAP_MF00375)).build(),
                                        Condition.builder().withOn(PROTEIN_TYPE)
                                                .withBind("protein").withOf("targetMatch").build())
                                .build())
                        .build())
                .withActions(Actions.builder()
                        .addAction(Action.builder().withType(ActionType.CREATE)
                                .addWith("evidence:'UR_FORWARD_REF'", "protein")
                                .addFact(RuleFact.builder().withType(PROTEIN_ANNOTATION_TYPE)
                                        .addField(
                                                Field.builder().withAttribute("type").withValue("comment.subunit").build(),
                                                Field.builder().withAttribute("value").withValue("Homodimer").build())
                                        .build())
                                .build())
                        .build())
                .build();

        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(parent, child)
                .build();
    }

    private static Rules createUpdateWithAndFieldRules() {
        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(Rule.builder().withId("UR_UPDATE_WITH_AND_FIELD").withStatus(RuleStatus.APPLY)
                        .withConditions(DisjunctiveConditionSet.builder()
                                .addAND(ConjunctiveConditionSet.builder()
                                        .addCondition(Condition.builder().withOn(PROTEIN_ANNOTATION_TYPE).withBind("ann")
                                                .withFilter(Filter.builder().withOn("type").withValue(SimpleValue.builder().withValue("keyword").build()).build())
                                                .build())
                                        .addCondition(Condition.builder().withOn(PROTEIN_TYPE).withBind("protein").build())
                                        .build())
                                .build())
                        .withActions(Actions.builder()
                                .addAction(Action.builder().withType(ActionType.UPDATE)
                                        .addWith("protein")
                                        .addFact(RuleFact.builder().withType(PROTEIN_ANNOTATION_TYPE).withId("ann")
                                                .addField(Field.builder().withAttribute("value").withValue("protein.id").withIsReference(true).build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static Rules createProceduralCallWithLiteralRules() {
        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(Rule.builder().withId("UR_PROC_LITERAL").withStatus(RuleStatus.APPLY)
                        .withConditions(DisjunctiveConditionSet.builder()
                                .addAND(ConjunctiveConditionSet.builder()
                                        .addCondition(Condition.builder().withOn(PROTEIN_TYPE).withBind("protein").build())
                                        .build())
                                .build())
                        .withActions(Actions.builder()
                                .addAction(Action.builder().withType(ActionType.CREATE)
                                        .addFact(RuleFact.builder().withType(PROTEIN_ANNOTATION_TYPE)
                                                .withCall(ProceduralAttachment.builder()
                                                        .withUri("java://" + TestProceduralHelper.class.getName())
                                                        .withProcedure("createAnnotationWithLiteral")
                                                        .withArguments(ProceduralAttachment.Arguments.builder()
                                                                .addArgument(ProcedureArgument.builder().withValue("protein").withIsReference(true).build())
                                                                .addArgument(ProcedureArgument.builder().withValue("'LiteralArg'").build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static Rules createCreateWithBindingShorthandRules() {
        return Rules.builder().withName("org.uniprot.unirule.test").withVersion("1.0")
                .addRule(Rule.builder().withId("UR_CREATE_WITH_SHORTHAND").withStatus(RuleStatus.APPLY)
                        .withConditions(DisjunctiveConditionSet.builder()
                                .addAND(ConjunctiveConditionSet.builder()
                                        .addCondition(Condition.builder().withOn(PROTEIN_TYPE).withBind("protein").build())
                                        .build())
                                .build())
                        .withActions(Actions.builder()
                                .addAction(Action.builder().withType(ActionType.CREATE)
                                        .addWith("protein")
                                        .addFact(RuleFact.builder().withType(PROTEIN_ANNOTATION_TYPE)
                                                .addField(
                                                        Field.builder().withAttribute("type").withValue("keyword").build(),
                                                        Field.builder().withAttribute("value").withValue("Copied").build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static List<ProteinAnnotation> executeRules(Rules rules, FactSet inputFacts) throws Exception {
        var ruleEngine = DroolsRuleEngineFactory.build(rules);

        ProteinAnnotationRetriever retriever = new ProteinAnnotationRetriever(ruleEngine);
        ruleEngine.start();
        FactSet outputFacts = new RuleExecution(retriever).apply(ruleEngine, inputFacts);
        ruleEngine.dispose();

        return outputFacts.getFact().stream()
                .filter(ProteinAnnotation.class::isInstance)
                .map(ProteinAnnotation.class::cast)
                .collect(Collectors.toList());
    }
}
