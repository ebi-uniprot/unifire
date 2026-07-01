package uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro6;

import org.apache.commons.lang3.StringUtils;

public enum InterPro6SignatureLibrary {
    ANTIFAM("AntiFam"),
    CATHGENE3D("CATH-Gene3D"),
    CATHFUNFAM("CATH-FunFam"),
    CDD("CDD"),
    COILS("COILS"),
    DEEPTMHHM("DeepTMHMM"),
    HAMAP("HAMAP"),
    INTERPRO_N("InterPro-N"),
    MOBIDBLITE("MobiDB-lite"),
    NCBIFAM("NCBIFAM"),
    PANTHER("PANTHER"),
    PHOBIUS("Phobius"),
    PFAM("Pfam"),
    PIRSF("PIRSF"),
    PIRSR("PIRSR"),
    PRINTS("PRINTS"),
    PROSITEPATTERNS("PROSITE patterns"),
    PROSITEPROFILES("PROSITE profiles"),
    SFLD("SFLD"),
    SMART("SMART"),
    SUPERFAMILY("SUPERFAMILY"),
    SIGNALP_EUK("SignalP-Euk"),
    SIGNALP_PROK("SignalP-Prok"),
    TMBED("TMbed");

    private String key;

    InterPro6SignatureLibrary(String key) {
        this.setKey(key);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public static InterPro6SignatureLibrary fromKey(String key) {
        for (InterPro6SignatureLibrary library : values()) {
            if (StringUtils.equals(library.getKey(), key)) {
                return library;
            }
        }
        return null;
    }
}
