/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.RTimeIntervalStatus;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author lazyman
 */
@Embeddable
@JaxbType(type = ActivationType.class)
public class RActivation {

    private RActivationStatus administrativeStatus;
    private RActivationStatus effectiveStatus;
    private XMLGregorianCalendar validFrom;
    private XMLGregorianCalendar validTo;
    private RTimeIntervalStatus validityStatus;
    private XMLGregorianCalendar disableTimestamp;
    private XMLGregorianCalendar enableTimestamp;
    private XMLGregorianCalendar archiveTimestamp;
    private XMLGregorianCalendar validityChangeTimestamp;
    private String disableReason;

    @Column(nullable = true)
    public XMLGregorianCalendar getArchiveTimestamp() {
        return archiveTimestamp;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getDisableTimestamp() {
        return disableTimestamp;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getEnableTimestamp() {
        return enableTimestamp;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getValidityChangeTimestamp() {
        return validityChangeTimestamp;
    }

    @Column(nullable = true)
    @Enumerated(EnumType.ORDINAL)
    public RTimeIntervalStatus getValidityStatus() {
        return validityStatus;
    }

    @Column(nullable = true)
    @Enumerated(EnumType.ORDINAL)
    public RActivationStatus getAdministrativeStatus() {
        return administrativeStatus;
    }

    @Column(nullable = true)
    @Enumerated(EnumType.ORDINAL)
    public RActivationStatus getEffectiveStatus() {
        return effectiveStatus;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getValidTo() {
        return validTo;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getValidFrom() {
        return validFrom;
    }

    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    public void setValidityStatus(RTimeIntervalStatus validityStatus) {
        this.validityStatus = validityStatus;
    }

    public void setAdministrativeStatus(RActivationStatus administrativeStatus) {
        this.administrativeStatus = administrativeStatus;
    }

    public void setEffectiveStatus(RActivationStatus effectiveStatus) {
        this.effectiveStatus = effectiveStatus;
    }

    public void setValidFrom(XMLGregorianCalendar validFrom) {
        this.validFrom = validFrom;
    }

    public void setValidTo(XMLGregorianCalendar validTo) {
        this.validTo = validTo;
    }

    public void setArchiveTimestamp(XMLGregorianCalendar archiveTimestamp) {
        this.archiveTimestamp = archiveTimestamp;
    }

    public void setDisableTimestamp(XMLGregorianCalendar disableTimestamp) {
        this.disableTimestamp = disableTimestamp;
    }

    public void setEnableTimestamp(XMLGregorianCalendar enableTimestamp) {
        this.enableTimestamp = enableTimestamp;
    }

    public void setValidityChangeTimestamp(XMLGregorianCalendar validityChangeTimestamp) {
        this.validityChangeTimestamp = validityChangeTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RActivation that = (RActivation) o;

        if (validFrom != null ? !validFrom.equals(that.validFrom) : that.validFrom != null) return false;
        if (validTo != null ? !validTo.equals(that.validTo) : that.validTo != null) return false;
        if (administrativeStatus != null ? !administrativeStatus.equals(that.administrativeStatus) :
                that.administrativeStatus != null) return false;
        if (effectiveStatus != null ? !effectiveStatus.equals(that.effectiveStatus) :
                that.effectiveStatus != null) return false;
        if (validityStatus != null ? !validityStatus.equals(that.validityStatus) :
                that.validityStatus != null) return false;
        if (archiveTimestamp != null ? !archiveTimestamp.equals(that.archiveTimestamp) :
                that.archiveTimestamp != null) return false;
        if (disableTimestamp != null ? !disableTimestamp.equals(that.disableTimestamp) :
                that.disableTimestamp != null) return false;
        if (enableTimestamp != null ? !enableTimestamp.equals(that.enableTimestamp) :
                that.enableTimestamp != null) return false;
        if (validityChangeTimestamp != null ? !validityChangeTimestamp.equals(that.validityChangeTimestamp) :
                that.validityChangeTimestamp != null) return false;
        if (disableReason != null ? !disableReason.equals(that.disableReason) :
                that.disableReason != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = validFrom != null ? validFrom.hashCode() : 0;
        result = 31 * result + (validTo != null ? validTo.hashCode() : 0);
        result = 31 * result + (administrativeStatus != null ? administrativeStatus.hashCode() : 0);
        result = 31 * result + (effectiveStatus != null ? effectiveStatus.hashCode() : 0);
        result = 31 * result + (validityStatus != null ? validityStatus.hashCode() : 0);
        result = 31 * result + (archiveTimestamp != null ? archiveTimestamp.hashCode() : 0);
        result = 31 * result + (disableTimestamp != null ? disableTimestamp.hashCode() : 0);
        result = 31 * result + (enableTimestamp != null ? enableTimestamp.hashCode() : 0);
        result = 31 * result + (validityChangeTimestamp != null ? validityChangeTimestamp.hashCode() : 0);
        result = 31 * result + (disableReason != null ? disableReason.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyFromJAXB(ActivationType jaxb, RActivation repo, RepositoryContext repositoryContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setAdministrativeStatus(RUtil.getRepoEnumValue(jaxb.getAdministrativeStatus(), RActivationStatus.class));
        repo.setEffectiveStatus(RUtil.getRepoEnumValue(jaxb.getEffectiveStatus(), RActivationStatus.class));
        repo.setValidityStatus(RUtil.getRepoEnumValue(jaxb.getValidityStatus(), RTimeIntervalStatus.class));
        repo.setValidFrom(jaxb.getValidFrom());
        repo.setValidTo(jaxb.getValidTo());
        repo.setDisableReason(jaxb.getDisableReason());

        repo.setArchiveTimestamp(jaxb.getArchiveTimestamp());
        repo.setDisableTimestamp(jaxb.getDisableTimestamp());
        repo.setEnableTimestamp(jaxb.getEnableTimestamp());
        repo.setValidityChangeTimestamp(jaxb.getValidityChangeTimestamp());
    }

    public static void copyToJAXB(RActivation repo, ActivationType jaxb, PrismContext prismContext) {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        if (repo.getAdministrativeStatus() != null) {
            jaxb.setAdministrativeStatus(repo.getAdministrativeStatus().getSchemaValue());
        }
        if (repo.getEffectiveStatus() != null) {
            jaxb.setEffectiveStatus(repo.getEffectiveStatus().getSchemaValue());
        }
        if (repo.getValidityStatus() != null) {
            jaxb.setValidityStatus(repo.getValidityStatus().getSchemaValue());
        }
        jaxb.setValidFrom(repo.getValidFrom());
        jaxb.setValidTo(repo.getValidTo());
        jaxb.setDisableReason(repo.getDisableReason());

        jaxb.setArchiveTimestamp(repo.getArchiveTimestamp());
        jaxb.setDisableTimestamp(repo.getDisableTimestamp());
        jaxb.setEnableTimestamp(repo.getEnableTimestamp());
        jaxb.setValidityChangeTimestamp(repo.getValidityChangeTimestamp());
    }

    public ActivationType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ActivationType activation = new ActivationType();
        RActivation.copyToJAXB(this, activation, prismContext);
        return activation;
    }
}
