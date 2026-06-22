package uk.ac.ebi.uniprot.urml.input.parsers.xml.interpro6;

import org.apache.commons.lang3.StringUtils;

public enum InterPro6SignatureLibrary {
    ANTIFAM("AntiFam", "AntiFam", "Prediction of spurious protein.", false),
    CDD("CDD", "CDD", "Prediction of CDD domains", true),
    COILS("COILS", "Coils", "Description to be added", false),
    FUNFAM("CATH-FunFam", "FunFam", "Sub-classification of superfamilies into functional families", false),
    GENE3D("CATH-Gene3D", "Gene3D", "Description to be added", true),
    HAMAP("HAMAP", "Hamap", "Description to be added", true),
    MOBIDB_LITE("MobiDB-lite", "MobiDBLite", "Predicts disordered protein regions", false),
    NCBIFAM("NCBIFAM", "NCBIfam", "NCBIfam is a collection of protein families based on Hidden Markov Models (HMMs)", true),
    PANTHER("PANTHER", "PANTHER", "The PANTHER (Protein ANalysis THrough Evolutionary Relationships) Classification System is a unique resource that classifies genes by their functions, using published scientific experimental evidence and evolutionary relationships to predict function even in the absence of direct experimental evidence.", true),
    PFAM("Pfam", "Pfam", "Description to be added", true),
    PHOBIUS("Phobius", "Phobius", "Prediction of signal peptides and trans-membrane regions", false),
    PIRSF("PIRSF", "PIRSF", "Family classification system at the Protein Information Resource", true),
    PIRSR("PIRSR", "PIRSR", "Family classification system - Residue level at the Protein Information Resource", false),
    PRINTS("PRINTS", "PRINTS", "Description to be added", true),
    PROSITE_PATTERNS("PROSITE patterns", "ProSitePatterns", "Description to be added", true),
    PROSITE_PROFILES("PROSITE profiles", "ProSiteProfiles", "Description to be added", true),
    SFLD("SFLD", "SFLD", "Description to be added", true),
    SIGNALP_EUK("SIGNALP_EUK", "SignalP_EUK", "SignalP (organism type eukaryotes) predicts the presence and location of signal peptide cleavage sites in amino acid sequences for eukaryotes.", false),
    SIGNALP_GRAM_NEGATIVE("SIGNALP_GRAM_NEGATIVE", "SignalP_GRAM_NEGATIVE", "SignalP (organism type gram-negative prokaryotes) predicts the presence and location of signal peptide cleavage sites in amino acid sequences for gram-negative prokaryotes.", false),
    SIGNALP_GRAM_POSITIVE("SIGNALP_GRAM_POSITIVE", "SignalP_GRAM_POSITIVE", "SignalP (organism type gram-positive prokaryotes) predicts the presence and location of signal peptide cleavage sites in amino acid sequences for gram-positive prokaryotes.", false),
    SMART("SMART", "SMART", "Description to be added", true),
    SUPERFAMILY("SUPERFAMILY", "SUPERFAMILY", "Description to be added", true),
    TMHMM("TMHMM", "TMHMM", "Prediction of transmembrane helices in proteins.", false),
    TIGRFAM("TIGRFAM", "TIGRFAM", "Description to be added", true),
    PRODOM("ProDom", "ProDom", "Description to be added", true);

    private String key;
    private String name;
    private String description;
    private boolean interproMDB;

    InterPro6SignatureLibrary(String key, String name, String description, boolean interproMDB) {
        this.setKey(key);
        this.setName(name);
        this.setDescription(description);
        this.setInterproMDB(interproMDB);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return this.name;
    }

    private void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    public boolean isInterproMDB() {
        return this.interproMDB;
    }

    private void setInterproMDB(boolean interproMDB) {
        this.interproMDB = interproMDB;
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
