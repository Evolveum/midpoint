/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.RTimeIntervalStatus;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
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

        return true;
    }

    @Override
    public int hashCode() {
        int result = validFrom != null ? validFrom.hashCode() : 0;
        result = 31 * result + (validTo != null ? validTo.hashCode() : 0);
        result = 31 * result + (administrativeStatus != null ? administrativeStatus.hashCode() : 0);
        result = 31 * result + (effectiveStatus != null ? effectiveStatus.hashCode() : 0);
        result = 31 * result + (validityStatus != null ? validityStatus.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyFromJAXB(ActivationType jaxb, RActivation repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setAdministrativeStatus(RUtil.getRepoEnumValue(jaxb.getAdministrativeStatus(), RActivationStatus.class));
        repo.setEffectiveStatus(RUtil.getRepoEnumValue(jaxb.getEffectiveStatus(), RActivationStatus.class));
        repo.setValidityStatus(RUtil.getRepoEnumValue(jaxb.getValidityStatus(), RTimeIntervalStatus.class));
        repo.setValidFrom(jaxb.getValidFrom());
        repo.setValidTo(jaxb.getValidTo());
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
    }

    public ActivationType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ActivationType activation = new ActivationType();
        RActivation.copyToJAXB(this, activation, prismContext);
        return activation;
    }
}
