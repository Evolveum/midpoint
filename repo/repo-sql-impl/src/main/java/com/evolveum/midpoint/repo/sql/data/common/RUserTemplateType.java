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

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "user_template")
public class RUserTemplateType extends RExtensibleObjectType {

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

    public static void copyToJAXB(RUserTemplateType repo, UserTemplateType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        try {
//            if (StringUtils.isNotEmpty(repo.getAccountConstruction())) {
//                JAXBElement<UserTemplateType> element = (JAXBElement<UserTemplateType>)
//                        JAXBUtil.unmarshal(repo.getAccountConstruction());
//                jaxb.getAccountConstruction().addAll(element.getValue().getAccountConstruction());
//            }
//
//            if (StringUtils.isNotEmpty(repo.getPropertyConstruction())) {
//                JAXBElement<UserTemplateType> element = (JAXBElement<UserTemplateType>)
//                        JAXBUtil.unmarshal(repo.getPropertyConstruction());
//                jaxb.getPropertyConstruction().addAll(element.getValue().getPropertyConstruction());
//            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(UserTemplateType jaxb, RUserTemplateType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        try {
            if (!jaxb.getAccountConstruction().isEmpty()) {
                UserTemplateType template = new UserTemplateType();
                template.getAccountConstruction().addAll(jaxb.getAccountConstruction());
                repo.setAccountConstruction(RUtil.toRepo(template));
            }

            if (!jaxb.getPropertyConstruction().isEmpty()) {
                UserTemplateType template = new UserTemplateType();
                template.getPropertyConstruction().addAll(jaxb.getPropertyConstruction());
                repo.setPropertyConstruction(RUtil.toRepo(template));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public UserTemplateType toJAXB() throws DtoTranslationException {
        UserTemplateType object = new UserTemplateType();
        RUserTemplateType.copyToJAXB(this, object);
        return object;
    }
}
