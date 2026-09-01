package uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro;

import uk.ac.ebi.uniprot.aa.interpro.scan.model.Protein;
import uk.ac.ebi.uniprot.urml.input.parsers.xml.AbstractXmlFactSetChunkParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import org.uniprot.urml.facts.FactSet;

public class InterProXmlFactSetChunkParser extends AbstractXmlFactSetChunkParser<Protein> {

    private static final Integer DEFAULT_CHUNKSIZE = 1000;

    public InterProXmlFactSetChunkParser(InputStream interproXmlIS) throws IOException {
        this(interproXmlIS, DEFAULT_CHUNKSIZE);
    }

    public InterProXmlFactSetChunkParser(InputStream inputStream, Integer chunkSize) throws IOException {
        super(inputStream, chunkSize);
    }



    @Override
    protected Iterator<FactSet> convertToFactSet(Collection<Protein> xmlEntities) {
        return new InterProXmlProteinConverter(xmlEntities);
    }
}