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

import uk.ac.ebi.uniprot.urml.procedures.unirule.TestUtils;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.urml.facts.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ResidueNameAtPosPlaceholderResolver}
 *
 * @author Alexandre Renaux
 */
class ResidueNameAtPosPlaceholderResolverTest {

    @Test
    void resolveMultipleResidueNameAtPosWithOnePositionalMapping(){
        Protein protein = TestUtils.createProtein("Q57GP3", "MPFSMRAIKARGTRRANKRLASCPARRFSSASICKRCSSSSGLVTTSASCANQSSRNGVFRVTICAILWLSYQLTISEGLMATYYSNDFRSGLKIMLDGEPYAVESSEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAECIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTAGTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRVK");
        Protein templateProtein = TestUtils.createProtein("P0A6N4", "MATYYSNDFRAGLKIMLDGEPYAVEASEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAECIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTAGTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRVK");
        Signature signature = TestUtils.createSignature(SignatureType.HAMAP, "MF_00141");

        PositionalProteinSignature templateMatch = TestUtils
                .createPositionalProteinSignature(templateProtein, signature, 3, 187,
                        "TYYSNDFRAGLKIMLDGePYAVEASEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAE-CIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTA-GTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRV");
        PositionalProteinSignature targetMatch = TestUtils.createPositionalProteinSignature(protein, signature, 83, 267,
                "TYYSNDFRSGLKIMLDGePYAVESSEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAE-CIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTA-GTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRV");

        PositionalMapping positionalMapping = TestUtils.createUnmappedPositionalMapping(targetMatch, templateMatch, "34", "34");

        List<PositionalMapping> positionalMappingList = new ArrayList<>();
        positionalMappingList.add(positionalMapping);

        ProteinAnnotation proteinAnnotation = buildProteinAnnotation("Is beta-lysylated on the epsilon-amino group of @RESIDUE_NAME_AT_POS|Lys|34|P0A6N4|MF_00141|@ by the combined action of EpmA and EpmB, and then hydroxylated on the C5 position of the same residue by EpmC. Lysylation is critical for the stimulatory effect of EF-P on peptide-bond formation. The lysylation moiety would extend toward the peptidyltransferase center and stabilize the terminal 3-CCA end of the tRNA. The hydroxylation of the C5 position on @RESIDUE_NAME_AT_POS|Lys|34|P0A6N4|MF_00141|@ would allow additional potential stabilizing hydrogen-bond interactions with the P-tRNA.", protein);
        ResidueNameAtPosPlaceholderResolver.resolve(proteinAnnotation, positionalMappingList);
        assertThat(proteinAnnotation.getValue(), equalTo("Is beta-lysylated on the epsilon-amino group of Lys-114 by the combined action of EpmA and EpmB, and then hydroxylated on the C5 position of the same residue by EpmC. Lysylation is critical for the stimulatory effect of EF-P on peptide-bond formation. The lysylation moiety would extend toward the peptidyltransferase center and stabilize the terminal 3-CCA end of the tRNA. The hydroxylation of the C5 position on Lys-114 would allow additional potential stabilizing hydrogen-bond interactions with the P-tRNA."));
        assertFalse(proteinAnnotation.getHasPlaceholder());
    }

    @Test
    void resolveMultipleResidueNameAtPosWithOnePositionalMappingAndNullTemplateMatch(){
        Protein protein = TestUtils.createProtein("Q57GP3", "MPFSMRAIKARGTRRANKRLASCPARRFSSASICKRCSSSSGLVTTSASCANQSSRNGVFRVTICAILWLSYQLTISEGLMATYYSNDFRSGLKIMLDGEPYAVESSEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAECIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTAGTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRVK");
        Protein templateProtein = TestUtils.createProtein("P0A6N4", "MATYYSNDFRAGLKIMLDGEPYAVEASEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAECIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTAGTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRVK");
        Signature signature = TestUtils.createSignature(SignatureType.HAMAP, "MF_00141");

        PositionalProteinSignature templateMatch = TestUtils
                .createPositionalProteinSignature(templateProtein, signature, 3, 187,
                        "TYYSNDFRAGLKIMLDGePYAVEASEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAE-CIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTA-GTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRV");
        PositionalProteinSignature targetMatch = TestUtils.createPositionalProteinSignature(protein, signature, 83, 267,
                "TYYSNDFRSGLKIMLDGePYAVESSEFVKPGKGQAFARVKLRRLLTGTRVEKTFKSTDSAEGADVVDMNLTYLYNDGEFWHFMNNETFEQLSADAKAIGDNAKWLLDQAE-CIVTLWNGQPISVTPPNFVELEIVDTDPGLKGDTA-GTGGKPATLSTGAVVKVPLFVQIGEVIKVDTRSGEYVSRV");

        PositionalMapping positionalMapping = TestUtils.createUnmappedPositionalMapping(targetMatch, templateMatch, "34", "34");

        List<PositionalMapping> positionalMappingList = new ArrayList<>();
        positionalMappingList.add(positionalMapping);

        // above is the identical part as resolveMultipleResidueNameAtPosWithOnePositionalMapping method
        PositionalMapping mocked = mock(PositionalMapping.class);
        assertNull(mocked.getTemplateMatch());
        positionalMappingList.add(mocked);

        ProteinAnnotation proteinAnnotation = buildProteinAnnotation("Is beta-lysylated on the epsilon-amino group of @RESIDUE_NAME_AT_POS|Lys|34|P0A6N4|MF_00141|@ by the combined action of EpmA and EpmB, and then hydroxylated on the C5 position of the same residue by EpmC. Lysylation is critical for the stimulatory effect of EF-P on peptide-bond formation. The lysylation moiety would extend toward the peptidyltransferase center and stabilize the terminal 3-CCA end of the tRNA. The hydroxylation of the C5 position on @RESIDUE_NAME_AT_POS|Lys|34|P0A6N4|MF_00141|@ would allow additional potential stabilizing hydrogen-bond interactions with the P-tRNA.", protein);
        // when
        ResidueNameAtPosPlaceholderResolver.resolve(proteinAnnotation, positionalMappingList);
        // then
        assertThat(proteinAnnotation.getValue(), equalTo("Is beta-lysylated on the epsilon-amino group of Lys-114 by the combined action of EpmA and EpmB, and then hydroxylated on the C5 position of the same residue by EpmC. Lysylation is critical for the stimulatory effect of EF-P on peptide-bond formation. The lysylation moiety would extend toward the peptidyltransferase center and stabilize the terminal 3-CCA end of the tRNA. The hydroxylation of the C5 position on Lys-114 would allow additional potential stabilizing hydrogen-bond interactions with the P-tRNA."));
        assertFalse(proteinAnnotation.getHasPlaceholder());
    }

    @Test
    void resolveSingleResidueNameAtPosWithMultiplePositionalMappings(){
        Protein protein = TestUtils.createProtein("A0A235IQD1", "MTDEKIRQIAFYGKGGIGKSTTSQNTLAAMAEMGQRVLIVGCDPKADSTRLMLHSKAQTSVLQLAAERGAVEDIELEEVMLTGFRDVRCVESGGPEPGVGCAGRGIITAINFLEENGAYKDVDFVSYDVLGDVVCGGFAMPIREGKAQEIYIVTSGEMMAMYAANNIARGVLKYAHTGGVRLGGLICNSRNVDREIDLIETLAKRLNTQMIHYVPRDNIVQHAELRRMTVNEYAPDSNQANEYRTLGTKIINNKNLTVPTPIEMEELEDLLIEFGILESEENAAMLVGKTATEAPV");
        Protein templateProtein = TestUtils.createProtein("P00459", "MAMRQCAIYGKGGIGKSTTTQNLVAALAEMGKKVMIVGCDPKADSTRLILHSKAQNTIMEMAAEAGTVEDLELEDVLKAGYGGVKCVESGGPEPGVGCAGRGVITAINFLEEEGAYEDDLDFVFYDVLGDVVCGGFAMPIRENKAQEIYIVCSGEMMAMYAANNISKGIVKYANSGSVRLGGLICNSRNTDREDELIIALANKLGTQMIHFVPRDNVVQRAEIRRMTVIEYDPKAKQADEYRALARKVVDNKLLVIPNPITMDELEELLMEFGIMEVEDESIVGKTAEEV");
        Signature signature = TestUtils.createSignature(SignatureType.HAMAP, "MF_00533");

        PositionalProteinSignature templateMatch = TestUtils
                .createPositionalProteinSignature(templateProtein, signature, 3, 276,
                        "MRQCAIYGKGGIGKSTTTQNLVAALAEMGKKVMIVGCDPKADSTRLILHSKAQNTIMEMAAEAGTVEDLELEDVLKAGYGGVKCVESGGPEPGVGCAGRGVITAINFLEEEGAYED-DLDFVFYDVLGDVVCGGFAMPIRENKAQEIYIVCSGEMMAMYAANNISKGIVKYANSGSVRLGGLICNSRNTDREDELIIALANKLGTQMIHFVPRDNVVQRAEIRRMTVIEYDPKAKQADEYRALARKVVDNKLLVIPNPITMDELEELLMEFGIME");
        PositionalProteinSignature targetMatch = TestUtils.createPositionalProteinSignature(protein, signature, 6, 278,
                "IRQIAFYGKGGIGKSTTSQNTLAAMAEMGQRVLIVGCDPKADSTRLMLHSKAQTSVLQLAAERGAVEDIELEEVMLTGFRDVRCVESGGPEPGVGCAGRGIITAINFLEENGAYKD--VDFVSYDVLGDVVCGGFAMPIREGKAQEIYIVTSGEMMAMYAANNIARGVLKYAHTGGVRLGGLICNSRNVDREIDLIETLAKRLNTQMIHYVPRDNIVQHAELRRMTVNEYAPDSNQANEYRTLGTKIINNKNLTVPTPIEMEELEDLLIEFGILE");

        PositionalMapping positionalMapping1 = TestUtils.createUnmappedPositionalMapping(targetMatch, templateMatch, "101", "101");
        PositionalMapping positionalMapping2 = TestUtils.createUnmappedPositionalMapping(targetMatch, templateMatch, "98", "98");
        PositionalMapping positionalMapping3 = TestUtils.createUnmappedPositionalMapping(targetMatch, templateMatch, "10", "17");


        List<PositionalMapping> positionalMappingList = new ArrayList<>();
        positionalMappingList.add(positionalMapping1);
        positionalMappingList.add(positionalMapping2);
        positionalMappingList.add(positionalMapping3);

        ProteinAnnotation proteinAnnotation = buildProteinAnnotation("The reversible ADP-ribosylation of @RESIDUE_NAME_AT_POS|Arg|101|P00459|MF_00533|@ inactivates the nitrogenase reductase and regulates nitrogenase activity.", protein);
        ResidueNameAtPosPlaceholderResolver.resolve(proteinAnnotation, positionalMappingList);
        assertThat(proteinAnnotation.getValue(), equalTo("The reversible ADP-ribosylation of Arg-104 inactivates the nitrogenase reductase and regulates nitrogenase activity."));
        assertFalse(proteinAnnotation.getHasPlaceholder());
    }

    @Test
    void residueNameAtPosUnresolvedWhenNoPositionalMapping(){
        Protein protein = TestUtils.createProtein("A0A235IQD1", "MTDEKIRQIAFYGKGGIGKSTTSQNTLAAMAEMGQRVLIVGCDPKADSTRLMLHSKAQTSVLQLAAERGAVEDIELEEVMLTGFRDVRCVESGGPEPGVGCAGRGIITAINFLEENGAYKDVDFVSYDVLGDVVCGGFAMPIREGKAQEIYIVTSGEMMAMYAANNIARGVLKYAHTGGVRLGGLICNSRNVDREIDLIETLAKRLNTQMIHYVPRDNIVQHAELRRMTVNEYAPDSNQANEYRTLGTKIINNKNLTVPTPIEMEELEDLLIEFGILESEENAAMLVGKTATEAPV");
        ProteinAnnotation proteinAnnotation = buildProteinAnnotation("The reversible ADP-ribosylation of @RESIDUE_NAME_AT_POS|Arg|101|P00459|MF_00533|@ inactivates the nitrogenase reductase and regulates nitrogenase activity.", protein);
        ResidueNameAtPosPlaceholderResolver.resolve(proteinAnnotation, newArrayList());
        assertThat(proteinAnnotation.getValue(), equalTo("The reversible ADP-ribosylation of @RESIDUE_NAME_AT_POS|Arg|101|P00459|MF_00533|@ inactivates the nitrogenase reductase and regulates nitrogenase activity."));
        assertTrue(proteinAnnotation.getHasPlaceholder());
    }

    private ProteinAnnotation buildProteinAnnotation(String annotationValue, Protein protein){
        return ProteinAnnotation.builder().withProtein(protein).withValue(annotationValue).withHasPlaceholder(true).build();
    }

}