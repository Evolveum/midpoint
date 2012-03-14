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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import javax.xml.namespace.QName;


/**
 * @author lazyman
 */
@Entity
@Table(name = "resource_shadow")
@ForeignKey(name = "fk_resource_object_shadow")
public class RResourceObjectShadowType extends RObjectType {

    private QName objectClass;
    //attributes
    private RAnyContainer attributes;

    @Columns(columns = {
            @Column(name = "class_namespace"),
            @Column(name = "class_localPart")
    })
    public QName getObjectClass() {
        return objectClass;
    }

    @OneToOne(optional = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JoinColumns({
            @JoinColumn(name = "attrOid", referencedColumnName = "owner_oid"),
            @JoinColumn(name = "attrId", referencedColumnName = "owner_id"),
            @JoinColumn(name = "attrType", referencedColumnName = "ownerType")
    })

    public RAnyContainer getAttributes() {
        return attributes;
    }

    public void setAttributes(RAnyContainer attributes) {
        this.attributes = attributes;
        if (attributes != null) {
            attributes.setOwnerType(RContainerType.RESOURCE_OBJECT_SHADOW);
        }
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }

    public static void copyToJAXB(RResourceObjectShadowType repo, ResourceObjectShadowType jaxb,
            PrismContext prismContext) throws DtoTranslationException {
        RObjectType.copyToJAXB(repo, jaxb, prismContext);

//        jaxb.setAttemptNumber(repo.getAttemptNumber());
//        jaxb.setObjectClass(repo.getObjectClass());
//        jaxb.setFailedOperationType(repo.getFailedOperationType());
//
//        if (repo.getOpResult() != null) {
//            jaxb.setResult(repo.getOpResult().toJAXB(prismContext));
//        }
//        if (repo.getResourceRef() != null) {
//            jaxb.setResourceRef(repo.getResourceRef().toJAXB(prismContext));
//        }
//
//        try {
//            jaxb.setObjectChange(RUtil.toJAXB(repo.getObjectChange(), ObjectChangeType.class, prismContext));
//        } catch (Exception ex) {
//            throw new DtoTranslationException(ex.getMessage(), ex);
//        }

        //todo implement attributes
    }

    public static void copyFromJAXB(ResourceObjectShadowType jaxb, RResourceObjectShadowType repo,
            PrismContext prismContext) throws DtoTranslationException {
        RObjectType.copyFromJAXB(jaxb, repo, prismContext);

//        if (jaxb.getResource() != null) {
//            LOGGER.warn("Resource from resource object shadow type won't be saved. It should be " +
//                    "translated to resource reference.");
//        }
//
//        repo.setAttemptNumber(jaxb.getAttemptNumber());
//        repo.setObjectClass(jaxb.getObjectClass());
//        repo.setFailedOperationType(jaxb.getFailedOperationType());
//
//        repo.setResourceRef(RUtil.jaxbRefToRepo(jaxb.getResourceRef(), jaxb, prismContext));
//        repo.setOpResult(RUtil.jaxbResultToRepo(repo, jaxb.getResult(), prismContext));
//
//        try {
//            repo.setObjectChange(RUtil.toRepo(jaxb.getObjectChange(), prismContext));
//        } catch (Exception ex) {
//            throw new DtoTranslationException(ex.getMessage(), ex);
//        }

        //todo implement attributes
    }

    @Override
    public ResourceObjectShadowType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ResourceObjectShadowType object = new ResourceObjectShadowType();
        RResourceObjectShadowType.copyToJAXB(this, object, prismContext);
        RUtil.revive(object.asPrismObject(), ResourceObjectShadowType.class, prismContext);
        return object;
    }
}
