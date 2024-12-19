/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.embedded;

import java.util.Objects;
import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.RTimeIntervalStatus;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@JaxbType(type = ActivationType.class)
@MappedSuperclass
public abstract class RActivation {

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

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getArchiveTimestamp() {
        return archiveTimestamp;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getDisableTimestamp() {
        return disableTimestamp;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getEnableTimestamp() {
        return enableTimestamp;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getValidityChangeTimestamp() {
        return validityChangeTimestamp;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RTimeIntervalStatus getValidityStatus() {
        return validityStatus;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RActivationStatus getAdministrativeStatus() {
        return administrativeStatus;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RActivationStatus getEffectiveStatus() {
        return effectiveStatus;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getValidTo() {
        return validTo;
    }

    @Column
    @Type(XMLGregorianCalendarType.class)
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
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        RActivation that = (RActivation) o;

        if (validFrom != null ? !validFrom.equals(that.validFrom) : that.validFrom != null) { return false; }
        if (validTo != null ? !validTo.equals(that.validTo) : that.validTo != null) { return false; }
        if (administrativeStatus != null ? !administrativeStatus.equals(that.administrativeStatus) :
                that.administrativeStatus != null) { return false; }
        if (effectiveStatus != null ? !effectiveStatus.equals(that.effectiveStatus) :
                that.effectiveStatus != null) { return false; }
        if (validityStatus != null ? !validityStatus.equals(that.validityStatus) :
                that.validityStatus != null) { return false; }
        if (archiveTimestamp != null ? !archiveTimestamp.equals(that.archiveTimestamp) :
                that.archiveTimestamp != null) { return false; }
        if (disableTimestamp != null ? !disableTimestamp.equals(that.disableTimestamp) :
                that.disableTimestamp != null) { return false; }
        if (enableTimestamp != null ? !enableTimestamp.equals(that.enableTimestamp) :
                that.enableTimestamp != null) { return false; }
        if (validityChangeTimestamp != null ? !validityChangeTimestamp.equals(that.validityChangeTimestamp) :
                that.validityChangeTimestamp != null) { return false; }
        if (disableReason != null ? !disableReason.equals(that.disableReason) :
                that.disableReason != null) { return false; }

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

    public static void fromJaxb(ActivationType jaxb, RActivation repo)
            throws DtoTranslationException {
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        Objects.requireNonNull(repo, "Repo object must not be null.");

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
        Objects.requireNonNull(jaxb, "JAXB object must not be null.");
        Objects.requireNonNull(repo, "Repo object must not be null.");

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

    public ActivationType toJAXB(PrismContext prismContext) {
        ActivationType activation = new ActivationType();
        RActivation.copyToJAXB(this, activation, prismContext);
        return activation;
    }
}
