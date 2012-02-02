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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "resource_object_shadow")
public class RResourceObjectShadowType extends RExtensibleObjectType {

    private static final Trace LOGGER = TraceManager.getTrace(RResourceObjectShadowType.class);
    private RObjectReferenceType resourceRef;
    private ROperationResultType opResult;
    private String objectChange;
    private Integer attemptNumber;
    private FailedOperationTypeType failedOperationType;
    private QName objectClass;
    private Set<RValue> attributes; //private ResourceObjectShadowType.Attributes attributes;

    @Type(type = "org.hibernate.type.TextType")
    public String getObjectChange() {
        return objectChange;
    }

    @OneToMany
    @JoinColumn(name = "objectShadowId")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RValue> getAttributes() {
        return attributes;
    }

    @Enumerated(EnumType.ORDINAL)
    public FailedOperationTypeType getFailedOperationType() {
        return failedOperationType;
    }

    @Columns(columns = {
            @Column(name = "namespaceURI"),
            @Column(name = "localPart")
    })
    public QName getObjectClass() {
        return objectClass;
    }

    @Column(nullable = true)
    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    @ManyToOne
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReferenceType getResourceRef() {
        return resourceRef;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public ROperationResultType getOpResult() {
        return opResult;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void setFailedOperationType(FailedOperationTypeType failedOperationType) {
        this.failedOperationType = failedOperationType;
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }

    public void setResourceRef(RObjectReferenceType resourceRef) {
        this.resourceRef = resourceRef;
    }

    public void setOpResult(ROperationResultType opResult) {
        this.opResult = opResult;
    }

    public void setAttributes(Set<RValue> attributes) {
        this.attributes = attributes;
    }

    public void setObjectChange(String objectChange) {
        this.objectChange = objectChange;
    }

    public static void copyToJAXB(RResourceObjectShadowType repo, ResourceObjectShadowType jaxb) throws
            DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        jaxb.setAttemptNumber(repo.getAttemptNumber());
        jaxb.setObjectClass(repo.getObjectClass());
        jaxb.setFailedOperationType(repo.getFailedOperationType());

        if (repo.getOpResult() != null) {
            jaxb.setResult(repo.getOpResult().toJAXB());
        }
        if (repo.getResourceRef() != null) {
            jaxb.setResourceRef(repo.getResourceRef().toJAXB());
        }

        try {
            jaxb.setObjectChange(RUtil.toJAXB(repo.getObjectChange(), ObjectChangeType.class));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        //todo implement attributes
    }

    public static void copyFromJAXB(ResourceObjectShadowType jaxb, RResourceObjectShadowType repo) throws
            DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        if (jaxb.getResource() != null) {
            LOGGER.warn("Resource from resource object shadow type won't be saved. It should be " +
                    "translated to resource reference.");
        }

        repo.setAttemptNumber(jaxb.getAttemptNumber());
        repo.setObjectClass(jaxb.getObjectClass());
        repo.setFailedOperationType(jaxb.getFailedOperationType());

        repo.setResourceRef(RUtil.jaxbRefToRepo(jaxb.getResourceRef()));
        repo.setOpResult(RUtil.jaxbResultToRepo(repo, jaxb.getResult()));

        try {
            repo.setObjectChange(RUtil.toRepo(jaxb.getObjectChange()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        //todo implement attributes
    }

    @Override
    public ResourceObjectShadowType toJAXB() throws DtoTranslationException {
        ResourceObjectShadowType object = new ResourceObjectShadowType();
        RResourceObjectShadowType.copyToJAXB(this, object);
        return object;
    }
}
