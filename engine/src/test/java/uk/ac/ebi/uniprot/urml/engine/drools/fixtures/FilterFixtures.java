package uk.ac.ebi.uniprot.urml.engine.drools.fixtures;

import lombok.experimental.UtilityClass;
import org.uniprot.urml.facts.SignatureType;
import org.uniprot.urml.rules.*;

import java.util.Arrays;

@UtilityClass
public final class FilterFixtures {

    public static Filter lineageContainsAny(Integer... ids) {
        return Filter.builder()
                .withOn("lineage.ids")
                .withContains(MultiValue.builder()
                        .withOperator(LogicalOperator.ANY)
                        .addValue(Arrays.stream(ids)
                                .map(id -> SimpleValue.builder().withValue(String.valueOf(id)).build())
                                .toArray(SimpleValue[]::new))
                        .build())
                .build();
    }

    public static Filter signatureEquals(SignatureType type, String value) {
        return Filter.builder()
                .withOn("signature")
                .addField(
                        Field.builder().withAttribute("type").withValue(type.value()).build(),
                        Field.builder().withAttribute("value").withValue(value).build())
                .build();
    }

    public static Filter signatureValueEquals(String value) {
        return Filter.builder()
                .withOn("signature.value")
                .withValue(SimpleValue.builder().withValue(value).build())
                .build();
    }

    public static Filter simpleEquals(String attribute, String value) {
        return Filter.builder()
                .withOn(attribute)
                .withValue(SimpleValue.builder().withValue(value).build())
                .build();
    }

    public static Filter simpleEqualsNegative(String attribute, String value) {
        return Filter.builder()
                .withOn(attribute)
                .withNegative(true)
                .withValue(SimpleValue.builder().withValue(value).build())
                .build();
    }

    public static Filter rangeFilter(String attribute, String start, String end, boolean negative) {
        return Filter.builder()
                .withOn(attribute)
                .withNegative(negative)
                .withRange(Range.builder()
                        .withStart(Integer.parseInt(start))
                        .withEnd(Integer.parseInt(end))
                        .build())
                .build();
    }

    public static Filter inFilter(String attribute, boolean any, boolean negative, String... values) {
        return Filter.builder()
                .withOn(attribute)
                .withNegative(negative)
                .withIn(MultiValue.builder()
                        .withOperator(any ? LogicalOperator.ANY : LogicalOperator.ALL)
                        .addValue(Arrays.stream(values)
                                .map(v -> SimpleValue.builder().withValue(v).build())
                                .toArray(SimpleValue[]::new))
                        .build())
                .build();
    }

    public static Filter containsFilter(String attribute, boolean any, boolean negative, String... values) {
        return Filter.builder()
                .withOn(attribute)
                .withNegative(negative)
                .withContains(MultiValue.builder()
                        .withOperator(any ? LogicalOperator.ANY : LogicalOperator.ALL)
                        .addValue(Arrays.stream(values)
                                .map(v -> SimpleValue.builder().withValue(v).build())
                                .toArray(SimpleValue[]::new))
                        .build())
                .build();
    }

    public static Filter startsWithFilter(String attribute, String value) {
        return Filter.builder()
                .withOn(attribute)
                .withStartsWith(StartsWith.builder().withValue(value).build())
                .build();
    }

    public static Filter matchesFilter(String attribute, String regex, boolean negative) {
        return Filter.builder()
                .withOn(attribute)
                .withNegative(negative)
                .withMatches(Matches.builder().withValue(regex).build())
                .build();
    }

    public static Filter booleanFilter(String attribute, boolean negative) {
        return Filter.builder()
                .withOn(attribute)
                .withNegative(negative)
                .build();
    }

    public static Filter refFilter(String attribute, String ref, boolean negative) {
        return Filter.builder()
                .withOn(attribute)
                .withNegative(negative)
                .withRef(ref)
                .build();
    }

    public static Filter nestedFieldReferenceFilter(String filterOn, String fieldAttribute, String ref) {
        return Filter.builder()
                .withOn(filterOn)
                .addField(Field.builder()
                        .withAttribute(fieldAttribute)
                        .withValue(ref)
                        .withIsReference(true)
                        .build())
                .build();
    }
}
