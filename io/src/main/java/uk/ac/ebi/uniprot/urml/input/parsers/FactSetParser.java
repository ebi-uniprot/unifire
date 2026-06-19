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

package uk.ac.ebi.uniprot.urml.input.parsers;

import uk.ac.ebi.uniprot.urml.input.InputType;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.facts.FactXmlParser;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro.InterProXmlProteinParser;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro6.InterPro6XmlProteinParser;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.uniparc.UniParcXmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.uniprot.urml.facts.FactSet;

/**
 * Input parser specification
 *
 * @author Alexandre Renaux
 */
public interface FactSetParser {

    Iterator<FactSet> parse(InputStream input) throws IOException;

    /**
     * Create the appropriate parser {@link FactSetParser} for the given {@link InputType}
     * @param inputType type of input
     * @return the parser to read this input
     */
    static FactSetParser of(InputType inputType){
        switch (inputType){
            case FACT_XML:
                return new FactXmlParser();
            case INTERPROSCAN_XML:
                return new InterProXmlProteinParser();
            case INTERPROSCAN6_XML:
                return new InterPro6XmlProteinParser();
            case UNIPARC_XML:
                return new UniParcXmlParser();
            default:
                throw new IllegalArgumentException("Unsupported input type "+inputType);
        }
    }

}
