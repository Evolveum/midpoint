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
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSynchronizationSituationDescription;
import com.evolveum.midpoint.repo.sql.data.common.enums.RFailedOperationTypeType;
import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryEntity;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author lazyman
 */
@Entity
@Table(name = "m_resource_shadow")
@org.hibernate.annotations.Table(appliesTo = "m_resource_shadow",
        indexes = {@Index(name = "iResourceObjectShadowEnabled", columnNames = "enabled"),
                @Index(name = "iResourceShadowName", columnNames = "name_norm"),
                @Index(name = "iShadowResourceRef", columnNames = "resourceRef_targetOid")})
@ForeignKey(name = "fk_resource_object_shadow")
public class RResourceObjectShadow extends RObject {

    private static final Trace LOGGER = TraceManager.getTrace(RResourceObjectShadow.class);
    @QueryAttribute(polyString = true)
    private RPolyString name;
    @QueryAttribute
    private QName objectClass;
    private RActivation activation;
    private ROperationResult result;
    @QueryAttribute(reference = true)
    private REmbeddedReference resourceRef;
    private String objectChange;
    private Integer attemptNumber;
    @QueryAttribute
    private Boolean dead;
    @QueryAttribute(enumerated = true)
    private RFailedOperationTypeType failedOperationType;
    private String intent;
    @QueryAttribute(enumerated = true)
    private RSynchronizationSituation synchronizationSituation;
    private Set<RSynchronizationSituationDescription> synchronizationSituationDescription;
    //attributes
    @QueryEntity(any = true)
    private RAnyContainer attributes;
    @QueryAttribute
    private XMLGregorianCalendar synchronizationTimestamp;

    @Columns(columns = {
            @Column(name = "class_namespace"),
            @Column(name = "class_localPart", length = RUtil.COLUMN_LENGTH_LOCALPART)
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

    @Embedded
    public REmbeddedReference getResourceRef() {
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

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Column(nullable = true)
    public String getObjectChange() {
        return objectChange;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    @ElementCollection
    @ForeignKey(name = "fk_shadow_sync_situation")
    @CollectionTable(name = "m_sync_situation_description", joinColumns = {
            @JoinColumn(name = "shadow_oid", referencedColumnName = "oid"),
            @JoinColumn(name = "shadow_id", referencedColumnName = "id")
    })
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RSynchronizationSituationDescription> getSynchronizationSituationDescription() {
        if (synchronizationSituationDescription == null) {
            synchronizationSituationDescription = new HashSet<RSynchronizationSituationDescription>();
        }
        return synchronizationSituationDescription;
    }

    @Enumerated(EnumType.ORDINAL)
    public RSynchronizationSituation getSynchronizationSituation() {
        return synchronizationSituation;
    }

    public String getIntent() {
        return intent;
    }

    public XMLGregorianCalendar getSynchronizationTimestamp() {
        return synchronizationTimestamp;
    }

    public void setSynchronizationTimestamp(XMLGregorianCalendar synchronizationTimestamp) {
        this.synchronizationTimestamp = synchronizationTimestamp;
    }

    public void setName(RPolyString name) {
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

    public void setResourceRef(REmbeddedReference resourceRef) {
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

    public void setSynchronizationSituation(RSynchronizationSituation synchronizationSituation) {
        this.synchronizationSituation = synchronizationSituation;
    }

    public Boolean isDead() {
        return dead;
    }

    public void setDead(Boolean dead) {
        this.dead = dead;
    }

    public void setSynchronizationSituationDescription(
            Set<RSynchronizationSituationDescription> synchronizationSituationDescription) {
        this.synchronizationSituationDescription = synchronizationSituationDescription;
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
        if (synchronizationSituation != null ? !synchronizationSituation.equals(that.synchronizationSituation) : that.synchronizationSituation != null)
            return false;
        if (synchronizationSituationDescription != null ? !synchronizationSituationDescription.equals(that.synchronizationSituationDescription) : that.synchronizationSituationDescription != null)
            return false;

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
        result1 = 31 * result1 + (synchronizationSituation != null ? synchronizationSituation.hashCode() : 0);
        result1 = 31 * result1 + (synchronizationSituationDescription != null ? synchronizationSituationDescription.hashCode() : 0);
        return result1;
    }

    public static void copyToJAXB(RResourceObjectShadow repo, ResourceObjectShadowType jaxb,
                                  PrismContext prismContext) throws DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
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

        if (repo.getSynchronizationSituation() != null) {
            jaxb.setSynchronizationSituation(repo.getSynchronizationSituation().getSyncType());
        }

        jaxb.setDead(repo.isDead());

        List situations = RUtil.safeSetSyncSituationToList(repo.getSynchronizationSituationDescription());
        if (!situations.isEmpty()) {
            jaxb.getSynchronizationSituationDescription().addAll(situations);
        }

        jaxb.setSynchronizationTimestamp(repo.getSynchronizationTimestamp());

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

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
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

        if (jaxb.getSynchronizationSituation() != null) {
            repo.setSynchronizationSituation(RSynchronizationSituation.toRepoType(jaxb.getSynchronizationSituation()));
        }

        repo.getSynchronizationSituationDescription()
                .addAll(RUtil.listSyncSituationToSet(jaxb.getSynchronizationSituationDescription()));
        repo.setSynchronizationTimestamp(jaxb.getSynchronizationTimestamp());
        repo.setResourceRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getResourceRef(), prismContext));

        repo.setAttemptNumber(jaxb.getAttemptNumber());
        repo.setDead(jaxb.isDead());
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
