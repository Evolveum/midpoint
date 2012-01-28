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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "resource_object_shadow")
public class RResourceObjectShadowType extends RExtensibleObjectType {

    //TODO MAPPINGGGGGGGGGGGGGGGGGGG
//    private RObjectReferenceType resourceRef;
//    private RResourceType resource;
    //    private ROperationResultType result; //todo: probably not necessary
//    private ObjectChangeType objectChange;
    private Integer attemptNumber;   //todo default value
    //    private FailedOperationTypeType failedOperationType;
    private QName objectClass;
    private Set<RAttribute> attributes; //private ResourceObjectShadowType.Attributes attributes;

    @OneToMany
    @JoinColumn(name = "objectShadowId")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<RAttribute> attributes) {
        this.attributes = attributes;
    }

//    @ManyToOne
//    @JoinColumn
//    @Cascade({org.hibernate.annotations.CascadeType.ALL})
//    public RResourceType getResource() {
//        return resource;
//    }

//    public ResourceObjectShadowType.Attributes getAttributes() {
//        return attributes;
//    }
//
//    public FailedOperationTypeType getFailedOperationType() {
//        return failedOperationType;
//    }
//
//    public ObjectChangeType getObjectChange() {
//        return objectChange;
//    }

    @Columns(columns = {
            @Column(name = "namespaceURI"),
            @Column(name = "localPart")
    })
    public QName getObjectClass() {
        return objectClass;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

//    public RObjectReferenceType getResourceRef() {
//        return resourceRef;
//    }
//
//    public ROperationResultType getResult() {
//        return result;
//    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

//    public void setAttributes(ResourceObjectShadowType.Attributes attributes) {
//        this.attributes = attributes;
//    }
//
//    public void setFailedOperationType(FailedOperationTypeType failedOperationType) {
//        this.failedOperationType = failedOperationType;
//    }
//
//    public void setObjectChange(ObjectChangeType objectChange) {
//        this.objectChange = objectChange;
//    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }

//    public void setResource(RResourceType resource) {
//        this.resource = resource;
//    }

//    public void setResourceRef(RObjectReferenceType resourceRef) {
//        this.resourceRef = resourceRef;
//    }
//
//    public void setResult(ROperationResultType result) {
//        this.result = result;
//    }

    public static void copyToJAXB(RResourceObjectShadowType repo, ResourceObjectShadowType jaxb) throws
            DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        //todo implement
    }

    public static void copyFromJAXB(ResourceObjectShadowType jaxb, RResourceObjectShadowType repo) throws
            DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        //todo implement
    }

    @Override
    public ResourceObjectShadowType toJAXB() throws DtoTranslationException {
        ResourceObjectShadowType object = new ResourceObjectShadowType();
        RResourceObjectShadowType.copyToJAXB(this, object);
        return object;
    }
}
