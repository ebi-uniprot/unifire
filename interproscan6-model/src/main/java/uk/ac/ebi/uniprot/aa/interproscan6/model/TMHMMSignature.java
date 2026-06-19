package uk.ac.ebi.uniprot.aa.interproscan6.model;

@Deprecated
public enum TMHMMSignature {
    INSIDE_CELL("inside", "inside cell region", "inside cell region"),
    OUTSIDE_CELL("outside", "outside cell region", "outside cell region"),
    MEMBRANE("TMhelix", "transmembrane helix", "Region of a membrane-bound protein predicted to be embedded in the membrane."),
    OTHER("O", "O region", (String)null);

    private String accession;
    private String shortDesc;
    private String description;

    private TMHMMSignature(String accession, String shortDesc, String description) {
        this.accession = accession;
        this.shortDesc = shortDesc;
        this.description = description;
    }

    public String getAccession() {
        return this.accession;
    }

    public String getShortDesc() {
        return this.shortDesc;
    }

    public String getDescription() {
        return this.description;
    }

    public String toString() {
        return this.accession;
    }

//    public static boolean isValidSignature(Signature signature) {
//        for(TMHMMSignature type : values()) {
//            if (type.getAccession().equals(signature.getAccession())) {
//                return true;
//            }
//        }
//
//        return false;
//    }
}