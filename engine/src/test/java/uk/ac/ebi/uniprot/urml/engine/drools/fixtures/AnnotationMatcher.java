package uk.ac.ebi.uniprot.urml.engine.drools.fixtures;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.uniprot.urml.facts.ProteinAnnotation;

import java.util.Objects;

public final class AnnotationMatcher extends TypeSafeMatcher<ProteinAnnotation> {
    private final String proteinId;
    private final String type;
    private final String value;
    private final String evidence;
    private final Integer positionStart;
    private final Integer positionEnd;
    private final Boolean hasPlaceholder;

    private AnnotationMatcher(String proteinId, String type, String value, String evidence,
                              Integer positionStart, Integer positionEnd, Boolean hasPlaceholder) {
        this.proteinId = proteinId;
        this.type = type;
        this.value = value;
        this.evidence = evidence;
        this.positionStart = positionStart;
        this.positionEnd = positionEnd;
        this.hasPlaceholder = hasPlaceholder;
    }

    public static AnnotationMatcher annotationLike(String proteinId, String type, String value) {
        return new AnnotationMatcher(proteinId, type, value, null, null, null, null);
    }

    public static AnnotationMatcher annotationLike(String proteinId, String type, String value, String evidence) {
        return new AnnotationMatcher(proteinId, type, value, evidence, null, null, null);
    }

    public static AnnotationMatcher annotationLike(String proteinId, String type, String value, String evidence,
                                                   int positionStart, int positionEnd) {
        return new AnnotationMatcher(proteinId, type, value, evidence, positionStart, positionEnd, null);
    }

    public static AnnotationMatcher annotationLike(String proteinId, String type, String value, String evidence,
                                                   Integer positionStart, Integer positionEnd, Boolean hasPlaceholder) {
        return new AnnotationMatcher(proteinId, type, value, evidence, positionStart, positionEnd, hasPlaceholder);
    }

    @Override
    protected boolean matchesSafely(ProteinAnnotation item) {
        return proteinId.equals(item.getProtein().getId())
                && type.equals(item.getType())
                && value.equals(item.getValue())
                && (evidence == null || evidence.equals(item.getEvidence()))
                && (positionStart == null || Objects.equals(positionStart, item.getPositionStart()))
                && (positionEnd == null || Objects.equals(positionEnd, item.getPositionEnd()))
                && (hasPlaceholder == null || Objects.equals(hasPlaceholder, item.getHasPlaceholder()));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("ProteinAnnotation{")
                .appendValue(proteinId)
                .appendText(", ")
                .appendValue(type)
                .appendText(", ")
                .appendValue(value);
        if (evidence != null) {
            description.appendText(", evidence=").appendValue(evidence);
        }
        if (positionStart != null) {
            description.appendText(", positionStart=").appendValue(positionStart);
        }
        if (positionEnd != null) {
            description.appendText(", positionEnd=").appendValue(positionEnd);
        }
        if (hasPlaceholder != null) {
            description.appendText(", hasPlaceholder=").appendValue(hasPlaceholder);
        }
        description.appendText("}");
    }
}
