/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RFailedOperationType;
import com.evolveum.midpoint.repo.sql.data.common.enums.RShadowKind;
import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.data.common.other.RContainerType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
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
@Table(name = "m_shadow")
@org.hibernate.annotations.Table(appliesTo = "m_shadow",
        indexes = {@Index(name = "iShadowAdministrative", columnNames = "administrativeStatus"),
                @Index(name = "iShadowEffective", columnNames = "effectiveStatus"),
                @Index(name = "iShadowName", columnNames = "name_norm"),
                @Index(name = "iShadowResourceRef", columnNames = "resourceRef_targetOid")})
@ForeignKey(name = "fk_shadow")
public class RShadow extends RObject {

    private static final Trace LOGGER = TraceManager.getTrace(RShadow.class);
    private RPolyString name;
    private QName objectClass;
    private RActivation activation;
    private ROperationResult result;
    private REmbeddedReference resourceRef;
    private String objectChange;
    private Integer attemptNumber;
    private Boolean dead;
    private RFailedOperationType failedOperationType;
    private String intent;
    private RSynchronizationSituation synchronizationSituation;
    private Set<RSynchronizationSituationDescription> synchronizationSituationDescription;
    //attributes
    private RAnyContainer attributes;
    private XMLGregorianCalendar synchronizationTimestamp;
    private RShadowKind kind;
    private Boolean assigned;
    private Boolean exists;

    public Boolean isAssigned() {
        return assigned;
    }

    @Column(name = "exist")
    public Boolean isExists() {
        return exists;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RShadowKind getKind() {
        return kind;
    }

    @Columns(columns = {
            @Column(name = "class_namespace"),
            @Column(name = "class_localPart", length = RUtil.COLUMN_LENGTH_LOCALPART)
    })
    public QName getObjectClass() {
        return objectClass;
    }

    @com.evolveum.midpoint.repo.sql.query.definition.Any(jaxbNameLocalPart = "attributes")
    @OneToOne(optional = true, orphanRemoval = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @JoinColumns({
            @JoinColumn(name = "attrOid", referencedColumnName = "owner_oid"),
            @JoinColumn(name = "attrId", referencedColumnName = "owner_id"),
            @JoinColumn(name = "attrType", referencedColumnName = "owner_type")
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
    public RFailedOperationType getFailedOperationType() {
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

    @ForeignKey(name = "fk_shadow_sync_situation")
    @OneToMany(mappedBy = "shadow", orphanRemoval = true)
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

    public Boolean isDead() {
        return dead;
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

    public void setFailedOperationType(RFailedOperationType failedOperationType) {
        this.failedOperationType = failedOperationType;
    }

    public void setObjectChange(String objectChange) {
        this.objectChange = objectChange;
    }

    public void setKind(RShadowKind kind) {
        this.kind = kind;
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
            this.attributes.setOwnerType(RContainerType.SHADOW);
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

    public void setDead(Boolean dead) {
        this.dead = dead;
    }

    public void setAssigned(Boolean assigned) {
        this.assigned = assigned;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
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

        RShadow that = (RShadow) o;

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
        if (kind != null ? !kind.equals(that.kind) : that.kind != null) return false;
        if (assigned != null ? !assigned.equals(that.assigned) : that.assigned != null) return false;
        if (exists != null ? !exists.equals(that.exists) : that.exists != null) return false;

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
        result1 = 31 * result1 + (kind != null ? kind.hashCode() : 0);
        result1 = 31 * result1 + (assigned != null ? assigned.hashCode() : 0);
        result1 = 31 * result1 + (exists != null ? exists.hashCode() : 0);

        return result1;
    }

    public static void copyToJAXB(RShadow repo, ShadowType jaxb,
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
            jaxb.setFailedOperationType(repo.getFailedOperationType().getSchemaValue());
        }

        if (repo.getSynchronizationSituation() != null) {
            jaxb.setSynchronizationSituation(repo.getSynchronizationSituation().getSchemaValue());
        }

        jaxb.setAssigned(repo.isAssigned());
        jaxb.setExists(repo.isExists());
        jaxb.setDead(repo.isDead());
        if (repo.getKind() != null) {
            jaxb.setKind(repo.getKind().getSchemaValue());
        }

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
            ShadowAttributesType attributes = new ShadowAttributesType();
            jaxb.setAttributes(attributes);
            RAnyContainer.copyToJAXB(repo.getAttributes(), attributes, prismContext);
        }
    }

    public static void copyFromJAXB(ShadowType jaxb, RShadow repo,
                                    PrismContext prismContext) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setObjectClass(jaxb.getObjectClass());
        repo.setIntent(jaxb.getIntent());
        repo.setKind(RUtil.getRepoEnumValue(jaxb.getKind(), RShadowKind.class));
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
            repo.setSynchronizationSituation(RUtil.getRepoEnumValue(jaxb.getSynchronizationSituation(),
                    RSynchronizationSituation.class));
        }

        repo.getSynchronizationSituationDescription()
                .addAll(RUtil.listSyncSituationToSet(repo, jaxb.getSynchronizationSituationDescription()));
        repo.setSynchronizationTimestamp(jaxb.getSynchronizationTimestamp());
        repo.setResourceRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getResourceRef(), prismContext));

        repo.setAttemptNumber(jaxb.getAttemptNumber());
        repo.setExists(jaxb.isExists());
        repo.setAssigned(jaxb.isAssigned());
        repo.setDead(jaxb.isDead());
        repo.setFailedOperationType(RUtil.getRepoEnumValue(jaxb.getFailedOperationType(), RFailedOperationType.class));

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
    public ShadowType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ShadowType object = new ShadowType();
        RUtil.revive(object, prismContext);
        RShadow.copyToJAXB(this, object, prismContext);

        return object;
    }
}
