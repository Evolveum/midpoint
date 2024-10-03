/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

@Ignore
@Entity
@Table(name = RAuditEventRecord.TABLE_NAME, indexes = {
        @Index(name = "iTimestampValue", columnList = RAuditEventRecord.COLUMN_TIMESTAMP),
        @Index(name = "iAuditEventRecordEStageTOid",
                columnList = RAuditEventRecord.EVENT_STAGE_COLUMN_NAME
                        + ", " + RAuditEventRecord.TARGET_OID_COLUMN_NAME)
})
public class RAuditEventRecord implements Serializable {

    public static final String TABLE_NAME = "m_audit_event";
    public static final String COLUMN_TIMESTAMP = "timestampValue";

    private static final long serialVersionUID = 621116861556252436L;

    public static final String ID_COLUMN_NAME = "id";
    public static final String ATTORNEY_NAME_COLUMN_NAME = "attorneyName";
    public static final String ATTORNEY_OID_COLUMN_NAME = "attorneyOid";
    private static final String CHANNEL_COLUMN_NAME = "channel";
    private static final String EVENT_IDENTIFIER_COLUMN_NAME = "eventIdentifier";
    public static final String EVENT_STAGE_COLUMN_NAME = "eventStage";
    private static final String EVENT_TYPE_COLUMN_NAME = "eventType";
    private static final String HOST_IDENTIFIER_COLUMN_NAME = "hostIdentifier";
    public static final String INITIATOR_NAME_COLUMN_NAME = "initiatorName";
    public static final String INITIATOR_OID_COLUMN_NAME = "initiatorOid";
    public static final String INITIATOR_TYPE_COLUMN_NAME = "initiatorType";
    private static final String MESSAGE_COLUMN_NAME = "message";
    private static final String NODE_IDENTIFIER_COLUMN_NAME = "nodeIdentifier";
    private static final String OUTCOME_COLUMN_NAME = "outcome";
    private static final String PARAMETER_COLUMN_NAME = "parameter";
    private static final String REMOTE_HOST_ADDRESS_COLUMN_NAME = "remoteHostAddress";
    private static final String REQUEST_IDENTIFIER_COLUMN_NAME = "requestIdentifier";
    private static final String RESULT_COLUMN_NAME = "result";
    private static final String SESSION_IDENTIFIER_COLUMN_NAME = "sessionIdentifier";
    public static final String TARGET_NAME_COLUMN_NAME = "targetName";
    public static final String TARGET_OID_COLUMN_NAME = "targetOid";
    public static final String TARGET_OWNER_NAME_COLUMN_NAME = "targetOwnerName";
    public static final String TARGET_OWNER_OID_COLUMN_NAME = "targetOwnerOid";
    public static final String TARGET_OWNER_TYPE_COLUMN_NAME = "targetOwnerType";
    public static final String TARGET_TYPE_COLUMN_NAME = "targetType";
    private static final String TASK_IDENTIFIER_COLUMN_NAME = "taskIdentifier";
    private static final String TASK_OID_COLUMN_NAME = "taskOID";
    private static final String TIMESTAMP_VALUE_COLUMN_NAME = "timestampValue";

    private long id;
    private Timestamp timestamp;
    private String eventIdentifier;
    private String sessionIdentifier;
    private String requestIdentifier;
    private String taskIdentifier;
    private String taskOID;
    private String hostIdentifier;
    private String nodeIdentifier;
    private String remoteHostAddress;

    // prism object
    private String initiatorOid;
    private String initiatorName;
    private RObjectType initiatorType;
    // prism object - user
    private String attorneyOid;
    private String attorneyName;
    // prism object
    private String targetOid;
    private String targetName;
    private RObjectType targetType;
    // prism object
    private String targetOwnerOid;
    private String targetOwnerName;
    private RObjectType targetOwnerType;

    private RAuditEventType eventType;
    private RAuditEventStage eventStage;

    // collection of object deltas
    private Set<RObjectDeltaOperation> deltas;
    private String channel;
    private ROperationResultStatus outcome;
    private String parameter;
    private String message;
    private Set<RAuditItem> changedItems;
    private Set<RAuditPropertyValue> propertyValues;
    private Set<RAuditReferenceValue> referenceValues;
    private Set<RTargetResourceOid> resourceOids;

    private String result;

    public String getResult() {
        return result;
    }

    @Column(length = AuditService.MAX_MESSAGE_SIZE)
    public String getMessage() {
        return message;
    }

    public String getParameter() {
        return parameter;
    }

    public String getChannel() {
        return channel;
    }

    @ForeignKey(name = "fk_audit_delta")
    @OneToMany(mappedBy = "record", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RObjectDeltaOperation> getDeltas() {
        if (deltas == null) {
            deltas = new HashSet<>();
        }
        return deltas;
    }

    @ForeignKey(name = "fk_audit_item")
    @OneToMany(mappedBy = "record", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RAuditItem> getChangedItems() {
        if (changedItems == null) {
            changedItems = new HashSet<>();
        }
        return changedItems;
    }

    @ForeignKey(name = "fk_audit_prop_value")
    @OneToMany(mappedBy = "record", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RAuditPropertyValue> getPropertyValues() {
        if (propertyValues == null) {
            propertyValues = new HashSet<>();
        }
        return propertyValues;
    }

    @ForeignKey(name = "fk_audit_ref_value")
    @OneToMany(mappedBy = "record", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RAuditReferenceValue> getReferenceValues() {
        if (referenceValues == null) {
            referenceValues = new HashSet<>();
        }
        return referenceValues;
    }

    @ForeignKey(name = "fk_audit_resource")
    @OneToMany(mappedBy = "record", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL })
    public Set<RTargetResourceOid> getResourceOids() {
        if (resourceOids == null) {
            resourceOids = new HashSet<>();
        }
        return resourceOids;
    }

    public String getEventIdentifier() {
        return eventIdentifier;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RAuditEventStage getEventStage() {
        return eventStage;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RAuditEventType getEventType() {
        return eventType;
    }

    public String getHostIdentifier() {
        return hostIdentifier;
    }

    public String getRemoteHostAddress() {
        return remoteHostAddress;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getInitiatorOid() {
        return initiatorOid;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RObjectType getInitiatorType() {
        return initiatorType;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getAttorneyOid() {
        return attorneyOid;
    }

    public String getAttorneyName() {
        return attorneyName;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatus getOutcome() {
        return outcome;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    public String getTargetName() {
        return targetName;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getTargetOid() {
        return targetOid;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RObjectType getTargetType() {
        return targetType;
    }

    public String getTargetOwnerName() {
        return targetOwnerName;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getTargetOwnerOid() {
        return targetOwnerOid;
    }

    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    public String getTaskOID() {
        return taskOID;
    }

    @Column(name = COLUMN_TIMESTAMP)
    public Timestamp getTimestamp() {
        return timestamp;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RObjectType getTargetOwnerType() {
        return targetOwnerType;
    }

    public void setTargetOwnerType(RObjectType targetOwnerType) {
        this.targetOwnerType = targetOwnerType;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setDeltas(Set<RObjectDeltaOperation> deltas) {
        this.deltas = deltas;
    }

    public void setChangedItems(Set<RAuditItem> changedItems) {
        this.changedItems = changedItems;
    }

    public void setResourceOids(Set<RTargetResourceOid> resourceOids) {
        this.resourceOids = resourceOids;
    }

    public void setPropertyValues(Set<RAuditPropertyValue> propertyValues) {
        this.propertyValues = propertyValues;
    }

    public void setReferenceValues(Set<RAuditReferenceValue> referenceValues) {
        this.referenceValues = referenceValues;
    }

    public void setEventIdentifier(String eventIdentifier) {
        this.eventIdentifier = eventIdentifier;
    }

    public void setEventStage(RAuditEventStage eventStage) {
        this.eventStage = eventStage;
    }

    public void setEventType(RAuditEventType eventType) {
        this.eventType = eventType;
    }

    public void setHostIdentifier(String hostIdentifier) {
        this.hostIdentifier = hostIdentifier;
    }

    public void setRemoteHostAddress(String remoteHostAddress) {
        this.remoteHostAddress = remoteHostAddress;
    }

    public void setNodeIdentifier(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setInitiatorName(String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public void setInitiatorOid(String initiatorOid) {
        this.initiatorOid = initiatorOid;
    }

    public void setInitiatorType(RObjectType initiatorType) {
        this.initiatorType = initiatorType;
    }

    public void setAttorneyOid(String attorneyOid) {
        this.attorneyOid = attorneyOid;
    }

    public void setAttorneyName(String attorneyName) {
        this.attorneyName = attorneyName;
    }

    public void setOutcome(ROperationResultStatus outcome) {
        this.outcome = outcome;
    }

    public void setSessionIdentifier(String sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public void setTargetType(RObjectType targetType) {
        this.targetType = targetType;
    }

    public void setTargetOwnerName(String targetOwnerName) {
        this.targetOwnerName = targetOwnerName;
    }

    public void setTargetOwnerOid(String targetOwnerOid) {
        this.targetOwnerOid = targetOwnerOid;
    }

    public void setRequestIdentifier(String requestIdentifier) {
        this.requestIdentifier = requestIdentifier;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public void setTaskOID(String taskOID) {
        this.taskOID = taskOID;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RAuditEventRecord)) { return false; }

        RAuditEventRecord that = (RAuditEventRecord) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
