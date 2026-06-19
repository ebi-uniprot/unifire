package uk.ac.ebi.uniprot.aa.interproscan6.model.alignment;

// Copied from InterProScan library uk.ac.ebi.uniprot.aa.interpro.scan.model.model.raw.alignment.AlignmentEncoder.
// After migration from InterproScan 6, some libraries were removed/added.
// However, the interproscan-model dependency is not maintained anymore to reflect the changes.
// Therefore, we have made copy of the used classes and removed the dependency.
// https://github.com/ebi-pf-team/interproscan/tree/master/core/model/src/main/java/uk/ac/ebi/interpro/scan/model

/**
 * Encode sequence alignment.
 *
 * @author  Antony Quinn
 * @version $Id$
 */
public interface AlignmentEncoder {

    public String encode(String alignment);

    public String decode(String sequence, String encodedAlignment, int start, int end);

    /**
     * Provides information about the encoded alignment
     *
     * @author  Antony Quinn
     */
    public interface Parser {
        public int getMatchCount();
        public int getInsertCount();
        public int getDeleteCount();
    }

}