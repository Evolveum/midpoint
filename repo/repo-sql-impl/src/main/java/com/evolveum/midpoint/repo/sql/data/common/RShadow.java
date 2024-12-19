/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import jakarta.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.persistence.Table;
import org.hibernate.annotations.*;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RFailedOperationType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.RShadowKind;
import com.evolveum.midpoint.repo.sql.data.common.enums.RSynchronizationSituation;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@Entity
@Table(name = "m_shadow", indexes = {
        @jakarta.persistence.Index(name = "iShadowNameOrig", columnList = "name_orig"),
        @jakarta.persistence.Index(name = "iShadowNameNorm", columnList = "name_norm"),
        @jakarta.persistence.Index(name = "iPrimaryIdentifierValueWithOC", columnList = "primaryIdentifierValue,objectClass,resourceRef_targetOid", unique = true) })
@org.hibernate.annotations.Table(appliesTo = "m_shadow",
        indexes = {
                @Index(name = "iShadowResourceRef", columnNames = "resourceRef_targetOid"),
                @Index(name = "iShadowDead", columnNames = "dead"),
                @Index(name = "iShadowKind", columnNames = "kind"),
                @Index(name = "iShadowIntent", columnNames = "intent"),
                @Index(name = "iShadowObjectClass", columnNames = "objectClass"),
                @Index(name = "iShadowFailedOperationType", columnNames = "failedOperationType"),
                @Index(name = "iShadowSyncSituation", columnNames = "synchronizationSituation"),
                @Index(name = "iShadowPendingOperationCount", columnNames = "pendingOperationCount")
        })
@ForeignKey(name = "fk_shadow")
@QueryEntity(anyElements = {
        @VirtualAny(jaxbNameLocalPart = "attributes", ownerType = RObjectExtensionType.ATTRIBUTES) })
@Persister(impl = MidPointJoinedPersister.class)
@DynamicUpdate
public class RShadow extends RObject implements ROperationResult {

    private RPolyString nameCopy;

    private String objectClass;
    private String primaryIdentifierValue;

    //operation result
    private ROperationResultStatus status;
    //end of operation result
    private RSimpleEmbeddedReference resourceRef;
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

    private Integer pendingOperationCount;

    @Column(name = "exist")
    public Boolean isExists() {
        return exists;
    }

    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public RShadowKind getKind() {
        return kind;
    }

    @Column(length = RUtil.COLUMN_LENGTH_QNAME)
    public String getObjectClass() {
        return objectClass;
    }

    @Column
    public String getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    @Embedded
    public RSimpleEmbeddedReference getResourceRef() {
        return resourceRef;
    }

    @Column
    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public RFailedOperationType getFailedOperationType() {
        return failedOperationType;
    }

    @JaxbName(localPart = "name")
    @AttributeOverrides({
            @AttributeOverride(name = "orig", column = @Column(name = "name_orig")),
            @AttributeOverride(name = "norm", column = @Column(name = "name_norm"))
    })
    @Embedded
    @NeverNull
    public RPolyString getNameCopy() {
        return nameCopy;
    }

    public void setNameCopy(RPolyString nameCopy) {
        this.nameCopy = nameCopy;
    }

    @JdbcType(IntegerJdbcType.class)
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

    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getSynchronizationTimestamp() {
        return synchronizationTimestamp;
    }

    @Type(XMLGregorianCalendarType.class)
    public XMLGregorianCalendar getFullSynchronizationTimestamp() {
        return fullSynchronizationTimestamp;
    }

    @Override
    @Enumerated(EnumType.ORDINAL)
    @JdbcType(IntegerJdbcType.class)
    public ROperationResultStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    public void setFullSynchronizationTimestamp(XMLGregorianCalendar fullSynchronizationTimestamp) {
        this.fullSynchronizationTimestamp = fullSynchronizationTimestamp;
    }

    public void setSynchronizationTimestamp(XMLGregorianCalendar synchronizationTimestamp) {
        this.synchronizationTimestamp = synchronizationTimestamp;
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

    public void setResourceRef(RSimpleEmbeddedReference resourceRef) {
        this.resourceRef = resourceRef;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public void setPrimaryIdentifierValue(String primaryIdentifierValue) {
        this.primaryIdentifierValue = primaryIdentifierValue;
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

    @Count
    public Integer getPendingOperationCount() {
        return pendingOperationCount;
    }

    public void setPendingOperationCount(Integer pendingOperationCount) {
        this.pendingOperationCount = pendingOperationCount;
    }

    // dynamically called
    public static void copyFromJAXB(ShadowType jaxb, RShadow repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult) throws DtoTranslationException {
        copyObjectInformationFromJAXB(jaxb, repo, repositoryContext, generatorResult);

        repo.setNameCopy(RPolyString.copyFromJAXB(jaxb.getName()));
        repo.setObjectClass(RUtil.qnameToString(jaxb.getObjectClass()));
        repo.setPrimaryIdentifierValue(jaxb.getPrimaryIdentifierValue());
        repo.setIntent(jaxb.getIntent());
        repo.setKind(RUtil.getRepoEnumValue(jaxb.getKind(), RShadowKind.class));
        repo.setFullSynchronizationTimestamp(jaxb.getFullSynchronizationTimestamp());

        if (jaxb.getSynchronizationSituation() != null) {
            repo.setSynchronizationSituation(RUtil.getRepoEnumValue(jaxb.getSynchronizationSituation(),
                    RSynchronizationSituation.class));
        }

        repo.setSynchronizationTimestamp(jaxb.getSynchronizationTimestamp());
        repo.setResourceRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getResourceRef(), repositoryContext.relationRegistry));

        repo.setExists(jaxb.isExists());
        repo.setDead(jaxb.isDead());

        if (jaxb.getAttributes() != null) {
            copyExtensionOrAttributesFromJAXB(jaxb.getAttributes().asPrismContainerValue(), repo, repositoryContext, RObjectExtensionType.ATTRIBUTES, generatorResult);
        }
        repo.pendingOperationCount = jaxb.getPendingOperation().size();
    }
}
