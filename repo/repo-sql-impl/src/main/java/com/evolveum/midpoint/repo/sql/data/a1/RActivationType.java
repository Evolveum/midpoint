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

package com.evolveum.midpoint.repo.sql.data.a1;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Index;

import javax.persistence.Embeddable;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author lazyman
 */
@Embeddable
public class RActivationType {

    private boolean enabled = true;
    private XMLGregorianCalendar validFrom;
    private XMLGregorianCalendar validTo;

    @Index(name = "iEnabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public XMLGregorianCalendar getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(XMLGregorianCalendar validFrom) {
        this.validFrom = validFrom;
    }

    public XMLGregorianCalendar getValidTo() {
        return validTo;
    }

    public void setValidTo(XMLGregorianCalendar validTo) {
        this.validTo = validTo;
    }

    public static void copyFromJAXB(ActivationType jaxb, RActivationType repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        boolean enabled = jaxb.isEnabled() != null ? jaxb.isEnabled() : true;
        repo.setEnabled(enabled);

        repo.setValidFrom(jaxb.getValidFrom());
        repo.setValidTo(repo.getValidTo());
    }

    public static void copyToJAXB(RActivationType repo, ActivationType jaxb, PrismContext prismContext) {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        jaxb.setEnabled(repo.isEnabled());
        jaxb.setValidFrom(repo.getValidFrom());
        jaxb.setValidTo(repo.getValidTo());
    }

    public ActivationType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ActivationType activation = new ActivationType();
        RActivationType.copyToJAXB(this, activation, prismContext);
        return activation;
    }
}
