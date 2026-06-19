/*
 * Copyright 2011 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.xml.bind.annotation.*;
import java.io.Serializable;

/**
 * Represents an open reading frame.
 *
 * @author Antony Quinn
 * @author Phil Jones
 * @version $Id$
 */
@XmlRootElement(name = "orf")
@XmlType(name = "OrfType")
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
public class OpenReadingFrame implements Serializable {

    protected Long id;

    private int start;

    private int end;

    private NucleotideSequenceStrand strand;

    private Protein protein;

    private NucleotideSequence nucleotideSequence;

    /**
     * protected no-arg constructor required by JPA - DO NOT USE DIRECTLY
     */
    protected OpenReadingFrame() {
    }

    public OpenReadingFrame(int start, int end, NucleotideSequenceStrand strand) {
        this.start = start;
        this.end = end;
        this.strand = strand;
    }

    public Long getId() {
        return id;
    }

    @XmlAttribute(required = true)
    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    @XmlAttribute(required = true)
    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @XmlAttribute(required = true)
    public NucleotideSequenceStrand getStrand() {
        return strand;
    }

    public void setStrand(NucleotideSequenceStrand strand) {
        this.strand = strand;
    }

    @XmlTransient
    public NucleotideSequence getNucleotideSequence() {
        return nucleotideSequence;
    }

    public void setNucleotideSequence(NucleotideSequence nucleotideSequence) {
        this.nucleotideSequence = nucleotideSequence;
    }

    public void setProtein(Protein protein) {
        this.protein = protein;
    }

    @XmlElement(name = "protein")
    public Protein getProtein() {
        return protein;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OpenReadingFrame))
            return false;
        final OpenReadingFrame f = (OpenReadingFrame) o;
        return new EqualsBuilder()
                .append(start, f.start)
                .append(end, f.end)
                .append(strand, f.strand)
                .append(protein, f.protein)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(71, 113)
                .append(start)
                .append(end)
                .append(strand)
                .append(protein)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}