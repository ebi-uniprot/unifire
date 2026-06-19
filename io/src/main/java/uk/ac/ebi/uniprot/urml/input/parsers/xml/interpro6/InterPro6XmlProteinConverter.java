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

package uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro6;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.urml.facts.*;
import org.uniprot.urml.facts.SignatureType;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.*;
import uk.ac.ebi.uniprot.urml.input.parsers.fasta.header.FastaHeaderData;
import uk.ac.ebi.uniprot.urml.input.parsers.fasta.header.FastaHeaderParser;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Iterates over {@link ProteinResultType} and convert them to {@link FactSet}.
 *
 * @author Alexandre Renaux
 */
public class InterPro6XmlProteinConverter implements Iterator<FactSet> {

    private static final Logger logger = LoggerFactory.getLogger(InterPro6XmlProteinConverter.class);

    private final Iterator<ProteinResultType> sourceIterator;
    private final Map<String, Organism> organismMap;
    private final FastaHeaderParser uniProtFastaHeaderParser;
    private final Queue<FactSet> factSetQueue;


    public InterPro6XmlProteinConverter(ResultsType resultsType) {
        this(resultsType.getProteinOrNucleotideSequence().stream()
                .filter(a -> a instanceof ProteinResultType)
                .map(a -> (ProteinResultType) a)
                .collect(Collectors.toList()));
    }

    public InterPro6XmlProteinConverter(Collection<ProteinResultType> proteins) {
        this.sourceIterator = proteins.iterator();
        this.organismMap = new HashMap<>();
        this.uniProtFastaHeaderParser = new FastaHeaderParser();
        this.factSetQueue = new LinkedList<>();
    }

    @Override
    public boolean hasNext() {
        return !factSetQueue.isEmpty() || sourceIterator.hasNext();
    }

    @Override
    public FactSet next() {
        if (factSetQueue.isEmpty() && !sourceIterator.hasNext()) {
            throw new NoSuchElementException();
        } else {
            if (factSetQueue.isEmpty()) {
                convertProteinMatches(sourceIterator.next());
            }
            return factSetQueue.poll();
        }
    }

    private void convertProteinMatches(ProteinResultType ipsProtein) {
        if (!ipsProtein.getXref().isEmpty()) {
            for (XrefType proteinXref : ipsProtein.getXref()) {
                FastaHeaderData fastaHeaderData = uniProtFastaHeaderParser.parse(proteinXref.getName());
                FactSet.Builder<Void> factSetBuilder = FactSet.builder();

                org.uniprot.urml.facts.Protein.Builder<Void> proteinBuilder = org.uniprot.urml.facts.Protein.builder();
                buildProtein(proteinBuilder, fastaHeaderData, ipsProtein);
                buildGeneInformation(proteinBuilder, fastaHeaderData);
                Organism organism = buildOrganism(factSetBuilder, fastaHeaderData);

                proteinBuilder.withOrganism(organism);
                org.uniprot.urml.facts.Protein protein = proteinBuilder.build();
                factSetBuilder.addFact(protein);

                for (MatchType match : ipsProtein.getMatches().getMatch()) {
                    buildProteinSignature(factSetBuilder, protein, match);
                }

                factSetQueue.add(factSetBuilder.build());
            }
        } else {
            throw new InterProScan6XmlFormatException(
                    String.format("Missing xref tag for ipsProtein sequence=%s", ipsProtein.getSequence()));
        }

    }

    private void buildProteinSignature(FactSet.Builder<Void> factSetBuilder, org.uniprot.urml.facts.Protein protein, MatchType match) {
        SignatureLibraryReleaseType libraryRelease = match.getSignature().getSignatureLibraryRelease();
        SignatureType signatureType = getSignatureType(libraryRelease);

        if (signatureType == null) {
            logger.warn("Ignored signature type {}", libraryRelease.getLibrary());
            return;
        }

        String accession = getSignatureValue(match, signatureType);

        Signature libSignature = Signature.builder()
                .withType(signatureType)
                .withValue(accession)
                .build();

        Signature ipSignature = null;
        if (match.getSignature().getEntry() != null) {
            String ipsAccession = match.getSignature().getEntry().getAc();
            ipSignature = Signature.builder()
                    .withType(SignatureType.INTER_PRO)
                    .withValue(ipsAccession)
                    .build();
        }

        var locations = match.getLocations().getLocation();
        for (LocationType o : locations) {
            PositionalProteinSignature.Builder<Void> pSignatureBuilder = PositionalProteinSignature.builder()
                    .withProtein(protein)
                    .withFrequency(locations.size());

            if (o.getAlignment() != null) {
                String alignment = o.getAlignment();
                pSignatureBuilder
                        .withAlignment()
                        .withValue(alignment);
            } else {
                pSignatureBuilder
                        .withPositionStart(o.getStart().intValue())
                        .withPositionEnd(o.getEnd().intValue());
            }

            var signature = pSignatureBuilder.withSignature(libSignature).build();
            factSetBuilder.addFact(signature);

            if (ipSignature != null) {
                var signatureWithIpSignature = pSignatureBuilder.withSignature(ipSignature).build();
                factSetBuilder.addFact(signatureWithIpSignature);
            }
        }
    }

    private Organism buildOrganism(FactSet.Builder<Void> factSetBuilder, FastaHeaderData fastaHeaderData) {
        Organism organism = createOrGetOrganism(fastaHeaderData.getOrganismScientificName(),
                fastaHeaderData.getOrganismLineage());
        factSetBuilder.addFact(organism);
        return organism;
    }

    private void buildProtein(org.uniprot.urml.facts.Protein.Builder proteinBuilder, FastaHeaderData fastaHeaderData,
                              ProteinResultType ipsProtein) {
        var sequence = Optional.ofNullable(ipsProtein.getSequence()).map(SequenceType::getValue).orElse(null);
        var sequenceLength = Optional.ofNullable(sequence).map(String::length).orElse(0);
        proteinBuilder.withId(fastaHeaderData.getIdentifier());
        proteinBuilder.withSequence()
                .withValue(sequence)
                .withLength(sequenceLength)
                .withIsFragment(fastaHeaderData.isFragment())
                .end();
    }

    private void buildGeneInformation(org.uniprot.urml.facts.Protein.Builder proteinBuilder,
                                      FastaHeaderData fastaHeaderData) {
        GeneInformation.Builder geneBuilder = proteinBuilder.withGene();

        if (fastaHeaderData.getRecommendedGeneName() != null) {
            geneBuilder.withNames(fastaHeaderData.getRecommendedGeneName());
        }
        if (fastaHeaderData.getRecommendedOlnOrOrf() != null) {
            geneBuilder.withOrfOrOlnNames(fastaHeaderData.getRecommendedOlnOrOrf());
        }
        if (!CollectionUtils.isEmpty(fastaHeaderData.getGeneLocationOrganelles())) {
            geneBuilder.withOrganelleLocations(fastaHeaderData.getGeneLocationOrganelles());
        }
    }

    private Organism createOrGetOrganism(String scientificName, List<Integer> taxIdLineage) {
        String key = "organism_" + taxIdLineage.get(taxIdLineage.size() - 1).toString();
        if (organismMap.containsKey(key)) {
            return organismMap.get(key);
        } else {
            Organism.Builder<Void> organismBuilder = Organism.builder();
            organismBuilder.withId(key).withLineage().withIds(taxIdLineage).end();
            if (scientificName != null) {
                organismBuilder.withScientificName(scientificName);
            }
            Organism organism = organismBuilder.build();
            organismMap.put(key, organism);
            return organism;
        }
    }

    private String getSignatureValue(MatchType matchType, SignatureType signatureType) {
        var signatureValue = matchType.getSignature().getAc();
        return switch (signatureType) {
            case GENE_3_D, FUNFAM -> signatureValue.replace("G3DSA:", "");
            case PANTHER -> matchType.getModelAc();
            default -> signatureValue;
        };
    }

    private SignatureType getSignatureType(SignatureLibraryReleaseType signatureLibrary) {
        var library = Optional.ofNullable(signatureLibrary)
                .map(SignatureLibraryReleaseType::getLibrary)
                .map(InterPro6SignatureLibrary::fromName)
                .orElse(null);

        if (library == null) return null;

        return switch (library) {
            case CDD -> SignatureType.CDD;
            case PFAM -> SignatureType.PFAM;
            case SFLD -> SignatureType.SFLD;
            case GENE3D -> SignatureType.GENE_3_D;
            case HAMAP -> SignatureType.HAMAP;
            case PANTHER -> SignatureType.PANTHER;
            case PIRSF -> SignatureType.PIR_SUPERFAMILY;
            case PRINTS -> SignatureType.PRINTS;
            case PRODOM -> SignatureType.PRO_DOM;
            case SMART -> SignatureType.SMART;
            case TIGRFAM, NCBIFAM -> SignatureType.NCBIFAM;
            case SUPERFAMILY -> SignatureType.SCOP_SUPERFAMILY;
            case PROSITE_PATTERNS, PROSITE_PROFILES -> SignatureType.PROSITE;
            case FUNFAM -> SignatureType.FUNFAM;
            default -> null;
        };
    }
}