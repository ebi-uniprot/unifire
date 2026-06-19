/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ebi.uniprot.aa.interproscan6.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.Set;

/**
 * Location(s) of match on protein sequence
 *
 * @author Antony Quinn
 * @version $Id$
 */
@XmlType(name = "HmmerLocationWithSitesType", propOrder = {"score", "evalue", "hmmStart", "hmmEnd", "hmmLength"})
@JsonIgnoreProperties({"id", "hmmBounds"}) // hmmBounds is not output in the json
public abstract class HmmerLocationWithSites<T extends LocationFragment> extends LocationWithSites<HmmerLocationWithSites.HmmerSite, T> {

    private int hmmStart;

    private int hmmEnd;

    private int hmmLength;

//     (nullable = false, name="hmm_bounds")
//
//    private HmmBounds hmmBounds;

    /**
     * Storing the hmmBounds just as the symbol representation,
     * rather than the eval, so [], [. etc. can be stored
     * in the database.
     */
    private String hmmBounds;

    private double evalue;

    private double score;

    /**
     * protected no-arg constructor required by JPA - DO NOT USE DIRECTLY.
     */
    protected HmmerLocationWithSites() {
    }

    // Don't use Builder pattern because all fields are required
    public HmmerLocationWithSites(T locationFragment, double score, double evalue,
                                  int hmmStart, int hmmEnd, int hmmLength, HmmBounds hmmBounds, Set<HmmerSite> sites) {
        super(locationFragment, sites);
        setHmmStart(hmmStart);
        setHmmEnd(hmmEnd);
        setHmmLength(hmmLength);
        setHmmBounds(hmmBounds);
        setEvalue(evalue);
        setScore(score);
    }

    @XmlAttribute(name = "hmm-start", required = true)
    public int getHmmStart() {
        return hmmStart;
    }

    private void setHmmStart(int hmmStart) {
        this.hmmStart = hmmStart;
    }

    @XmlAttribute(name = "hmm-end", required = true)
    public int getHmmEnd() {
        return hmmEnd;
    }

    private void setHmmEnd(int hmmEnd) {
        this.hmmEnd = hmmEnd;
    }

    @XmlAttribute(name = "hmm-length", required = true)
    public int getHmmLength() {
        return hmmLength;
    }

    private void setHmmLength(int hmmLength) {
        this.hmmLength = hmmLength;
    }

    @XmlAttribute(name="hmm-bounds", required=true)
    public HmmBounds getHmmBounds() {
        return HmmBounds.parseSymbol(hmmBounds);
    }

    private void setHmmBounds(HmmBounds hmmBounds) {
        this.hmmBounds = hmmBounds.getSymbol();
    }

    @XmlAttribute(required = true)
    public double getEvalue() {
        return evalue;
    }

    private void setEvalue(double evalue) {
        this.evalue = evalue;
    }

    @XmlAttribute(required = true)
    public double getScore() {
        return score;
    }

    private void setScore(double score) {
        this.score = score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof HmmerLocationWithSites))
            return false;
        final HmmerLocationWithSites h = (HmmerLocationWithSites) o;
        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(hmmStart, h.hmmStart)
                .append(hmmEnd, h.hmmEnd)
                .append(hmmLength, h.hmmLength)
                .append(hmmBounds, h.hmmBounds)
                .append(score, h.score)
                .isEquals()
                &&
                PersistenceConversion.equivalent(evalue, h.evalue);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 53)
                .appendSuper(super.hashCode())
                .append(hmmStart)
                .append(hmmEnd)
                .append(hmmLength)
                .append(hmmBounds)
                .append(score)
                .append(evalue)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @XmlType(name = "HmmerSiteType")
    public static class HmmerSite extends Site {

        protected HmmerSite() {
        }

        public HmmerSite(String description, Set<SiteLocation> siteLocations) {
            super(description, siteLocations);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof HmmerSite))
                return false;
            return new EqualsBuilder()
                    .appendSuper(super.equals(o))
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(41, 59)
                    .appendSuper(super.hashCode())
                    .toHashCode();
        }

        public Object clone() throws CloneNotSupportedException {
            final Set<SiteLocation> clonedSiteLocations = new HashSet<>(this.getSiteLocations().size());
            for (SiteLocation sl : this.getSiteLocations()) {
                clonedSiteLocations.add((SiteLocation) sl.clone());
            }
            return new HmmerSite(this.getDescription(), clonedSiteLocations);
        }

    }


}