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

package uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro;

import org.junit.jupiter.api.Test;
import org.uniprot.urml.facts.*;
import uk.ac.ebi.interpro.scan.model.Entry;
import uk.ac.ebi.interpro.scan.model.EntryType;
import uk.ac.ebi.interpro.scan.model.HmmBounds;
import uk.ac.ebi.interpro.scan.model.Hmmer3Match;
import uk.ac.ebi.interpro.scan.model.Location;
import uk.ac.ebi.interpro.scan.model.Match;
import uk.ac.ebi.interpro.scan.model.Model;
import uk.ac.ebi.interpro.scan.model.PantherMatch;
import uk.ac.ebi.interpro.scan.model.ProfileScanMatch;
import uk.ac.ebi.interpro.scan.model.Protein;
import uk.ac.ebi.interpro.scan.model.ProteinMatchesHolder;
import uk.ac.ebi.interpro.scan.model.ProteinXref;
import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.SignatureLibrary;
import uk.ac.ebi.interpro.scan.model.SignatureLibraryRelease;
import uk.ac.ebi.interpro.scan.model.DCStatus;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.XmlFormatException;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InterProXmlProteinConverter}.
 */
class InterProXmlProteinConverterTest {

    private static final String SEQUENCE = "ACDEFGHIKLMNPQRSTVWY";
    private static final String FASTA_HEADER =
            "P12345|Protein name OX=1,2,3 OS=Test organism GN=gene1 GL=orf1 OG=mitochondrion";
    private static final String FASTA_HEADER_FRAGMENT =
            "P12345|Protein name (Fragment) OX=1,2,3 OS=Test organism";
    private static final String FASTA_HEADER_NO_GENE =
            "P12345|Protein name OX=1,2,3";

    /**
     * UniFIRE streams large InterProScan result sets, so the converter exposes standard Iterator semantics.
     * Tests {@link InterProXmlProteinConverter#hasNext()} when no proteins are available.
     */
    @Test
    void test_emptyCollection_hasNextShouldReturnFalse() {
        // arrange
        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.emptyList());

        // act & assert
        assertFalse(converter.hasNext());
    }

    /**
     * UniFIRE drives downstream chunking/writing via the iterator. Calling next() past the end must fail fast.
     * Tests {@link InterProXmlProteinConverter#next()} when the iterator is exhausted.
     */
    @Test
    void test_exhaustedIterator_nextShouldThrowNoSuchElementException() {
        // arrange
        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.emptyList());

        // act & assert
        assertThrows(NoSuchElementException.class, converter::next);
    }

    /**
     * InterProScan 5 protein matches are held in a ProteinMatchesHolder. UniFIRE consumes protein-derived facts.
     * Tests the {@link InterProXmlProteinConverter#InterProXmlProteinConverter(ProteinMatchesHolder)} constructor.
     */
    @Test
    void test_proteinMatchesHolder_proteinsShouldBeConverted() {
        // arrange
        ProteinMatchesHolder proteinMatchesHolder = new ProteinMatchesHolder("5.66-98.0");
        proteinMatchesHolder.addProtein(createProtein("P1", FASTA_HEADER_NO_GENE, SEQUENCE));

        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(proteinMatchesHolder);

        // act & assert
        assertTrue(converter.hasNext());
        assertThat(converter.next().getFact(), hasItem(instanceOf(org.uniprot.urml.facts.Protein.class)));
        assertFalse(converter.hasNext());
    }

    /**
     * Every protein needs at least one xref so the FASTA header parser can extract identifier, organism lineage,
     * and gene metadata. Without it, signatures cannot be attributed to a protein.
     * Tests {@link InterProXmlProteinConverter#convertProteinMatches(Protein)} xref validation.
     */
    @Test
    void test_missingXref_shouldThrowXmlFormatException() {
        // arrange
        Protein protein = createProtein("P1", null, SEQUENCE);
        protein.getCrossReferences().clear();

        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act & assert
        XmlFormatException exception = assertThrows(XmlFormatException.class, converter::next);
        assertThat(exception.getMessage(), containsString("Missing xref tag"));
    }

    /**
     * The protein sequence is the central input to all downstream signature matches. A null sequence indicates
     * malformed XML and must abort conversion. The legacy InterProScan 5 model rejects it during construction.
     * Tests {@link Protein#Protein(String)}.
     */
    @Test
    void test_nullSequence_shouldThrowIllegalArgumentException() {
        // act & assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createProtein("P1", FASTA_HEADER_NO_GENE, null));
        assertThat(exception.getMessage(), containsString("is null"));
    }

    /**
     * An empty sequence is as useless as a null one; it means the XML contained no sequence data for the protein.
     * The legacy InterProScan 5 model rejects it during construction.
     * Tests {@link Protein#Protein(String)}.
     */
    @Test
    void test_emptySequence_shouldThrowIllegalArgumentException() {
        // act & assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createProtein("P1", FASTA_HEADER_NO_GENE, ""));
        assertThat(exception.getMessage(), containsString("amino acid sequence"));
    }

    /**
     * UniProt distinguishes fragment from complete sequences. The fragment flag from the FASTA header must flow
     * into the URML Protein fact for fragment-aware downstream rules.
     * Tests {@link InterProXmlProteinConverter#buildProtein(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData, Protein)}.
     */
    @Test
    void test_fragmentHeader_proteinShouldHaveFragmentSequence() {
        // arrange
        Protein protein = createProtein("P1", FASTA_HEADER_FRAGMENT, SEQUENCE);

        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        org.uniprot.urml.facts.Protein resultProtein = extractSingleFact(factSet, org.uniprot.urml.facts.Protein.class);
        assertThat(resultProtein.getId(), equalTo("P12345"));
        assertThat(resultProtein.getSequence().getValue(), equalTo(SEQUENCE));
        assertThat(resultProtein.getSequence().getLength(), equalTo(SEQUENCE.length()));
        assertTrue(resultProtein.getSequence().getIsFragment());
    }

    /**
     * Gene names, ORF/OLN names, and organelle locations come from the custom FASTA header and are attached to
     * the protein fact for gene-centric annotations and organelle-specific UniFIRE rules.
     * Tests {@link InterProXmlProteinConverter#buildGeneInformation(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData)}.
     */
    @Test
    void test_headerWithGeneInformation_geneFactsShouldBeSet() {
        // arrange
        Protein protein = createProtein("P1", FASTA_HEADER, SEQUENCE);

        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        org.uniprot.urml.facts.Protein resultProtein = extractSingleFact(factSet, org.uniprot.urml.facts.Protein.class);
        GeneInformation gene = resultProtein.getGene();
        assertNotNull(gene);
        assertThat(gene.getNames(), contains("gene1"));
        assertThat(gene.getOrfOrOlnNames(), contains("orf1"));
        assertThat(gene.getOrganelleLocations(), contains(OrganelleType.MITOCHONDRION));
    }

    /**
     * When the FASTA header carries no gene metadata, the converter still builds an empty GeneInformation fact
     * (the builder is invoked unconditionally) rather than leaving it null.
     * Tests {@link InterProXmlProteinConverter#buildGeneInformation(org.uniprot.urml.facts.Protein.Builder, FastaHeaderData)}.
     */
    @Test
    void test_headerWithoutGeneInformation_geneFactsShouldBeEmpty() {
        // arrange
        Protein protein = createProtein("P1", FASTA_HEADER_NO_GENE, SEQUENCE);

        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        org.uniprot.urml.facts.Protein resultProtein = extractSingleFact(factSet, org.uniprot.urml.facts.Protein.class);
        GeneInformation gene = resultProtein.getGene();
        assertNotNull(gene);
        assertTrue(gene.getNames().isEmpty());
        assertTrue(gene.getOrfOrOlnNames().isEmpty());
        assertTrue(gene.getOrganelleLocations().isEmpty());
    }

    /**
     * Organism lineage (NCBI tax IDs) and scientific name are required for taxonomic filtering and
     * organism-specific rule matching downstream.
     * Tests {@link InterProXmlProteinConverter#buildOrganism(FactSet.Builder, FastaHeaderData)} and
     * {@link InterProXmlProteinConverter#createOrGetOrganism(String, List)}.
     */
    @Test
    void test_headerWithOrganism_organismFactsShouldBeSet() {
        // arrange
        Protein protein = createProtein("P1", FASTA_HEADER, SEQUENCE);

        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.singletonList(protein));

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
     * Tests {@link InterProXmlProteinConverter#createOrGetOrganism(String, List)}.
     */
    @Test
    void test_sameLastTaxId_organismShouldBeReused() {
        // arrange
        Protein proteinOne = createProtein("P1", FASTA_HEADER, SEQUENCE);
        String secondHeader = "P2|Protein name OX=4,5,3 OS=Different organism";
        Protein proteinTwo = createProtein("P2", secondHeader, SEQUENCE);

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Arrays.asList(proteinOne, proteinTwo));

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
     * Tests {@link InterProXmlProteinConverter#convertProteinMatches(Protein)} xref loop.
     */
    @Test
    void test_multipleXrefs_multipleFactSetsShouldBeCreated() {
        // arrange
        Protein protein = createProtein("ignored", "P1|Protein name OX=1,2,3 OS=Test organism", SEQUENCE);
        ProteinXref secondXref = new ProteinXref("P2");
        secondXref.setName("P2|Another name OX=4,5,6 OS=Another organism");
        protein.addCrossReference(secondXref);

        InterProXmlProteinConverter converter = new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        List<FactSet> factSets = new ArrayList<>();
        while (converter.hasNext()) {
            factSets.add(converter.next());
        }

        // assert
        assertThat(factSets, hasSize(2));
        assertThat(extractSingleFact(factSets.get(0), org.uniprot.urml.facts.Protein.class).getId(), equalTo("P1"));
        assertThat(extractSingleFact(factSets.get(1), org.uniprot.urml.facts.Protein.class).getId(), equalTo("P2"));
    }

    /**
     * InterProScan 5 uses library enum values, but the UniFIRE rule engine relies on a fixed SignatureType enum.
     * Correct mapping is essential for rules to fire.
     * Tests {@link InterProXmlProteinConverter#getSignatureType(SignatureLibrary)}.
     */
    @Test
    void test_supportedLibraries_signatureTypeShouldBeMapped() {
        // arrange
        Map<SignatureLibrary, SignatureType> expectedMappings = new LinkedHashMap<>();
        expectedMappings.put(SignatureLibrary.CDD, SignatureType.CDD);
        expectedMappings.put(SignatureLibrary.PFAM, SignatureType.PFAM);
        expectedMappings.put(SignatureLibrary.SFLD, SignatureType.SFLD);
        expectedMappings.put(SignatureLibrary.GENE3D, SignatureType.GENE_3_D);
        expectedMappings.put(SignatureLibrary.HAMAP, SignatureType.HAMAP);
        expectedMappings.put(SignatureLibrary.PANTHER, SignatureType.PANTHER);
        expectedMappings.put(SignatureLibrary.PIRSF, SignatureType.PIR_SUPERFAMILY);
        expectedMappings.put(SignatureLibrary.PRINTS, SignatureType.PRINTS);
        expectedMappings.put(SignatureLibrary.PRODOM, SignatureType.PRO_DOM);
        expectedMappings.put(SignatureLibrary.SMART, SignatureType.SMART);
        expectedMappings.put(SignatureLibrary.NCBIFAM, SignatureType.NCBIFAM);
        expectedMappings.put(SignatureLibrary.TIGRFAM, SignatureType.NCBIFAM);
        expectedMappings.put(SignatureLibrary.SUPERFAMILY, SignatureType.SCOP_SUPERFAMILY);
        expectedMappings.put(SignatureLibrary.PROSITE_PATTERNS, SignatureType.PROSITE);
        expectedMappings.put(SignatureLibrary.PROSITE_PROFILES, SignatureType.PROSITE);
        expectedMappings.put(SignatureLibrary.FUNFAM, SignatureType.FUNFAM);

        for (Map.Entry<SignatureLibrary, SignatureType> entry : expectedMappings.entrySet()) {
            Protein protein = createProtein(
                    "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch(entry.getKey(), "AC123", null));

            InterProXmlProteinConverter converter =
                    new InterProXmlProteinConverter(Collections.singletonList(protein));

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
     * Tests {@link InterProXmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, Match)}.
     */
    @Test
    void test_unsupportedLibrary_signatureShouldBeSkipped() {
        // arrange
        Protein protein = createProtein(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch(SignatureLibrary.COILS, "AC123", null));

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        List<ProteinSignature> signatures = extractFacts(factSet, ProteinSignature.class);
        assertThat(signatures, empty());
    }

    /**
     * PANTHER families and subfamilies use different accessions. When a subfamily model-ac is present, UniFIRE
     * prefers it for finer-grained annotation.
     * Tests {@link InterProXmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, Match)} PANTHER handling.
     */
    @Test
    void test_pantherWithModelAc_modelAcShouldBeUsedAsValue() {
        // arrange
        Protein protein = createProtein(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createPantherMatch("PTHR123", "PTHR123:SF1"));

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

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
     * Tests {@link InterProXmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, Match)} PANTHER handling.
     */
    @Test
    void test_pantherWithBlankModelAc_signatureAcShouldBeUsedAsValue() {
        // arrange
        Protein protein = createProtein(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createPantherMatch("PTHR123", "  "));

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        ProteinSignature signature = extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getSignature().getValue(), equalTo("PTHR123"));
    }

    /**
     * CATH-Gene3D signatures are prefixed with "G3DSA:" in IPS5 XML, but UniFIRE's canonical signatures omit it.
     * Stripping ensures consistency with existing rule databases.
     * Tests {@link InterProXmlProteinConverter#getSignatureValue(String, SignatureType)} GENE_3_D branch.
     */
    @Test
    void test_gene3dWithG3dsaPrefix_prefixShouldBeStripped() {
        // arrange
        Protein protein = createProtein(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch(SignatureLibrary.GENE3D, "G3DSA:1.10.10.10", null));

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        ProteinSignature signature = extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getSignature().getValue(), equalTo("1.10.10.10"));
        assertThat(signature.getSignature().getType(), equalTo(SignatureType.GENE_3_D));
    }

    /**
     * CATH-FunFam signatures are also prefixed with "G3DSA:" in IPS5 XML and must be stripped for the same reason
     * as CATH-Gene3D.
     * Tests {@link InterProXmlProteinConverter#getSignatureValue(String, SignatureType)} FUNFAM branch.
     */
    @Test
    void test_funfamWithG3dsaPrefix_prefixShouldBeStripped() {
        // arrange
        Protein protein = createProtein(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch(SignatureLibrary.FUNFAM, "G3DSA:1.10.10.10:FF:000001", null));

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

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
     * Tests {@link InterProXmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, Match)}.
     */
    @Test
    void test_signatureWithInterProEntry_interProSignatureShouldBeAdded() {
        // arrange
        Entry entry = new Entry("IPR000001", "Test entry", EntryType.FAMILY);

        Protein protein = createProtein(
                "P1", FASTA_HEADER_NO_GENE, SEQUENCE, createMatch(SignatureLibrary.PFAM, "PF00001", entry));

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

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
     * Tests {@link InterProXmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, Match)}.
     */
    @Test
    void test_multipleLocations_frequencyShouldEqualLocationCount() {
        // arrange
        Location locationOne = new Hmmer3Match.Hmmer3Location(1, 10, 0.0d, 0.0d, 1, 1, 1, HmmBounds.COMPLETE, 1, 1, false, DCStatus.CONTINUOUS);
        Location locationTwo = new Hmmer3Match.Hmmer3Location(11, 20, 0.0d, 0.0d, 1, 1, 1, HmmBounds.COMPLETE, 1, 1, false, DCStatus.CONTINUOUS);
        Match match = createMatchWithLocations(SignatureLibrary.PFAM, "PF00001", null, locationOne, locationTwo);

        Protein protein = createProtein("P1", FASTA_HEADER_NO_GENE, SEQUENCE, match);

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

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
     * Some matches include a sequence alignment string (e.g., from HAMAP/ProfileScan). Preserving it in the positional
     * signature allows alignment-aware rules and output formatting.
     * Tests {@link InterProXmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, Match)}.
     */
    @Test
    void test_locationWithAlignment_alignmentShouldBePreserved() {
        // arrange
        ProfileScanMatch.ProfileScanLocation location = new ProfileScanMatch.ProfileScanLocation(1, 12, 0.0d, "12M");
        Match match = createMatchWithLocations(SignatureLibrary.PROSITE_PROFILES, "PS00001", null, location);

        Protein protein = createProtein("P1", FASTA_HEADER_NO_GENE, SEQUENCE, match);

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        PositionalProteinSignature signature = (PositionalProteinSignature) extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getAlignment().getValue(), equalTo("ACDEFGHIKLMN"));
    }

    /**
     * Not all location types provide start/end coordinates. The converter must gracefully omit positions rather than
     * fail or emit invalid coordinates.
     * Tests {@link InterProXmlProteinConverter#buildProteinSignature(FactSet.Builder, org.uniprot.urml.facts.Protein, Match)}.
     */
    @Test
    void test_locationWithoutStartEnd_positionsShouldBeOmitted() {
        // arrange
        Hmmer3Match.Hmmer3Location location = new Hmmer3Match.Hmmer3Location(0, 0, 0.0d, 0.0d, 1, 1, 1, HmmBounds.COMPLETE, 1, 1, false, DCStatus.CONTINUOUS);
        Match match = createMatchWithLocations(SignatureLibrary.PFAM, "PF00001", null, location);

        Protein protein = createProtein("P1", FASTA_HEADER_NO_GENE, SEQUENCE, match);

        InterProXmlProteinConverter converter =
                new InterProXmlProteinConverter(Collections.singletonList(protein));

        // act
        FactSet factSet = converter.next();

        // assert
        PositionalProteinSignature signature = (PositionalProteinSignature) extractSingleFact(factSet, ProteinSignature.class);
        assertThat(signature.getPositionStart(), equalTo(0));
        assertThat(signature.getPositionEnd(), equalTo(0));
    }

    private Protein createProtein(String identifier, String fastaHeader, String sequence) {
        return createProtein(identifier, fastaHeader, sequence, new Match[0]);
    }

    @SafeVarargs
    private final Protein createProtein(String identifier, String fastaHeader, String sequence, Match<? extends Location>... matches) {
        Protein protein = new Protein(sequence);

        if (fastaHeader != null) {
            ProteinXref xref = new ProteinXref(identifier);
            xref.setName(fastaHeader);
            protein.addCrossReference(xref);
        }

        if (matches != null) {
            for (Match<? extends Location> match : matches) {
                protein.addMatch(match);
            }
        }

        return protein;
    }

    private Match<? extends Location> createMatch(SignatureLibrary library, String signatureAc, Entry entry) {
        Location location = new Hmmer3Match.Hmmer3Location(1, 10, 0.0d, 0.0d, 1, 1, 1, HmmBounds.COMPLETE, 1, 1, false, DCStatus.CONTINUOUS);
        return createMatchWithLocations(library, signatureAc, entry, location);
    }

    @SafeVarargs
    private final Match<? extends Location> createMatchWithLocations(SignatureLibrary library, String signatureAc, Entry entry,
                                                                     Location... locations) {
        SignatureLibraryRelease release = new SignatureLibraryRelease(library, "1.0");
        Signature signature = new Signature(signatureAc, "Test signature", null, null, null, release, Collections.emptySet());
        signature.setEntry(entry);

        if (locations.length == 0) {
            Set<Hmmer3Match.Hmmer3Location> locationSet = Collections.emptySet();
            return new Hmmer3Match(signature, signatureAc, 0.0d, 0.0d, locationSet);
        }

        if (locations[0] instanceof ProfileScanMatch.ProfileScanLocation) {
            Set<ProfileScanMatch.ProfileScanLocation> locationSet = new LinkedHashSet<>();
            for (Location location : locations) {
                locationSet.add((ProfileScanMatch.ProfileScanLocation) location);
            }
            return new ProfileScanMatch(signature, signatureAc, locationSet);
        }

        Set<Hmmer3Match.Hmmer3Location> locationSet = new LinkedHashSet<>();
        for (Location location : locations) {
            locationSet.add((Hmmer3Match.Hmmer3Location) location);
        }
        return new Hmmer3Match(signature, signatureAc, 0.0d, 0.0d, locationSet);
    }

    private Match<? extends Location> createPantherMatch(String signatureAc, String modelAc) {
        SignatureLibraryRelease release = new SignatureLibraryRelease(SignatureLibrary.PANTHER, "1.0");
        Model model = new Model(signatureAc, signatureAc + " model", null);
        Signature signature = new Signature(signatureAc, "Test signature", null, null, null, release, Collections.singleton(model));
        PantherMatch.PantherLocation location = new PantherMatch.PantherLocation(1, 10, 1, 10, 1, HmmBounds.COMPLETE, 1, 1);
        Set<PantherMatch.PantherLocation> locations = Collections.singleton(location);
        PantherMatch match = new PantherMatch(signature, signatureAc, locations, 0.0d, 0.0d, null);
        match.setSignatureModels(modelAc);
        return match;
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
