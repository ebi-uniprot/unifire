package uk.ac.ebi.uniprot.aa.interproscan6.model;

public enum TMbedSignature {
    TMbeta_out_to_in("TMbeta_out-to-in", "Transmembrane beta strand (out-to-in)", "Transmembrane alpha helix (out-to-in)"),
    TMbeta_in_to_out("TMbeta_in-to-out", "Transmembrane beta strand (in-to-out)", "Transmembrane alpha helix (in-to-out)"),
    TMhelix_out_to_in("TMhelix_out-to-in", "Transmembrane alpha helix (out-to-in)", "Transmembrane alpha helix (out-to-in)"),
    TMhelix_in_to_out("TMhelix_in-to-out", "Transmembrane alpha helix (in-to-out)", "Transmembrane alpha helix (in-to-out)"),
    SIGNAL_PEPTIDE("SIGNAL_PEPTIDE", "Signal Peptide", "Signal peptide region");

    private String accession;
    private String shortDesc;
    private String description;

    private TMbedSignature(String accession, String shortDesc, String description) {
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

    public static boolean isTransmembraneHelix(String signature) {
        return signature.equals(TMhelix_in_to_out.toString()) ||
                signature.equals(TMhelix_out_to_in.toString());
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