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
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "generic_object")
@ForeignKey(name = "fk_generic_object")
public class RGenericObject extends RObject {

    private String objectType;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public static void copyToJAXB(RGenericObject repo, GenericObjectType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setObjectType(repo.getObjectType());
    }

    public static void copyFromJAXB(GenericObjectType jaxb, RGenericObject repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setObjectType(jaxb.getObjectType());
    }

    @Override
    public GenericObjectType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        GenericObjectType object = new GenericObjectType();
        RUtil.revive(object.asPrismObject(), GenericObjectType.class, prismContext);
        RGenericObject.copyToJAXB(this, object, prismContext);

        return object;
    }
}                                                    
