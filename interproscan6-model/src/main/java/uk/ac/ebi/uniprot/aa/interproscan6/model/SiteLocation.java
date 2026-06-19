/*
 * Copyright 2009 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * Location of match on protein sequence.
 *
 * @version $Id$
 * @since 1.0
 */

@XmlType(name = "ResidueLocationType", propOrder = {"residue", "start", "end"})
@JsonIgnoreProperties({"id"})
public class SiteLocation implements Serializable, Cloneable {
    private Long id;

    // to match start - 'start' is reserved word in SQL.
    private int start;

    // 'end' is reserved word in SQL.
    private int end;

    private String residue;

    @JsonBackReference
    private Site site;

    /**
     * protected no-arg constructor required by JPA - DO NOT USE DIRECTLY.
     */
    protected SiteLocation() {
    }

    public SiteLocation(String residue, int start, int end) {
        setResidue(residue);
        setStart(start);
        setEnd(end);
    }

    /**
     * @return the persistence unique identifier for this object.
     */
    @XmlTransient
    public Long getId() {
        return null;
    }

    /**
     * @param id being the persistence unique identifier for this object.
     */
    private void setId(Long id) {
    }

    /**
     * Returns the start coordinate of this Location.
     *
     * @return the start coordinate of this Location.
     */
    @XmlAttribute(required = true)
    public int getStart() {
        return start;
    }

    /**
     * Start coordinate of this Location.
     *
     * @param start Start coordinate of this Location
     */
    private void setStart(int start) {
        this.start = start;
    }

    /**
     * Returns the end coordinate of this Location.
     *
     * @return the end coordinate of this Location.
     */
    @XmlAttribute(required = true)
    public int getEnd() {
        return end;
    }

    /**
     * End coordinate of this Location.
     *
     * @param end End coordinate of this Location.
     */
    private void setEnd(int end) {
        this.end = end;
    }

    /**
     * Returns the residue of this Location.
     *
     * @return the residue of this Location.
     */
    @XmlAttribute(required = true)
    public String getResidue() {
        return residue;
    }

    /**
     * Residue of this Location.
     *
     * @param residue Residue of this Location.
     */
    private void setResidue(String residue) {
        this.residue = residue;
    }


    /**
     * This method is called by Site, upon the addition of a residue location to a site.
     *
     * @param site to which this residue location is related.
     */
    void setSite(Site site) {
        this.site = site;
    }

    /**
     * Returns the Match that this Location is related to.
     *
     * @return
     */
    @XmlTransient
    public Site getSite() {
        return site;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SiteLocation))
            return false;
        final SiteLocation h = (SiteLocation) o;
        return new EqualsBuilder()
                .append(residue, h.residue)
                .append(start, h.start)
                .append(end, h.end)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(29, 57)
                .append(residue)
                .append(start)
                .append(end)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public Object clone() throws CloneNotSupportedException {
        return new SiteLocation(this.getResidue(), this.getStart(), this.getEnd());
    }

}
