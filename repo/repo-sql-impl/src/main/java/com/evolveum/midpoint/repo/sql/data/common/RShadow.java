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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RFailedOperationType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.RShadowKind;
import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query.definition.VirtualAny;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.Collection;


/**
 * @author lazyman
 */
@Entity
@Table(name = "m_shadow")
@org.hibernate.annotations.Table(appliesTo = "m_shadow",
        indexes = {@Index(name = "iShadowResourceRef", columnNames = "resourceRef_targetOid"),
                @Index(name = "iShadowDead", columnNames = "dead")})
@ForeignKey(name = "fk_shadow")
@QueryEntity(anyElements = {
        @VirtualAny(jaxbNameLocalPart = "attributes", ownerType = RObjectExtensionType.ATTRIBUTES)})
@Persister(impl = MidPointJoinedPersister.class)
public class RShadow<T extends ShadowType> extends RObject<T> implements OperationResult {

    private static final Trace LOGGER = TraceManager.getTrace(RShadow.class);
    private RPolyString name;

    private String objectClass;
    //operation result
    private ROperationResultStatus status;
    //end of operation result
    private REmbeddedReference resourceRef;
    private Integer attemptNumber;
    private Boolean dead;
    private RFailedOperationType failedOperationType;
    private String intent;
    private RSynchronizationSituation synchronizationSituation;
    //attributes
    private XMLGregorianCalendar synchronizationTimestamp;
    private RShadowKind kind;
    private Boolean exists;
    private XMLGregorianCalendar fullSynchronizationTimestamp;

    @Column(name = "exist")
    public Boolean isExists() {
        return exists;
    }

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = true)
    public RShadowKind getKind() {
        return kind;
    }

    @Column(length = RUtil.COLUMN_LENGTH_QNAME)
    public String getObjectClass() {
        return objectClass;
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

    @Embedded
    public RPolyString getName() {
        return name;
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

    public XMLGregorianCalendar getFullSynchronizationTimestamp() {
        return fullSynchronizationTimestamp;
    }

    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatus getStatus() {
        return status;
    }

    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    public void setFullSynchronizationTimestamp(XMLGregorianCalendar fullSynchronizationTimestamp) {
        this.fullSynchronizationTimestamp = fullSynchronizationTimestamp;
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

    public void setKind(RShadowKind kind) {
        this.kind = kind;
    }

    public void setResourceRef(REmbeddedReference resourceRef) {
        this.resourceRef = resourceRef;
    }

    public void setObjectClass(String objectClass) {
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

    public void setExists(Boolean exists) {
        this.exists = exists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RShadow that = (RShadow) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (attemptNumber != null ? !attemptNumber.equals(that.attemptNumber) : that.attemptNumber != null)
            return false;
        if (failedOperationType != that.failedOperationType) return false;
        if (objectClass != null ? !objectClass.equals(that.objectClass) : that.objectClass != null) return false;
        if (resourceRef != null ? !resourceRef.equals(that.resourceRef) : that.resourceRef != null) return false;
        if (intent != null ? !intent.equals(that.intent) : that.intent != null) return false;
        if (synchronizationSituation != null ? !synchronizationSituation.equals(that.synchronizationSituation) : that.synchronizationSituation != null)
            return false;
        if (kind != null ? !kind.equals(that.kind) : that.kind != null) return false;
        if (exists != null ? !exists.equals(that.exists) : that.exists != null) return false;
        if (status != that.status) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result1 = super.hashCode();
        result1 = 31 * result1 + (name != null ? name.hashCode() : 0);
        result1 = 31 * result1 + (objectClass != null ? objectClass.hashCode() : 0);
        result1 = 31 * result1 + (attemptNumber != null ? attemptNumber.hashCode() : 0);
        result1 = 31 * result1 + (failedOperationType != null ? failedOperationType.hashCode() : 0);
        result1 = 31 * result1 + (intent != null ? intent.hashCode() : 0);
        result1 = 31 * result1 + (synchronizationSituation != null ? synchronizationSituation.hashCode() : 0);
        result1 = 31 * result1 + (kind != null ? kind.hashCode() : 0);
        result1 = 31 * result1 + (exists != null ? exists.hashCode() : 0);
        result1 = 31 * result1 + (fullSynchronizationTimestamp != null ? fullSynchronizationTimestamp.hashCode() : 0);
        result1 = 31 * result1 + (status != null ? status.hashCode() : 0);

        return result1;
    }

    public static <T extends ShadowType> void copyFromJAXB(ShadowType jaxb, RShadow<T> repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setObjectClass(RUtil.qnameToString(jaxb.getObjectClass()));
        repo.setIntent(jaxb.getIntent());
        repo.setKind(RUtil.getRepoEnumValue(jaxb.getKind(), RShadowKind.class));
        repo.setFullSynchronizationTimestamp(jaxb.getFullSynchronizationTimestamp());

        ItemDefinition def = jaxb.asPrismObject().getDefinition();
        RUtil.copyResultFromJAXB(def, ShadowType.F_RESULT, jaxb.getResult(), repo, repositoryContext.prismContext);

        if (jaxb.getSynchronizationSituation() != null) {
            repo.setSynchronizationSituation(RUtil.getRepoEnumValue(jaxb.getSynchronizationSituation(),
                    RSynchronizationSituation.class));
        }

        repo.setSynchronizationTimestamp(jaxb.getSynchronizationTimestamp());
        repo.setResourceRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getResourceRef(), repositoryContext.prismContext));

        repo.setAttemptNumber(jaxb.getAttemptNumber());
        repo.setExists(jaxb.isExists());
        repo.setDead(jaxb.isDead());
        repo.setFailedOperationType(RUtil.getRepoEnumValue(jaxb.getFailedOperationType(), RFailedOperationType.class));

        if (jaxb.getResource() != null) {
            LOGGER.warn("Resource from resource object shadow type won't be saved. It should be " +
                    "translated to resource reference.");
        }

        if (jaxb.getAttributes() != null) {
            copyFromJAXB(jaxb.getAttributes().asPrismContainerValue(), repo, repositoryContext, RObjectExtensionType.ATTRIBUTES);
        }
    }

    @Override
    public T toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        ShadowType object = new ShadowType();
        RUtil.revive(object, prismContext);
        RObject.copyToJAXB(this, object, prismContext, options);

        return (T) object;
    }
}
