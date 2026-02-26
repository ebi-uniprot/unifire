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

package uk.ac.ebi.uniprot.urml.procedures.unirule.placeholders;

import uk.ac.ebi.kraken.datamining.unirule.annotationprocess.replacement.exceptions.ReplacementResolverException;
import uk.ac.ebi.kraken.datamining.unirule.annotationprocess.replacement.resolvers.ResidueNameAtPositionResolver;
import uk.ac.ebi.kraken.datamining.unirule.query.Placeholders;
import uk.ac.ebi.kraken.model.unirule.Placeholder;
import uk.ac.ebi.uniprot.urml.procedures.unirule.PositionalMappingData;
import uk.ac.ebi.uniprot.urml.procedures.unirule.positionalfeatures.InvalidPositionalMappingDataException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.urml.facts.PositionalMapping;
import org.uniprot.urml.facts.ProteinAnnotation;

/**
 * Resolves {@link ProteinAnnotation#value} using {@link PositionalMapping} data to
 * replace {@link Placeholder#RESIDUE_NAME_AT_POS}.
 *
 * @author Alexandre Renaux
 */
public class ResidueNameAtPosPlaceholderResolver extends ResidueNameAtPositionResolver implements PlaceholderResolver {

    private static final Logger logger = LoggerFactory.getLogger(ResidueNameAtPosPlaceholderResolver.class);

    private static final ResidueNameAtPosPlaceholderResolver residueNameAtPosPlaceholderResolver = new ResidueNameAtPosPlaceholderResolver();

    public static void resolve(ProteinAnnotation proteinAnnotation, List<PositionalMapping> positionalMappings){
        residueNameAtPosPlaceholderResolver.resolveResidueNameAtPos(proteinAnnotation, positionalMappings);
    }

    private void resolveResidueNameAtPos(ProteinAnnotation proteinAnnotation, List<PositionalMapping> positionalMappings){
        List<String> placeholders = Placeholders.split(proteinAnnotation.getValue());
        Set<String> templateSignature = new HashSet<>();
        for (String placeholder : placeholders) {
            if (Placeholder.RESIDUE_NAME_AT_POS.containsPlaceholder(placeholder)) {
                String[] arguments = Placeholder.RESIDUE_NAME_AT_POS.extractArguments(placeholder);
                if (arguments.length >= 4) {
                    String templateProteinId = arguments[2];
                    String signatureName = arguments[3];
                    templateSignature.add(templateProteinId + signatureName);
                }
            }
        }
        PositionalMapping validPositionalMapping = null;
        for (PositionalMapping positionalMapping : positionalMappings) {
            if (positionalMapping.getTemplateMatch() == null) continue;
            String templateProteinId = positionalMapping.getTemplateMatch().getProtein().getId();
            String signatureName = positionalMapping.getTargetMatch().getSignature().getValue();
            if (templateSignature.contains(templateProteinId+signatureName)){
                validPositionalMapping = positionalMapping;
                break;
            }
        }
        if (validPositionalMapping != null){
            try {
                PositionalMappingData positionalMappingData = new PositionalMappingData(validPositionalMapping);
                resolve(proteinAnnotation, getResolvedAnnotationValue(proteinAnnotation.getValue(), positionalMappingData));
            } catch (ReplacementResolverException | InvalidPositionalMappingDataException e) {
                logger.debug(e.getMessage());
            }
        }
    }
}
