/*
 * Copyright (c) 2018 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.uniprot.urml.output.xml;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import org.uniprot.urml.facts.Fact;
import org.uniprot.urml.facts.FactSet;
import org.uniprot.urml.facts.ObjectFactory;
import org.w3c.dom.Document;
import uk.ac.ebi.uniprot.urml.core.xml.schema.URMLConstants;
import uk.ac.ebi.uniprot.urml.core.xml.schema.mappers.FactNamespaceMapper;
import uk.ac.ebi.uniprot.urml.core.xml.writers.AbstractURMLWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static javax.xml.stream.XMLOutputFactory.newFactory;
import static uk.ac.ebi.uniprot.urml.core.xml.schema.URMLConstants.URML_FACT_NAMESPACE;

/**
 * @author Vishal Joshi
 */
public class IndividualFactWriter extends AbstractURMLWriter<FactSet, Fact> {

    public IndividualFactWriter(OutputStream outputStream) throws XMLStreamException, JAXBException {
        super(outputStream, JAXBContext.newInstance(URMLConstants.URML_FACTS_JAXB_CONTEXT), URML_FACT_NAMESPACE);
        writeRoot(getRootDocument(new FactSet()));
    }

    @Override
    public void write(FactSet superSet) throws XMLStreamException {
        superSet.getFact().forEach(fact -> {
            try {
                writeElementWise(fact);
            } catch (JAXBException e) {
                throw new IllegalArgumentException("Error writing fact " + fact, e);
            }
        });
        completeWrite();
    }

    @Override
    public void writeElementWise(Fact fact) throws JAXBException {
        Marshaller marshaller = initMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        QName qualifiedName = new QName(URML_FACT_NAMESPACE, URMLConstants.URML_FACT_TAG);
        JAXBElement<Fact> jbe = new JAXBElement<>(qualifiedName, Fact.class, fact);
        try {
            marshaller.marshal(jbe, this.xmlStreamWriter);
        } catch (JAXBException | RuntimeException e) {
            throw new JAXBException("Cannot marshall fact: " + fact, e);
        }
    }

    @Override
    public Marshaller initMarshaller() throws JAXBException {
        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new FactNamespaceMapper());
        return m;
    }

    @Override
    protected Document getRootDocument(FactSet factSet) {
        List<Fact> factList = null;
        if (factSet == null) {
            factSet = new ObjectFactory().createFactSet();
        } else if (factSet.isSetFact()) {
            factList = factSet.getFact();
            factSet.unsetFact();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XMLStreamWriter tmpWriter = newFactory().createXMLStreamWriter(outputStream);
            tmpWriter = new IndentingXMLStreamWriter(tmpWriter);
            Marshaller marshaller = initMarshaller();
            marshaller.marshal(factSet, tmpWriter);
            if (factList != null) {
                factSet.setFact(factList);
            }
            try (InputStream newInput = new ByteArrayInputStream(outputStream.toByteArray())) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                // Addresses CWE-611 vulnerability
                dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                dbFactory.setNamespaceAware(true);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                return dBuilder.parse(newInput);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Conversion failed for document root", e);
        }
    }
}
