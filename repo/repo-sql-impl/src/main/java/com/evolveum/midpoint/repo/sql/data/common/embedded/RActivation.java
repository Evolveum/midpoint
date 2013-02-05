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
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author lazyman
 */
@Embeddable
public class RActivation {

    @QueryAttribute
    private Boolean enabled;
    @QueryAttribute
    private XMLGregorianCalendar validFrom;
    @QueryAttribute
    private XMLGregorianCalendar validTo;

    @Column(nullable = true)
    public Boolean isEnabled() {
        return enabled;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getValidTo() {
        return validTo;
    }

    @Column(nullable = true)
    public XMLGregorianCalendar getValidFrom() {
        return validFrom;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

        if (enabled != null ? !enabled.equals(that.enabled) : that.enabled != null) return false;
        if (validFrom != null ? !validFrom.equals(that.validFrom) : that.validFrom != null) return false;
        if (validTo != null ? !validTo.equals(that.validTo) : that.validTo != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = enabled != null ? enabled.hashCode() : 0;
        result = 31 * result + (validFrom != null ? validFrom.hashCode() : 0);
        result = 31 * result + (validTo != null ? validTo.hashCode() : 0);
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

        repo.setEnabled(jaxb.isEnabled());
        repo.setValidFrom(jaxb.getValidFrom());
        repo.setValidTo(jaxb.getValidTo());
    }

    public static void copyToJAXB(RActivation repo, ActivationType jaxb, PrismContext prismContext) {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        jaxb.setEnabled(repo.isEnabled());
        jaxb.setValidFrom(repo.getValidFrom());
        jaxb.setValidTo(repo.getValidTo());
    }

    public ActivationType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ActivationType activation = new ActivationType();
        RActivation.copyToJAXB(this, activation, prismContext);
        return activation;
    }
}
