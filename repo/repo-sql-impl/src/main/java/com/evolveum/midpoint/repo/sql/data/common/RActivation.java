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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Index;

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

    @Index(name = "iEnabled")
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

    public static void copyFromJAXB(ActivationType jaxb, RActivation repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setEnabled(jaxb.isEnabled());
        repo.setValidFrom(jaxb.getValidFrom());
        repo.setValidTo(repo.getValidTo());
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
