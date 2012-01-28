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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtoStructureType;

import javax.persistence.Entity;
import java.io.Serializable;
import java.util.List;

/**
 * @author lazyman
 */
//@Entity
//@Table(name = "proto_structure")
public abstract class RProtoStructureType extends RExtensibleObjectType {

    private List<Serializable> containedObjectOrContainedObjectRef;  //todo mapping


    public static void copyToJAXB(RProtoStructureType repo, ProtoStructureType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        //todo implement
    }

    public static void copyFromJAXB(ProtoStructureType jaxb, RProtoStructureType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        //todo implement
    }
}
