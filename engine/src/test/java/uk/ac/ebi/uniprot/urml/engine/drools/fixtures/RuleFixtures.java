package uk.ac.ebi.uniprot.urml.engine.drools.fixtures;

import lombok.experimental.UtilityClass;
import org.uniprot.urml.rules.Action;
import org.uniprot.urml.rules.ActionType;
import org.uniprot.urml.rules.Field;
import org.uniprot.urml.rules.RuleFact;

import static uk.ac.ebi.uniprot.urml.engine.drools.fixtures.FactFixtures.PROTEIN_ANNOTATION_TYPE;

@UtilityClass
public final class RuleFixtures {

    public static RuleFact annotation(String fieldType, String value) {
        return annotation(fieldType, value, false);
    }

    public static RuleFact annotation(String fieldType, String value, boolean valueIsReference) {
        return RuleFact.builder()
                .withType(PROTEIN_ANNOTATION_TYPE)
                .addField(
                        Field.builder().withAttribute("type").withValue(fieldType).build(),
                        Field.builder().withAttribute("value").withValue(value).withIsReference(valueIsReference).build())
                .build();
    }

    public static Action createAction(String withClause, RuleFact... facts) {
        return Action.builder()
                .withType(ActionType.CREATE)
                .addWith(withClause, "protein")
                .addFact(facts)
                .build();
    }

    public static Action updateAction(RuleFact fact) {
        return Action.builder()
                .withType(ActionType.UPDATE)
                .addFact(fact)
                .build();
    }

    public static Action removeAction(RuleFact fact) {
        return Action.builder()
                .withType(ActionType.REMOVE)
                .addFact(fact)
                .build();
    }
}
