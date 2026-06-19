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

package uk.ac.ebi.uniprot.urml.input.parsers.xml;

import uk.ac.ebi.uniprot.urml.core.xml.schema.JAXBContextInitializationException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.primitives.Ints.asList;
import static javax.xml.stream.XMLStreamConstants.*;

/**
 * Idea taken from  https://stackoverflow.com/questions/1134189/can-jaxb-parse-large-xml-files-in-chunks
 * @param <T>
 * @author Hermann Zellner
 */

public class PartialUnmarshaller<T> implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(PartialUnmarshaller.class);

    XMLStreamReader reader;
    Class<T> clazz;
    Unmarshaller unmarshaller;

    public PartialUnmarshaller(InputStream stream, Class<T> clazz) throws IOException {
        this(stream, clazz, 1);
    }

    public PartialUnmarshaller(InputStream stream, Class<T> clazz, int numSkip) throws IOException {
        this.clazz = clazz;
        try {
            this.unmarshaller = JAXBContext.newInstance(clazz).createUnmarshaller();
        } catch (JAXBException e) {
            throw new JAXBContextInitializationException(
                    String.format("Cannot initialize %s", this.getClass().getSimpleName()), e);
        }
        try {
            this.reader = XMLInputFactory.newInstance().createXMLStreamReader(stream);
            /* ignore headers */
            skipElements(START_DOCUMENT, COMMENT, DTD);
            /* ignore root elements */
            for (int i=0; i<numSkip; ++i) {
                int tag = reader.nextTag();
            }
            /* if there's no tag, ignore root element's end */
            skipElements(END_ELEMENT);
        } catch (XMLStreamException e) {
            throw new IOException("Cannot parse the input source", e);
        }
    }

    public T next() throws XMLStreamException, JAXBException {
        if (!hasNext())
            throw new NoSuchElementException();

        T value = unmarshaller.unmarshal(reader, clazz).getValue();

        skipElements(CHARACTERS, END_ELEMENT);
        return value;
    }

    public boolean hasNext() throws XMLStreamException{
        return reader.hasNext();
    }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            logger.warn("XMLStreamException while closing XMLStreamReader {}", e.getMessage());
        }
    }

    void skipElements(int... elements) throws XMLStreamException {
        int eventType = reader.getEventType();

        List<Integer> types = asList(elements);
        while (types.contains(eventType))
            eventType = reader.next();
    }
}
