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

package uk.ac.ebi.uniprot.urml.core.xml.readers;

import uk.ac.ebi.uniprot.urml.core.xml.schema.JAXBContextInitializationException;
import uk.ac.ebi.uniprot.urml.core.xml.schema.URMLConstants;
import uk.ac.ebi.uniprot.urml.core.xml.schema.resolvers.StatefulIDResolver;

import com.sun.xml.bind.IDResolver;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.urml.facts.FactSet;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * URML fact unmarshaller using JAXB.
 *
 * @author Alexandre Renaux
 */
public class URMLFactReader implements URMLReader<FactSet> {

    private final Logger logger = LoggerFactory.getLogger(URMLFactReader.class);

    private JAXBContext context;
    private XMLReader xmlReader;

    public URMLFactReader() {
        try {
            context = JAXBContext.newInstance(URMLConstants.URML_FACTS_JAXB_CONTEXT);
            SAXParserFactory spf = SAXParserFactory.newInstance();
            // Addresses CWE-611 vulnerability
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setNamespaceAware(true);
            spf.setXIncludeAware(true);
            spf.setValidating(true);
            xmlReader = spf.newSAXParser().getXMLReader();
        } catch (Exception e){
            throw new JAXBContextInitializationException("Cannot initialize the URML fact reader", e);
        }
    }

    @Override
    public FactSet read(InputStream factInputStream) throws JAXBException, IOException {
        return read(factInputStream, context.createUnmarshaller());
    }

    public FactSet read(InputStream factInputStream, Unmarshaller unmarshaller) throws JAXBException, IOException {
        logger.info("Reading input facts");
        if (factInputStream == null){
            throw new IOException("Null input stream");
        }
        SAXSource source = new SAXSource(xmlReader, new InputSource(factInputStream));
        JAXBElement<?> jaxbElement = (JAXBElement<?>) unmarshaller.unmarshal(source);
        return ((FactSet) jaxbElement.getValue());
    }

    /**
     * Preload the unmarshaller with facts that are anchored by xsd:id.
     * @see <a href="https://www.w3.org/TR/xmlschema11-2/#ID">W3C:ID</a>
     * This method can be used when multiple XML are connected to each other through ID/IDREF references.
     * @see <a href="https://www.w3.org/Consortium/Offices/Presentations/DTD/slide20-0.htm">ID/IDREF example</a>
     * Once the dependant facts are loaded, {@link #read(InputStream, Unmarshaller)} method can be used with
     * the returned {@link Unmarshaller}.
     * For multiple dependances, use {@link #loadDependentFacts(InputStream, Unmarshaller)}
     * @param factInputStream anchored facts to be loaded in the returned unmarshaller
     * @return an unmarshaller, loaded with the anchored facts
     * @throws JAXBException if {@param factInputStream} cannot be unmarshalled
     */
    public Unmarshaller loadDependentFacts(InputStream factInputStream) throws JAXBException{
        return loadDependentFacts(factInputStream, context.createUnmarshaller());
    }

    /**
     * @see #loadDependentFacts(InputStream, Unmarshaller)
     * Multiple dependant XML files can be loaded by reusing the returned {@link Unmarshaller} as {@param unmarshaller}.
     *
     * @param unmarshaller unmarshaller from {@link #loadDependentFacts(InputStream)}
     * @param factInputStream anchored facts to be loaded in the {@param unmarshaller}
     * @return the unmarshaller, provided by {@param unmarshaller}, loaded with the anchored facts
     * @throws JAXBException if {@param factInputStream} cannot be unmarshaled
     */
    public Unmarshaller loadDependentFacts(InputStream factInputStream, Unmarshaller unmarshaller) throws JAXBException{
        logger.info("Loading input dependant facts");
        unmarshaller.setProperty(IDResolver.class.getName(), new StatefulIDResolver());
        SAXSource source = new SAXSource(xmlReader, new InputSource(factInputStream));
        unmarshaller.unmarshal(source);
        return unmarshaller;
    }

}
