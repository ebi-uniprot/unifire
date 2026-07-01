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

package uk.ac.ebi.uniprot.urml.input.parsers;

import uk.ac.ebi.uniprot.urml.input.InputType;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro.InterProXmlFactSetChunkParser;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro6.InterPro6XmlFactSetChunkParser;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.uniparc.UniParcXmlFactSetChunkParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import org.uniprot.urml.facts.FactSet;

public interface FactSetChunkParser extends AutoCloseable {
    Iterator<FactSet> nextChunk() throws XMLStreamException, JAXBException;
    boolean hasNext() throws XMLStreamException;

    static FactSetChunkParser of(InputType inputType, InputStream inputStream) throws IOException {
        return of(inputType, inputStream, null);
    }

    static FactSetChunkParser of(InputType inputType, InputStream inputStream, Integer chunksize) throws IOException {
        if (chunksize == null ) {
            switch (inputType) {
                case FACT_XML:
                    throw new UnsupportedOperationException("FACT-XML cannot be parsed chunk-by-chunk.");
                case INTERPROSCAN_XML:
                    return new InterProXmlFactSetChunkParser(inputStream);
                case INTERPROSCAN6_XML:
                    return new InterPro6XmlFactSetChunkParser(inputStream);
                case UNIPARC_XML:
                    return new UniParcXmlFactSetChunkParser(inputStream);
                default:
                    throw new IllegalArgumentException("Unsupported input type " + inputType);
            }
        }
        else {
            switch (inputType) {
                case FACT_XML:
                    throw new UnsupportedOperationException("FACT-XML cannot be parsed chunk-by-chunk.");
                case INTERPROSCAN_XML:
                    return new InterProXmlFactSetChunkParser(inputStream, chunksize);
                case INTERPROSCAN6_XML:
                    return new InterPro6XmlFactSetChunkParser(inputStream, chunksize);
                case UNIPARC_XML:
                    return new UniParcXmlFactSetChunkParser(inputStream, chunksize);
                default:
                    throw new IllegalArgumentException("Unsupported input type " + inputType);
            }
        }
    }
}
