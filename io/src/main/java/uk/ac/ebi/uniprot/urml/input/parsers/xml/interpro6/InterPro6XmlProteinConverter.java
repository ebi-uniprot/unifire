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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.urml.facts.*;
import org.uniprot.urml.facts.Signature;
import uk.ac.ebi.uniprot.aa.interproscan6.model.*;
import uk.ac.ebi.uniprot.aa.interproscan6.model.Protein;
import uk.ac.ebi.uniprot.urml.input.parsers.fasta.header.FastaHeaderData;
import uk.ac.ebi.uniprot.urml.input.parsers.fasta.header.FastaHeaderParser;

import java.util.*;

/**
 * Iterates over {@link Protein} and convert them to {@link FactSet}.
 *
 * @author Alexandre Renaux
 */
public class InterPro6XmlProteinConverter implements Iterator<FactSet>{

    private static final Logger logger = LoggerFactory.getLogger(InterPro6XmlProteinConverter.class);

    private final Iterator<Protein> sourceIterator;
    private final Map<String, Organism> organismMap;
    private final FastaHeaderParser uniProtFastaHeaderParser;
    private final Queue<FactSet> factSetQueue;


    public InterPro6XmlProteinConverter(ProteinMatchesHolder proteinMatches){
        this(proteinMatches.getProteins());
    }

    public InterPro6XmlProteinConverter(Collection<Protein> proteins) {
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
        if (factSetQueue.isEmpty() && !sourceIterator.hasNext()){
            throw new NoSuchElementException();
        } else {
            if (factSetQueue.isEmpty()) {
                convertProteinMatches(sourceIterator.next());
            }
            return factSetQueue.poll();
        }
    }

    private void convertProteinMatches(Protein ipsProtein){

        if (!ipsProtein.getCrossReferences().isEmpty()) {
            for (ProteinXref proteinXref : ipsProtein.getCrossReferences()) {
                FastaHeaderData fastaHeaderData = uniProtFastaHeaderParser.parse(proteinXref.getName());
                FactSet.Builder<Void> factSetBuilder = FactSet.builder();

                org.uniprot.urml.facts.Protein.Builder<Void> proteinBuilder = org.uniprot.urml.facts.Protein.builder();
                buildProtein(proteinBuilder, fastaHeaderData, ipsProtein);
                buildGeneInformation(proteinBuilder, fastaHeaderData);
                Organism organism = buildOrganism(factSetBuilder, fastaHeaderData);

                proteinBuilder.withOrganism(organism);
                org.uniprot.urml.facts.Protein protein = proteinBuilder.build();
                factSetBuilder.addFact(protein);

                for (Match match : ipsProtein.getMatches()) {
                    buildProteinSignature(factSetBuilder, protein, match);
                }

                factSetQueue.add(factSetBuilder.build());
            }
        } else {
            throw new InterProScan6XmlFormatException(
                    String.format("Missing xref tag for ipsProtein md5=%s", ipsProtein.getMd5()));
        }

    }

    private void buildProteinSignature(FactSet.Builder<Void> factSetBuilder, org.uniprot.urml.facts.Protein protein,
                                       Match match) {
        SignatureLibrary library = match.getSignature().getSignatureLibraryRelease().getLibrary();
        SignatureType signatureType = getSignatureType(library);
        if (signatureType == null){
            logger.warn("Ignored signature type {}", library.getName());
            return;
        }

        String accession = match.getSignature().getAccession();
        if (match instanceof PantherMatch) {
            //Panther subfamily accession is under model-ac element
            accession = StringUtils.isNotBlank(match.getSignatureModels()) ? match.getSignatureModels() : accession;
        }
        accession = getSignatureValue(accession, signatureType);
        Signature libSignature = Signature.builder().withType(signatureType).withValue(accession).build();

        Signature ipSignature = null;
        if (match.getSignature().getEntry() != null) {
            String ipsAccession = match.getSignature().getEntry().getAccession();
            ipSignature = Signature.builder().withType(SignatureType.INTER_PRO).withValue(ipsAccession).build();
        }

        for (Object o : match.getLocations()) {
            PositionalProteinSignature.Builder<Void> pSignatureBuilder = PositionalProteinSignature.builder();
            pSignatureBuilder.withProtein(protein);
            pSignatureBuilder.withFrequency(match.getLocations().size());
            if (o instanceof ProfileScanMatch.ProfileScanLocation profileScanLocation) {
                String alignment = profileScanLocation.getAlignment();
                pSignatureBuilder.withAlignment().withValue(alignment).end();
            }
            if (o instanceof Location location) {
                pSignatureBuilder.withPositionStart(location.getStart());
                pSignatureBuilder.withPositionEnd(location.getEnd());
            }
            factSetBuilder.addFact(pSignatureBuilder.withSignature(libSignature).build());
            if (ipSignature != null) {
                factSetBuilder.addFact(pSignatureBuilder.withSignature(ipSignature).build());
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
                              Protein ipsProtein) {
        proteinBuilder.withId(fastaHeaderData.getIdentifier());
        proteinBuilder.withSequence().withValue(ipsProtein.getSequence())
                .withLength(ipsProtein.getSequenceLength()).withIsFragment(fastaHeaderData.isFragment()).end();
    }

    private void buildGeneInformation(org.uniprot.urml.facts.Protein.Builder proteinBuilder,
                                      FastaHeaderData fastaHeaderData) {
        GeneInformation.Builder geneBuilder = proteinBuilder.withGene();

        if(fastaHeaderData.getRecommendedGeneName() != null){
            geneBuilder.withNames(fastaHeaderData.getRecommendedGeneName());
        }
        if (fastaHeaderData.getRecommendedOlnOrOrf() != null){
            geneBuilder.withOrfOrOlnNames(fastaHeaderData.getRecommendedOlnOrOrf());
        }
        if (!CollectionUtils.isEmpty(fastaHeaderData.getGeneLocationOrganelles())){
            geneBuilder.withOrganelleLocations(fastaHeaderData.getGeneLocationOrganelles());
        }
    }

    private Organism createOrGetOrganism(String scientificName, List<Integer> taxIdLineage){
        String key = "organism_"+taxIdLineage.get(taxIdLineage.size() - 1).toString();
        if (organismMap.containsKey(key)){
            return organismMap.get(key);
        } else {
            Organism.Builder<Void> organismBuilder = Organism.builder();
            organismBuilder.withId(key).withLineage().withIds(taxIdLineage).end();
            if (scientificName != null){
                organismBuilder.withScientificName(scientificName);
            }
            Organism organism = organismBuilder.build();
            organismMap.put(key, organism);
            return organism;
        }
    }

    private String getSignatureValue(String value, SignatureType signatureType){
        if (signatureType.equals(SignatureType.GENE_3_D) || signatureType.equals(SignatureType.FUNFAM)) {
            return value.replace("G3DSA:", "");
        }
        return value;
    }

    private SignatureType getSignatureType(SignatureLibrary signatureLibrary) {
        switch (signatureLibrary){
            case CDD:
                return SignatureType.CDD;
            case PFAM:
                return SignatureType.PFAM;
            case SFLD:
                return SignatureType.SFLD;
            case GENE3D:
                return SignatureType.GENE_3_D;
            case HAMAP:
                return SignatureType.HAMAP;
            case PANTHER:
                return SignatureType.PANTHER;
            case PIRSF:
                return SignatureType.PIR_SUPERFAMILY;
            case PRINTS:
                return SignatureType.PRINTS;
            case PRODOM:
                return SignatureType.PRO_DOM;
            case SMART:
                return SignatureType.SMART;
            case TIGRFAM, NCBIFAM:
                return SignatureType.NCBIFAM;
            case SUPERFAMILY:
                return SignatureType.SCOP_SUPERFAMILY;
            case PROSITE_PATTERNS, PROSITE_PROFILES:
                return SignatureType.PROSITE;
            case FUNFAM:
                return SignatureType.FUNFAM;
            default:
                return null;
        }
    }

}