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
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "user_template")
@ForeignKey(name = "fk_user_template")
public class RUserTemplate extends RObject {

    private String propertyConstruction;
    private String accountConstruction;

    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = true)
    public String getAccountConstruction() {
        return accountConstruction;
    }

    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = true)
    public String getPropertyConstruction() {
        return propertyConstruction;
    }

    public void setAccountConstruction(String accountConstruction) {
        this.accountConstruction = accountConstruction;
    }

    public void setPropertyConstruction(String propertyConstruction) {
        this.propertyConstruction = propertyConstruction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RUserTemplate that = (RUserTemplate) o;

        if (accountConstruction != null ? !accountConstruction.equals(that.accountConstruction) : that.accountConstruction != null)
            return false;
        if (propertyConstruction != null ? !propertyConstruction.equals(that.propertyConstruction) : that.propertyConstruction != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (propertyConstruction != null ? propertyConstruction.hashCode() : 0);
        result = 31 * result + (accountConstruction != null ? accountConstruction.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RUserTemplate repo, UserTemplateType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        try {
            if (StringUtils.isNotEmpty(repo.getAccountConstruction())) {
                UserTemplateType holder = RUtil.toJAXB(repo.getAccountConstruction(), UserTemplateType.class, prismContext);
                jaxb.getAccountConstruction().addAll(holder.getAccountConstruction());
            }

            if (StringUtils.isNotEmpty(repo.getPropertyConstruction())) {
                UserTemplateType holder = RUtil.toJAXB(repo.getPropertyConstruction(), UserTemplateType.class, prismContext);
                jaxb.getPropertyConstruction().addAll(holder.getPropertyConstruction());
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(UserTemplateType jaxb, RUserTemplate repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        try {
            if (!jaxb.getAccountConstruction().isEmpty()) {
                UserTemplateType template = new UserTemplateType();
                template.getAccountConstruction().addAll(jaxb.getAccountConstruction());
                repo.setAccountConstruction(RUtil.toRepo(template, prismContext));
            }

            if (!jaxb.getPropertyConstruction().isEmpty()) {
                UserTemplateType template = new UserTemplateType();
                template.getPropertyConstruction().addAll(jaxb.getPropertyConstruction());
                repo.setPropertyConstruction(RUtil.toRepo(template, prismContext));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public UserTemplateType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        UserTemplateType object = new UserTemplateType();
        RUtil.revive(object, prismContext);
        RUserTemplate.copyToJAXB(this, object, prismContext);

        return object;
    }
}
