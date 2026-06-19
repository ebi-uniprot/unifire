package uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro6;

import org.uniprot.urml.facts.FactSet;
import uk.ac.ebi.uniprot.aa.interproscan6.model.Protein;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.AbstractXmlFactSetChunkParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

public class InterPro6XmlFactSetChunkParser extends AbstractXmlFactSetChunkParser<Protein> {

    private static final Integer DEFAULT_CHUNKSIZE = 1000;

    public InterPro6XmlFactSetChunkParser(InputStream interproXmlIS) throws IOException {
        this(interproXmlIS, DEFAULT_CHUNKSIZE);
    }

    public InterPro6XmlFactSetChunkParser(InputStream inputStream, Integer chunkSize) throws IOException {
        super(inputStream, chunkSize);
    }



    @Override
    protected Iterator<FactSet> convertToFactSet(Collection<Protein> xmlEntities) {
        return new InterPro6XmlProteinConverter(xmlEntities);
    }
}