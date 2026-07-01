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

import org.uniprot.urml.facts.FactSet;
import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.ResultsType;
import uk.ac.ebi.uniprot.urml.input.parsers.FactSetParser;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.XmlUnmarshaller;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Parses the InterProScan XML output and provides an iterator of {@link FactSet}
 *
 * @author Alexandre Renaux
 */
public class InterPro6XmlProteinParser implements FactSetParser {

    private final XmlUnmarshaller<ResultsType> interProXMLUnmarshaller;

    public InterPro6XmlProteinParser() {
        this.interProXMLUnmarshaller = new XmlUnmarshaller<>(ResultsType.class);
    }

    public Iterator<FactSet> parse(InputStream inputStream) throws IOException {
        ResultsType resultsType;
        try {
            resultsType = interProXMLUnmarshaller.read(inputStream);
        } catch (JAXBException e) {
            throw new IOException("Cannot parse the input source", e);
        }

        return new InterPro6XmlProteinConverter(resultsType);
    }

}
