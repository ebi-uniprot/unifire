package uk.ac.ebi.uniprot.urml.engine.drools.engine;

import org.junit.jupiter.api.Test;
import org.uniprot.urml.facts.Protein;
import org.uniprot.urml.facts.ProteinAnnotation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Static helpers used by procedural attachment integration tests.
 *
 * <p><strong>WARNING:</strong> These methods are invoked reflectively by Drools at runtime
 * via {@link org.uniprot.urml.rules.ProceduralAttachment}. IDEs and static analyzers will
 * therefore report them as unused. Do not remove them.
 *
 * <p>Each helper is referenced from a specific URML rule in
 * {@link DroolsRuleEngineIntegrationTest}. The procedural-call integration tests and
 * {@link #proceduralHelperMethods_shouldBeAvailableForReflection()} below act as guards
 * against accidental deletion.</p>
 */
public class TestProceduralHelper {

    /**
     * Creates an annotation from a protein binding with no extra arguments.
     *
     * <p>Used reflectively by rule "UR_PROC" in
     * {@link DroolsRuleEngineIntegrationTest.SmokeTests#proceduralCallAction_shouldCreateFactFromHelper()}.</p>
     */
    @SuppressWarnings("unused")
    public static ProteinAnnotation createAnnotation(Protein protein) {
        return ProteinAnnotation.builder()
                .withProtein(protein)
                .withEvidence("EV_FROM_CALL")
                .withType("keyword")
                .withValue("FromCall")
                .build();
    }

    /**
     * Creates an annotation whose value is taken from the literal argument.
     *
     * <p>Used reflectively by rule "UR_PROC_LITERAL" in
     * {@link DroolsRuleEngineIntegrationTest.SmokeTests#proceduralCallWithLiteralArgument_shouldInvokeHelper()}.</p>
     */
    @SuppressWarnings("unused")
    public static ProteinAnnotation createAnnotationWithLiteral(Protein protein, String literal) {
        return ProteinAnnotation.builder()
                .withProtein(protein)
                .withEvidence("EV_FROM_CALL")
                .withType("keyword")
                .withValue(literal)
                .build();
    }

    /**
     * Conditionally creates an annotation from a protein binding.
     *
     * <p>Returns {@code null} when the protein is not a fragment. Used reflectively by rule
     * "UR_PROC_NULL" in
     * {@link DroolsRuleEngineIntegrationTest.SmokeTests#proceduralCallAction_shouldCreateNoFactWhenHelperReturnsNull()}.
     * </p>
     */
    @SuppressWarnings("unused")
    public static ProteinAnnotation createAnnotationIfFragment(Protein protein) {
        if (protein == null || protein.getSequence() == null || !protein.getSequence().getIsFragment()) {
            return null;
        }
        return ProteinAnnotation.builder()
                .withProtein(protein)
                .withEvidence("EV_FROM_CALL")
                .withType("keyword")
                .withValue("FromConditionalCall")
                .build();
    }

    /**
     * Verifies that every helper method registered in a Drools procedural attachment is
     * still present. This test fails early if a method is renamed or removed, which is
     * otherwise hard to catch because Drools only discovers the missing method at rule
     * execution time via reflection.
     */
    @Test
    void proceduralHelperMethods_shouldBeAvailableForReflection() {
        assertDoesNotThrow(() -> TestProceduralHelper.class.getMethod("createAnnotation", Protein.class));
        assertDoesNotThrow(() -> TestProceduralHelper.class.getMethod("createAnnotationWithLiteral", Protein.class, String.class));
        assertDoesNotThrow(() -> TestProceduralHelper.class.getMethod("createAnnotationIfFragment", Protein.class));
    }
}
