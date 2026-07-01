/*
 *  Copyright (c) 2026 European Molecular Biology Laboratory
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

package uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro6;

import org.junit.jupiter.api.Test;
import org.uniprot.urml.facts.*;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.EntryType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.LocationType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.LocationsType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.MatchType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.MatchesType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.NucleicResultType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.ProteinResultType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.ResultsType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.SequenceType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.SignatureLibraryReleaseType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.XrefType;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.XmlFormatException;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InterPro6XmlProteinConverter}.
 */
class InterPro6XmlProteinConverterTest {

    private static final String SEQUENCE = "ACDEFGHIKLMNPQRSTVWY";
    private static final String FASTA_HEADER =
            "P12345|Protein name OX=1,2,3 OS=Test organism GN=gene1 GL=orf1 OG=mitochondrion";
    private static final String FASTA_HEADER_FRAGMENT =
            "P12345|Protein name (Fragment) OX=1,2,3 OS=Test organism";
    private static final String FASTA_HEADER_NO_GENE =
            "P12345|Protein name OX=1,2,3";

    /**
     * UniFIRE streams large InterProScan result sets, so the converter exposes standard Iterator semantics.
     * Tests {@link InterPro6XmlProteinConverter#hasNext()} when no proteins are available.
     */
    @Test
    void test_emptyCollection_hasNextShouldReturnFalse() {
        // arrange
        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.emptyList());

        // act & assert
        assertFalse(converter.hasNext());
    }

    /**
     * UniFIRE drives downstream chunking/writing via the iterator. Calling next() past the end must fail fast.
     * Tests {@link InterPro6XmlProteinConverter#next()} when the iterator is exhausted.
     */
    @Test
    void test_exhaustedIterator_nextShouldThrowNoSuchElementException() {
        // arrange
        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.emptyList());

        // act & assert
        assertThrows(NoSuchElementException.class, converter::next);
    }

    /**
     * InterProScan 6 can run on proteins and nucleotide sequences, but UniFIRE only consumes protein-derived facts.
     * Tests the {@link InterPro6XmlProteinConverter#InterPro6XmlProteinConverter(ResultsType)} constructor filtering.
     */
    @Test
    void test_resultsTypeWithNucleotideSequence_proteinsShouldBeFilteredIn() {
        // arrange
        ResultsType resultsType = new ResultsType();
        resultsType.getProteinOrNucleotideSequence().add(createProteinResult("P1", FASTA_HEADER_NO_GENE, SEQUENCE));

        NucleicResultType nucleicResultType = new NucleicResultType();
        SequenceType nucleicSequence = new SequenceType();
        nucleicSequence.setValue("ATCG");
        nucleicSequence.setMd5("dummymd5");
        nucleicResultType.setSequence(nucleicSequence);
        resultsType.getProteinOrNucleotideSequence().add(nucleicResultType);

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(resultsType);

        // act & assert
        assertTrue(converter.hasNext());
        assertThat(converter.next().getFact(), hasItem(instanceOf(Protein.class)));
        assertFalse(converter.hasNext());
    }

    /**
     * Every protein needs at least one xref so the FASTA header parser can extract identifier, organism lineage,
     * and gene metadata. Without it, signatures cannot be attributed to a protein.
     * Tests {@link InterPro6XmlProteinConverter#convertProteinMatches(ProteinResultType)} xref validation.
     */
    @Test
    void test_missingXref_shouldThrowXmlFormatException() {
        // arrange
        ProteinResultType protein = createProteinResult("P1", null, SEQUENCE);
        protein.getXref().clear();

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act & assert
        XmlFormatException exception = assertThrows(XmlFormatException.class, converter::next);
        assertThat(exception.getMessage(), containsString("Missing xref tag"));
        assertThat(exception.getMessage(), containsString(SEQUENCE));
    }

    /**
     * The protein sequence is the central input to all downstream signature matches. A null sequence indicates
     * malformed XML and must abort conversion.
     * Tests {@link InterPro6XmlProteinConverter#buildProtein(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData, ProteinResultType)}.
     */
    @Test
    void test_nullSequence_shouldThrowIllegalStateException() {
        // arrange
        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER_NO_GENE, null);

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act & assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, converter::next);
        assertThat(exception.getMessage(), containsString("Protein sequence was NULL"));
    }

    /**
     * An empty sequence is as useless as a null one; it means the XML contained no sequence data for the protein.
     * Tests {@link InterPro6XmlProteinConverter#buildProtein(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData, ProteinResultType)}.
     */
    @Test
    void test_emptySequence_shouldThrowIllegalStateException() {
        // arrange
        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER_NO_GENE, "");

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act & assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, converter::next);
        assertThat(exception.getMessage(), containsString("Protein sequence was NULL"));
    }

    /**
     * UniProt distinguishes fragment from complete sequences. The fragment flag from the FASTA header must flow
     * into the URML Protein fact for fragment-aware downstream rules.
     * Tests {@link InterPro6XmlProteinConverter#buildProtein(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData, ProteinResultType)}.
     */
    @Test
    void test_fragmentHeader_proteinShouldHaveFragmentSequence() {
        // arrange
        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER_FRAGMENT, SEQUENCE);

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        Protein resultProtein = extractSingleFact(factSet, Protein.class);
        assertThat(resultProtein.getId(), equalTo("P12345"));
        assertThat(resultProtein.getSequence().getValue(), equalTo(SEQUENCE));
        assertThat(resultProtein.getSequence().getLength(), equalTo(SEQUENCE.length()));
        assertTrue(resultProtein.getSequence().getIsFragment());
    }

    /**
     * Gene names, ORF/OLN names, and organelle locations come from the custom FASTA header and are attached to
     * the protein fact for gene-centric annotations and organelle-specific UniFIRE rules.
     * Tests {@link InterPro6XmlProteinConverter#buildGeneInformation(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData)}.
     */
    @Test
    void test_headerWithGeneInformation_geneFactsShouldBeSet() {
        // arrange
        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER, SEQUENCE);

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        Protein resultProtein = extractSingleFact(factSet, Protein.class);
        GeneInformation gene = resultProtein.getGene();
        assertNotNull(gene);
        assertThat(gene.getNames(), contains("gene1"));
        assertThat(gene.getOrfOrOlnNames(), contains("orf1"));
        assertThat(gene.getOrganelleLocations(), contains(OrganelleType.MITOCHONDRION));
    }

    /**
     * When the FASTA header carries no gene metadata, the converter still builds an empty GeneInformation fact
     * (the builder is invoked unconditionally) rather than leaving it null.
     * Tests {@link InterPro6XmlProteinConverter#buildGeneInformation(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData)}.
     */
    @Test
    void test_headerWithoutGeneInformation_geneFactsShouldBeEmpty() {
        // arrange
        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER_NO_GENE, SEQUENCE);

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        Protein resultProtein = extractSingleFact(factSet, Protein.class);
        GeneInformation gene = resultProtein.getGene();
        assertNotNull(gene);
        assertTrue(gene.getNames().isEmpty());
        assertTrue(gene.getOrfOrOlnNames().isEmpty());
        assertTrue(gene.getOrganelleLocations().isEmpty());
    }

    /**
     * Organism lineage (NCBI tax IDs) and scientific name are required for taxonomic filtering and
     * organism-specific rule matching downstream.
     * Tests {@link InterPro6XmlProteinConverter#buildOrganism(FactSet.Builder, FastaHeaderData)} and
     * {@link InterPro6XmlProteinConverter#createOrGetOrganism(String, List)}.
     */
    @Test
    void test_headerWithOrganism_organismFactsShouldBeSet() {
        // arrange
        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER, SEQUENCE);

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        Organism organism = extractSingleFact(factSet, Organism.class);
        assertThat(organism.getId(), equalTo("organism_3"));
        assertThat(organism.getScientificName(), equalTo("Test organism"));
        assertThat(organism.getLineage().getIds(), contains(1, 2, 3));
    }

    /**
     * Many proteins in one InterProScan run belong to the same organism. Caching the Organism fact by the last
     * tax ID avoids duplicate facts and keeps the URML document smaller.
     * Tests {@link InterPro6XmlProteinConverter#createOrGetOrganism(String, List)}.
     */
    @Test
    void test_sameLastTaxId_organismShouldBeReused() {
        // arrange
        ProteinResultType proteinOne = createProteinResult("P1", FASTA_HEADER, SEQUENCE);
        String secondHeader = "P2|Protein name OX=4,5,3 OS=Different organism";
        ProteinResultType proteinTwo = createProteinResult("P2", secondHeader, SEQUENCE);

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Arrays.asList(proteinOne, proteinTwo));

        // act
        FactSet factSetOne = converter.next();
        Organism organismOne = extractSingleFact(factSetOne, Organism.class);

        FactSet factSetTwo = converter.next();
        Organism organismTwo = extractSingleFact(factSetTwo, Organism.class);

        // assert
        assertSame(organismOne, organismTwo);
        assertThat(organismTwo.getScientificName(), equalTo("Test organism"));
    }

    /**
     * A single IPRScan protein can list multiple xrefs. UniFIRE emits one FactSet
     * per xref so each identifier receives its own annotated protein facts.
     * Tests {@link InterPro6XmlProteinConverter#convertProteinMatches(ProteinResultType)} xref loop.
     */
    @Test
    void test_multipleXrefs_multipleFactSetsShouldBeCreated() {
        // arrange
        ProteinResultType protein = createProteinResult("ignored", "P1|Protein name OX=1,2,3 OS=Test organism", SEQUENCE);
        XrefType secondXref = new XrefType();
        secondXref.setId("P2");
        secondXref.setName("P2|Another name OX=4,5,6 OS=Another organism");
        protein.getXref().add(secondXref);

        InterPro6XmlProteinConverter converter = new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        List<FactSet> factSets = new ArrayList<>();
        while (converter.hasNext()) {
            factSets.add(converter.next());
        }

        // assert
        assertThat(factSets, hasSize(2));
        assertThat(extractSingleFact(factSets.get(0), Protein.class).getId(), equalTo("P1"));
        assertThat(extractSingleFact(factSets.get(1), Protein.class).getId(), equalTo("P2"));
    }

    /**
     * InterProScan 6 uses library names such as "CATH-Gene3D" or "PANTHER", but the UniFIRE rule engine relies on
     * a fixed SignatureType enum. Correct mapping is essential for rules to fire.
     * Tests {@link InterPro6XmlProteinConverter#getSignatureType(SignatureLibraryReleaseType)}.
     */
    @Test
    void test_supportedLibraries_signatureTypeShouldBeMapped() {
        // arrange
        Map<String, SignatureType> expectedMappings = new LinkedHashMap<>();
        expectedMappings.put("CDD", SignatureType.CDD);
        expectedMappings.put("Pfam", SignatureType.PFAM);
        expectedMappings.put("SFLD", SignatureType.SFLD);
        expectedMappings.put("CATH-Gene3D", SignatureType.GENE_3_D);
        expectedMappings.put("HAMAP", SignatureType.HAMAP);
        expectedMappings.put("PANTHER", SignatureType.PANTHER);
        expectedMappings.put("PIRSF", SignatureType.PIR_SUPERFAMILY);
        expectedMappings.put("PRINTS", SignatureType.PRINTS);
        expectedMappings.put("SMART", SignatureType.SMART);
        expectedMappings.put("NCBIFAM", SignatureType.NCBIFAM);
        expectedMappings.put("SUPERFAMILY", SignatureType.SCOP_SUPERFAMILY);
        expectedMappings.put("PROSITE patterns", SignatureType.PROSITE);
        expectedMappings.put("PROSITE profiles", SignatureType.PROSITE);
        expectedMappings.put("CATH-FunFam", SignatureType.FUNFAM);

        for (Map.Entry<String, SignatureType> entry : expectedMappings.entrySet()) {
            ProteinResultType protein = createProteinResult(
                    "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch(entry.getKey(), "AC123", null));

            InterPro6XmlProteinConverter converter =
                    new InterPro6XmlProteinConverter(Collections.singletonList(protein));

            // act
            FactSet factSet = converter.next();

            // assert
            List<ProteinSignature> signatures = extractFacts(factSet, ProteinSignature.class);
            assertThat("Library " + entry.getKey() + " should produce one signature",
                    signatures, hasSize(1));
            assertThat(signatures.get(0).getSignature().getType(), equalTo(entry.getValue()));
        }
    }

    /**
     * Libraries such as COILS, TMHMM or MobiDB-lite produce predictions that UniFIRE does not model as signatures.
     * They must be skipped rather than creating invalid/unknown SignatureType values.
     * Tests {@link InterPro6XmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, MatchType)}.
     */
    @Test
    void test_unsupportedLibrary_signatureShouldBeSkipped() {
        // arrange
        ProteinResultType protein = createProteinResult(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch("COILS", "AC123", null));

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        List<ProteinSignature> signatures = extractFacts(factSet, ProteinSignature.class);
        assertThat(signatures, empty());
    }

    /**
     * PANTHER families and subfamilies use different accessions. When a subfamily model-ac is present, UniFIRE
     * prefers it for finer-grained annotation.
     * Tests {@link InterPro6XmlProteinConverter#getSignatureValue(MatchType, SignatureType)} PANTHER branch.
     */
    @Test
    void test_pantherWithModelAc_modelAcShouldBeUsedAsValue() {
        // arrange
        ProteinResultType protein = createProteinResult(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createPantherMatch("PTHR123", "PTHR123:SF1"));

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        ProteinSignature signature = extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getSignature().getValue(), equalTo("PTHR123:SF1"));
        assertThat(signature.getSignature().getType(), equalTo(SignatureType.PANTHER));
    }

    /**
     * If the PANTHER match only carries the family model-ac (or a blank subfamily model-ac), the converter must
     * fall back to the signature accession.
     * Tests {@link InterPro6XmlProteinConverter#getSignatureValue(MatchType, SignatureType)} PANTHER branch.
     */
    @Test
    void test_pantherWithBlankModelAc_signatureAcShouldBeUsedAsValue() {
        // arrange
        ProteinResultType protein = createProteinResult(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createPantherMatch("PTHR123", "  "));

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        ProteinSignature signature = extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getSignature().getValue(), equalTo("PTHR123"));
    }

    /**
     * CATH-Gene3D signatures are prefixed with "G3DSA:" in IPS6 XML, but UniFIRE's canonical signatures omit it.
     * Stripping ensures consistency with existing rule databases.
     * Tests {@link InterPro6XmlProteinConverter#getSignatureValue(MatchType, SignatureType)} GENE_3_D branch.
     */
    @Test
    void test_gene3dWithG3dsaPrefix_prefixShouldBeStripped() {
        // arrange
        ProteinResultType protein = createProteinResult(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch("CATH-Gene3D", "G3DSA:1.10.10.10", null));

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        ProteinSignature signature = extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getSignature().getValue(), equalTo("1.10.10.10"));
        assertThat(signature.getSignature().getType(), equalTo(SignatureType.GENE_3_D));
    }

    /**
     * CATH-FunFam signatures are also prefixed with "G3DSA:" in IPS6 XML and must be stripped for the same reason
     * as CATH-Gene3D.
     * Tests {@link InterPro6XmlProteinConverter#getSignatureValue(MatchType, SignatureType)} FUNFAM branch.
     */
    @Test
    void test_funfamWithG3dsaPrefix_prefixShouldBeStripped() {
        // arrange
        ProteinResultType protein = createProteinResult(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch("CATH-FunFam", "G3DSA:1.10.10.10:FF:000001", null));

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        ProteinSignature signature = extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getSignature().getValue(), equalTo("1.10.10.10:FF:000001"));
        assertThat(signature.getSignature().getType(), equalTo(SignatureType.FUNFAM));
    }

    /**
     * When a member database signature is integrated into InterPro, UniFIRE emits both the original library signature
     * and an INTER_PRO signature, allowing downstream rules to match either the source or the integrated entry.
     * Tests {@link InterPro6XmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, MatchType)}.
     */
    @Test
    void test_signatureWithInterProEntry_interProSignatureShouldBeAdded() {
        // arrange
        EntryType entry = new EntryType();
        entry.setAc("IPR000001");
        entry.setName("Test entry");
        entry.setDesc("Test description");
        entry.setType("Family");

        ProteinResultType protein = createProteinResult(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch("Pfam", "PF00001", entry));

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        List<ProteinSignature> signatures = extractFacts(factSet, ProteinSignature.class);
        assertThat(signatures, hasSize(2));

        List<String> values = signatures.stream()
                .map(s -> s.getSignature().getValue())
                .collect(Collectors.toList());
        assertThat(values, containsInAnyOrder("PF00001", "IPR000001"));

        Map<String, SignatureType> valueToType = signatures.stream()
                .collect(Collectors.toMap(s -> s.getSignature().getValue(), s -> s.getSignature().getType()));
        assertThat(valueToType.get("PF00001"), equalTo(SignatureType.PFAM));
        assertThat(valueToType.get("IPR000001"), equalTo(SignatureType.INTER_PRO));
    }

    /**
     * A signature can match multiple non-contiguous regions. The frequency field records the location count.
     * Tests {@link InterPro6XmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, MatchType)}.
     */
    @Test
    void test_multipleLocations_frequencyShouldEqualLocationCount() {
        // arrange
        LocationType locationOne = createLocation(1, 10, null);
        LocationType locationTwo = createLocation(11, 20, null);
        MatchType match = createMatchWithLocations("Pfam", "PF00001", null, locationOne, locationTwo);

        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER_NO_GENE, SEQUENCE, match);

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        List<ProteinSignature> signatures = extractFacts(factSet, ProteinSignature.class);
        assertThat(signatures, hasSize(2));
        for (ProteinSignature signature : signatures) {
            assertThat(signature.getFrequency(), equalTo(2));
            PositionalProteinSignature positional = (PositionalProteinSignature) signature;
            assertTrue(
                    (positional.getPositionStart() == 1 && positional.getPositionEnd() == 10)
                            || (positional.getPositionStart() == 11 && positional.getPositionEnd() == 20));
        }
    }

    /**
     * Some matches include a sequence alignment string (e.g., from HAMAP). Preserving it in the positional signature
     * allows alignment-aware rules and output formatting.
     * Tests {@link InterPro6XmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, MatchType)}.
     */
    @Test
    void test_locationWithAlignment_alignmentShouldBePreserved() {
        // arrange
        LocationType location = createLocation(1, 10, "ACDEFGHIKLMN");
        MatchType match = createMatchWithLocations("Pfam", "PF00001", null, location);

        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER_NO_GENE, SEQUENCE, match);

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        PositionalProteinSignature signature = (PositionalProteinSignature) extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getAlignment().getValue(), equalTo("ACDEFGHIKLMN"));
    }

    /**
     * Not all location types provide start/end coordinates. The converter must gracefully omit positions rather than
     * fail or emit invalid coordinates.
     * Tests {@link InterPro6XmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, MatchType)}.
     */
    @Test
    void test_locationWithoutStartEnd_positionsShouldBeOmitted() {
        // arrange
        LocationType location = new LocationType();
        location.setRepresentative(true);
        MatchType match = createMatchWithLocations("Pfam", "PF00001", null, location);

        ProteinResultType protein = createProteinResult("P1", FASTA_HEADER_NO_GENE, SEQUENCE, match);

        InterPro6XmlProteinConverter converter =
                new InterPro6XmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        PositionalProteinSignature signature = (PositionalProteinSignature) extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getPositionStart(), equalTo(0));
        assertNull(signature.getPositionEnd());
    }

    private ProteinResultType createProteinResult(String identifier, String fastaHeader, String sequence) {
        return createProteinResult(identifier, fastaHeader, sequence, new MatchType[0]);
    }

    private ProteinResultType createProteinResult(
            String identifier, String fastaHeader, String sequence, MatchType... matches) {
        ProteinResultType protein = new ProteinResultType();

        SequenceType sequenceType = new SequenceType();
        sequenceType.setValue(sequence);
        sequenceType.setMd5("dummymd5");
        protein.setSequence(sequenceType);

        if (fastaHeader != null) {
            XrefType xref = new XrefType();
            xref.setId(identifier);
            xref.setName(fastaHeader);
            protein.getXref().add(xref);
        }

        MatchesType matchesType = new MatchesType();
        if (matches != null) {
            for (MatchType match : matches) {
                matchesType.getMatch().add(match);
            }
        }
        protein.setMatches(matchesType);

        return protein;
    }

    private MatchType createMatch(String library, String signatureAc, EntryType entry) {
        LocationType location = createLocation(1, 10, null);
        return createMatchWithLocations(library, signatureAc, entry, location);
    }

    private MatchType createMatchWithLocations(String library, String signatureAc, EntryType entry,
                                               LocationType... locations) {
        MatchType match = new MatchType();
        match.setSource(library);
        match.setModelAc(signatureAc);

        uk.ac.ebi.uniprot.aa.interproscan6.model.generated.SignatureType signature =
                new uk.ac.ebi.uniprot.aa.interproscan6.model.generated.SignatureType();
        signature.setAc(signatureAc);
        signature.setName("Test signature");

        SignatureLibraryReleaseType release = new SignatureLibraryReleaseType();
        release.setLibrary(library);
        release.setVersion("1.0");
        signature.setSignatureLibraryRelease(release);
        signature.setEntry(entry);

        match.setSignature(signature);

        LocationsType locationsType = new LocationsType();
        for (LocationType location : locations) {
            locationsType.getLocation().add(location);
        }
        match.setLocations(locationsType);

        return match;
    }

    private MatchType createPantherMatch(String signatureAc, String modelAc) {
        MatchType match = createMatch("PANTHER", signatureAc, null);
        match.setModelAc(modelAc);
        return match;
    }

    private LocationType createLocation(int start, int end, String alignment) {
        LocationType location = new LocationType();
        location.setStart(BigInteger.valueOf(start));
        location.setEnd(BigInteger.valueOf(end));
        location.setRepresentative(true);
        location.setAlignment(alignment);
        return location;
    }

    private <T extends Fact> T extractSingleFact(FactSet factSet, Class<T> type) {
        List<T> facts = extractFacts(factSet, type);
        assertThat("Expected exactly one fact of type " + type.getSimpleName(), facts, hasSize(1));
        return facts.get(0);
    }

    @SuppressWarnings("unchecked")
    private <T extends Fact> List<T> extractFacts(FactSet factSet, Class<T> type) {
        return factSet.getFact().stream()
                .filter(type::isInstance)
                .map(f -> (T) f)
                .collect(Collectors.toList());
    }
}
