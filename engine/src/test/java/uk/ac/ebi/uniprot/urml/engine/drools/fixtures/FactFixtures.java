package uk.ac.ebi.uniprot.urml.engine.drools.fixtures;

import lombok.experimental.UtilityClass;
import org.uniprot.urml.facts.*;

import javax.xml.namespace.QName;

@UtilityClass
public final class FactFixtures {

    private static final String FACT_NS = "http://uniprot.org/urml/facts";

    public static final QName PROTEIN_TYPE = new QName(FACT_NS, "Protein");
    public static final QName ORGANISM_TYPE = new QName(FACT_NS, "Organism");
    public static final QName PROTEIN_SIGNATURE_TYPE = new QName(FACT_NS, "ProteinSignature");
    public static final QName PROTEIN_ANNOTATION_TYPE = new QName(FACT_NS, "ProteinAnnotation");
    public static final QName POSITIONAL_PROTEIN_SIGNATURE_TYPE = new QName(FACT_NS, "PositionalProteinSignature");
    public static final QName TEMPLATE_PROTEIN_SIGNATURE_TYPE = new QName(FACT_NS, "TemplateProteinSignature");
    public static final QName POSITIONAL_MAPPING_TYPE = new QName(FACT_NS, "PositionalMapping");

    private static final String DEFAULT_SEQUENCE = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static Protein protein(String id, Organism organism) {
        return protein(id, organism, 26, false);
    }

    public static Protein protein(String id, Organism organism, int length, boolean fragment) {
        return Protein.builder()
                .withId(id)
                .withSequence(ProteinSequence.builder()
                        .withValue(DEFAULT_SEQUENCE.substring(0, Math.min(length, 26)))
                        .withLength(length)
                        .withIsFragment(fragment)
                        .build())
                .withOrganism(organism)
                .build();
    }

    public static Organism organism(String name, Integer... lineageIds) {
        return organismWithId("organism_" + name, name, lineageIds);
    }

    public static Organism organismWithId(String id, String name, Integer... lineageIds) {
        return Organism.builder()
                .withId(id)
                .withScientificName(name)
                .withLineage(Lineage.builder().withIds(lineageIds).build())
                .build();
    }

    public static Organism eukaryote() {
        return organism("Eukaryota", 2759);
    }

    public static ProteinSignature proteinSignature(Protein protein, SignatureType type, String value) {
        return ProteinSignature.builder()
                .withProtein(protein)
                .withSignature(Signature.builder().withType(type).withValue(value).build())
                .withFrequency(1)
                .build();
    }

    public static ProteinAnnotation proteinAnnotation(Protein protein, String type, String value) {
        return ProteinAnnotation.builder()
                .withProtein(protein)
                .withEvidence("EV")
                .withType(type)
                .withValue(value)
                .build();
    }

    public static PositionalProteinSignature positionalProteinSignature(
            Protein protein, SignatureType type, String value, int start, int end) {
        return PositionalProteinSignature.builder()
                .withProtein(protein)
                .withSignature(Signature.builder().withType(type).withValue(value).build())
                .withFrequency(1)
                .withPositionStart(start)
                .withPositionEnd(end)
                .build();
    }

    public static TemplateProtein templateProtein(String id) {
        return TemplateProtein.builder().withId(id).build();
    }

    public static TemplateProteinSignature templateProteinSignature(
            TemplateProtein protein, SignatureType type, String value, int start, int end) {
        return TemplateProteinSignature.builder()
                .withProtein(protein)
                .withSignature(Signature.builder().withType(type).withValue(value).build())
                .withFrequency(1)
                .withPositionStart(start)
                .withPositionEnd(end)
                .build();
    }

    public static PositionalMapping positionalMapping(
            Protein protein,
            PositionalProteinSignature targetMatch,
            TemplateProteinSignature templateMatch,
            String templateStart,
            String templateEnd,
            String mappedSequence,
            int mappedStart,
            int mappedEnd) {
        return PositionalMapping.builder()
                .withProtein(protein)
                .withTargetMatch(targetMatch)
                .withTemplateMatch(templateMatch)
                .withTemplateStart(templateStart)
                .withTemplateEnd(templateEnd)
                .withMappedSequence(mappedSequence)
                .withIsValid(true)
                .withMappedStart(mappedStart)
                .withMappedEnd(mappedEnd)
                .build();
    }
}
