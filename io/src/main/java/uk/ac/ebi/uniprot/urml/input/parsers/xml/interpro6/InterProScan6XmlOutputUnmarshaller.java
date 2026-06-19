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

import uk.ac.ebi.uniprot.aa.interproscan6.model.generated.ResultsType;
import uk.ac.ebi.uniprot.urml.core.xml.schema.JAXBContextInitializationException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

/**
 * Unmarshalls the InterProScan output XML using the interpro schema model (cf. {@link uk.ac.ebi.uniprot.aa.interproscan6.model.generated})
 *
 * @author Alexandre Renaux modified for InterProScan 6 by Muhammad Hilmy
 */
public class InterProScan6XmlOutputUnmarshaller {

    private Unmarshaller unmarshaller;

    public InterProScan6XmlOutputUnmarshaller() {
        try {
            JAXBContext context = JAXBContext.newInstance(ResultsType.class);
            unmarshaller = context.createUnmarshaller();
        } catch (JAXBException e) {
            throw new JAXBContextInitializationException("Cannot initialize " + this.getClass().getSimpleName(), e);
        }
    }

    public ResultsType read(InputStream inputStream) throws JAXBException, IOException {
        if (inputStream == null) {
            throw new IOException("Null input stream");
        }
        return ((ResultsType) unmarshaller.unmarshal(inputStream));
    }


}
