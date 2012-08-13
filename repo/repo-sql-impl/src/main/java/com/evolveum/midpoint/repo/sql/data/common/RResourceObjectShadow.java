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
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryEntity;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.namespace.QName;


/**
 * @author lazyman
 */
@Entity
@Table(name = "m_resource_shadow")
@org.hibernate.annotations.Table(appliesTo = "m_resource_shadow",
        indexes = {@Index(name = "iResourceObjectShadowEnabled", columnNames = "enabled")})
@ForeignKey(name = "fk_resource_object_shadow")
public class RResourceObjectShadow extends RObject {

    private static final Trace LOGGER = TraceManager.getTrace(RResourceObjectShadow.class);
    @QueryAttribute
    private String name;
    private QName objectClass;
    private RActivation activation;
    private ROperationResult result;
    @QueryAttribute
    private RObjectReference resourceRef;
    private String objectChange;
    private Integer attemptNumber;
    private RFailedOperationTypeType failedOperationType;
    private String intent;
    //attributes
    @QueryEntity(any = true)
    private RAnyContainer attributes;

    @Columns(columns = {
            @Column(name = "class_namespace"),
            @Column(name = "class_localPart")
    })
    public QName getObjectClass() {
        return objectClass;
    }

    @OneToOne(optional = true, orphanRemoval = true)
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

    @Embedded
    public RActivation getActivation() {
        return activation;
    }

    @OneToOne(optional = true, mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public ROperationResult getResult() {
        return result;
    }

    @OneToOne(optional = true, mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReference getResourceRef() {
        return resourceRef;
    }

    @Column(nullable = true)
    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RFailedOperationTypeType getFailedOperationType() {
        return failedOperationType;
    }

    @Type(type = "org.hibernate.type.TextType")
    @Column(nullable = true)
    public String getObjectChange() {
        return objectChange;
    }

    @Index(name = "iResourceShadowName")
    @Column(name = "objectName")
    public String getName() {
        return name;
    }
    
    public String getIntent() {
		return intent;
	}

    public void setName(String name) {
        this.name = name;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void setFailedOperationType(RFailedOperationTypeType failedOperationType) {
        this.failedOperationType = failedOperationType;
    }

    public void setObjectChange(String objectChange) {
        this.objectChange = objectChange;
    }

    public void setResourceRef(RObjectReference resourceRef) {
        this.resourceRef = resourceRef;
    }

    public void setResult(ROperationResult result) {
        this.result = result;
    }

    public void setActivation(RActivation activation) {
        this.activation = activation;
    }

    public void setAttributes(RAnyContainer attributes) {
        this.attributes = attributes;
        if (this.attributes != null) {
            this.attributes.setOwnerType(RContainerType.RESOURCE_OBJECT_SHADOW);
        }
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }
    
    public void setIntent(String intent) {
		this.intent = intent;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RResourceObjectShadow that = (RResourceObjectShadow) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (activation != null ? !activation.equals(that.activation) : that.activation != null) return false;
        if (attemptNumber != null ? !attemptNumber.equals(that.attemptNumber) : that.attemptNumber != null)
            return false;
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        if (failedOperationType != that.failedOperationType) return false;
        if (objectChange != null ? !objectChange.equals(that.objectChange) : that.objectChange != null) return false;
        if (objectClass != null ? !objectClass.equals(that.objectClass) : that.objectClass != null) return false;
        if (resourceRef != null ? !resourceRef.equals(that.resourceRef) : that.resourceRef != null) return false;
        if (result != null ? !result.equals(that.result) : that.result != null) return false;
        if (intent != null ? !intent.equals(that.intent) : that.intent != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (name != null ? name.hashCode() : 0);
        result1 = 31 * result1 + (objectClass != null ? objectClass.hashCode() : 0);
        result1 = 31 * result1 + (activation != null ? activation.hashCode() : 0);
        result1 = 31 * result1 + (objectChange != null ? objectChange.hashCode() : 0);
        result1 = 31 * result1 + (attemptNumber != null ? attemptNumber.hashCode() : 0);
        result1 = 31 * result1 + (failedOperationType != null ? failedOperationType.hashCode() : 0);
        result1 = 31 * result1 + (intent != null ? intent.hashCode() : 0);
        return result1;
    }

    public static void copyToJAXB(RResourceObjectShadow repo, ResourceObjectShadowType jaxb,
            PrismContext prismContext) throws DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(repo.getName());
        jaxb.setObjectClass(repo.getObjectClass());
        jaxb.setIntent(repo.getIntent());
        if (repo.getActivation() != null) {
            jaxb.setActivation(repo.getActivation().toJAXB(prismContext));
        }

        if (repo.getResult() != null) {
            jaxb.setResult(repo.getResult().toJAXB(prismContext));
        }

        if (repo.getResourceRef() != null) {
            jaxb.setResourceRef(repo.getResourceRef().toJAXB(prismContext));
        }

        jaxb.setAttemptNumber(repo.getAttemptNumber());
        if (repo.getFailedOperationType() != null) {
            jaxb.setFailedOperationType(repo.getFailedOperationType().getOperation());
        }

        try {
            jaxb.setObjectChange(RUtil.toJAXB(repo.getObjectChange(), ObjectDeltaType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        if (repo.getAttributes() != null) {
            ResourceObjectShadowAttributesType attributes = new ResourceObjectShadowAttributesType();
            jaxb.setAttributes(attributes);
            RAnyContainer.copyToJAXB(repo.getAttributes(), attributes, prismContext);
        }
    }

    public static void copyFromJAXB(ResourceObjectShadowType jaxb, RResourceObjectShadow repo,
            PrismContext prismContext) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(jaxb.getName());
        repo.setObjectClass(jaxb.getObjectClass());
        repo.setIntent(jaxb.getIntent());
        if (jaxb.getActivation() != null) {
            RActivation activation = new RActivation();
            RActivation.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
            repo.setActivation(activation);
        }

        if (jaxb.getResult() != null) {
            ROperationResult result = new ROperationResult();
            result.setOwner(repo);
            ROperationResult.copyFromJAXB(jaxb.getResult(), result, prismContext);
            repo.setResult(result);
        }

        repo.setResourceRef(RUtil.jaxbRefToRepo(jaxb.getResourceRef(), repo, prismContext));
        repo.setAttemptNumber(jaxb.getAttemptNumber());
        repo.setFailedOperationType(RFailedOperationTypeType.toRepoType(jaxb.getFailedOperationType()));

        if (jaxb.getResource() != null) {
            LOGGER.warn("Resource from resource object shadow type won't be saved. It should be " +
                    "translated to resource reference.");
        }

        try {
            repo.setObjectChange(RUtil.toRepo(jaxb.getObjectChange(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        if (jaxb.getAttributes() != null) {
            RAnyContainer attributes = new RAnyContainer();
            attributes.setOwner(repo);

            repo.setAttributes(attributes);
            RAnyContainer.copyFromJAXB(jaxb.getAttributes(), attributes, prismContext);
        }
    }

    @Override
    public ResourceObjectShadowType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ResourceObjectShadowType object = new ResourceObjectShadowType();
        RUtil.revive(object, prismContext);
        RResourceObjectShadow.copyToJAXB(this, object, prismContext);

        return object;
    }
}
