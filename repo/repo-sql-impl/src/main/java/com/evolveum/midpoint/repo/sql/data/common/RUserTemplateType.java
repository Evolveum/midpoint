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

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "user_template")
public class RUserTemplateType extends RExtensibleObjectType {

//    private List<PropertyConstructionType> propertyConstruction;   //todo mapping
//    private List<AccountConstructionType> accountConstruction;   //todo mapping


    public static void copyToJAXB(RUserTemplateType repo, UserTemplateType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        //todo implement
    }

    public static void copyFromJAXB(UserTemplateType jaxb, RUserTemplateType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        //todo implement
    }

    @Override
    public UserTemplateType toJAXB() throws DtoTranslationException {
        UserTemplateType object = new UserTemplateType();
        RUserTemplateType.copyToJAXB(this, object);
        return object;
    }
}
