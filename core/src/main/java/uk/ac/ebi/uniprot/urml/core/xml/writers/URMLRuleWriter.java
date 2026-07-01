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

package uk.ac.ebi.uniprot.urml.core.xml.writers;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import org.uniprot.urml.rules.ObjectFactory;
import org.uniprot.urml.rules.Rule;
import org.uniprot.urml.rules.Rules;
import org.w3c.dom.Document;
import uk.ac.ebi.uniprot.urml.core.xml.schema.URMLConstants;
import uk.ac.ebi.uniprot.urml.core.xml.schema.mappers.RuleNamespaceMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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
import static uk.ac.ebi.uniprot.urml.core.xml.schema.URMLConstants.URML_RULE_NAMESPACE;

/**
 * URML rule writer using JAXB.
 *
 * @author Alexandre Renaux
 */
public class URMLRuleWriter extends AbstractURMLWriter<Rules, Rule> {

    public URMLRuleWriter(OutputStream outputStream, Rules rules) throws XMLStreamException, JAXBException {
        super(outputStream, JAXBContext.newInstance(URMLConstants.URML_RULES_JAXB_CONTEXT), URML_RULE_NAMESPACE);
        writeRoot(getRootDocument(rules));
    }

    @Override
    public void write(Rules rules) throws JAXBException, XMLStreamException {
        Marshaller marshaller = initMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(rules, xmlStreamWriter);
        completeWrite();
    }

    @Override
    public void writeElementWise(Rule rule) throws JAXBException {
        Marshaller marshaller = initMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        try {
            marshaller.marshal(rule, xmlStreamWriter);
        } catch (JAXBException | RuntimeException e) {
            throw new JAXBException("Cannot marshall rule: " + rule, e);
        }
    }

    @Override
    protected Document getRootDocument(Rules rules) {
        List<Rule> ruleList = null;
        if (rules == null) {
            rules = new ObjectFactory().createRules();
        } else if (rules.isSetRule()) {
            ruleList = rules.getRule();
            rules.unsetRule();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XMLStreamWriter tmpWriter = newFactory().createXMLStreamWriter(outputStream);
            tmpWriter = new IndentingXMLStreamWriter(tmpWriter);
            Marshaller marshaller = initMarshaller();
            marshaller.marshal(rules, tmpWriter);
            if (ruleList != null) {
                rules.setRule(ruleList);
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

    @Override
    protected Marshaller initMarshaller() throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new RuleNamespaceMapper());
        return marshaller;
    }
}
