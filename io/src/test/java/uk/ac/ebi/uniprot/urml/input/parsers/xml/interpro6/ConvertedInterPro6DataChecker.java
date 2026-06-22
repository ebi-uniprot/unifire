/*
 *  Copyright (c) 2021 European Molecular Biology Laboratory
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

import org.uniprot.urml.facts.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

abstract class ConvertedInterPro6DataChecker {

    protected int proteinCounter = 0;
    private int organismCounter = 0;
    private int proteinSignatureCounter = 0;

    void check(FactSet factSet){
        organismCounter = 0;
        proteinSignatureCounter = 0;
        for (Fact fact : factSet.getFact()) {
            if (fact instanceof Organism) {
                checkOrganism((Organism) fact);
                organismCounter++;
            } else if (fact instanceof ProteinSignature) {
                ProteinSignature proteinSignature = (ProteinSignature) fact;
                checkProteinSignature(proteinSignature);
                checkProtein((Protein) proteinSignature.getProtein());
                proteinSignatureCounter++;
                proteinCounter++;
            } else if (fact instanceof Protein){
                Protein protein = (Protein) fact;
                checkProtein(protein);
                checkOrganism(protein.getOrganism());
                proteinCounter++;
            } else {
                fail("Unexpected fact type: "+fact);
            }
        }
        assertThat(organismCounter, equalTo(organismCounter));
        assertThat(proteinSignatureCounter, equalTo(expectedNumberOfProteinSignatures()));
    }

    protected abstract int expectedNumberOfProteins();

    protected abstract int expectedNumberOfProteinSignatures();

    protected abstract void checkProtein(Protein protein);

    protected abstract void checkOrganism(Organism organism);

    protected abstract void checkProteinSignature(ProteinSignature proteinSignature);

    void checkInterProSignatureType(ProteinSignature proteinSignature, SignatureType expectedLibraryType){
        if (proteinSignature.getSignature().getValue().startsWith("IPR")){
            assertThat(proteinSignature.getSignature().getType(), equalTo(SignatureType.INTER_PRO));
        } else {
            assertThat(proteinSignature.getSignature().getType(), equalTo(expectedLibraryType));
        }
    }
}
