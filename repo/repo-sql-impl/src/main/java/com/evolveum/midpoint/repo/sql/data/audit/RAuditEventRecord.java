/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import javax.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.InsertQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

/**
 * @author lazyman
 */
@Ignore
@Entity
@Table(name = RAuditEventRecord.TABLE_NAME, indexes = {
        @Index(name = "iTimestampValue", columnList = RAuditEventRecord.COLUMN_TIMESTAMP) }) // TODO correct index name
public class RAuditEventRecord implements Serializable {

    public static final String TABLE_NAME = "m_audit_event";
    public static final String COLUMN_TIMESTAMP = "timestampValue";

    private static final long serialVersionUID = 621116861556252436L;

    public static final String ID_COLUMN_NAME = "id";
    public static final String ATTORNEY_NAME_COLUMN_NAME = "attorneyName";
    public static final String ATTORNEY_OID_COLUMN_NAME = "attorneyOid";
    private static final String CHANNEL_COLUMN_NAME = "channel";
    private static final String EVENT_IDENTIFIER_COLUMN_NAME = "eventIdentifier";
    private static final String EVENT_STAGE_COLUMN_NAME = "eventStage";
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

    @Enumerated(EnumType.ORDINAL)
    public RAuditEventStage getEventStage() {
        return eventStage;
    }

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

    public static RAuditEventRecord toRepo(AuditEventRecord record, PrismContext prismContext, Boolean isTransient,
            SystemConfigurationAuditType auditConfiguration)
            throws DtoTranslationException {

        Objects.requireNonNull(record, "Audit event record must not be null.");
        Objects.requireNonNull(prismContext, "Prism context must not be null.");

        RAuditEventRecord repo = new RAuditEventRecord();

        if (record.getRepoId() != null) {
            repo.setId(record.getRepoId());
        }

        repo.setChannel(record.getChannel());
        if (record.getTimestamp() != null) {
            repo.setTimestamp(new Timestamp(record.getTimestamp()));
        }
        repo.setEventStage(RAuditEventStage.toRepo(record.getEventStage()));
        repo.setEventType(RAuditEventType.toRepo(record.getEventType()));
        repo.setSessionIdentifier(record.getSessionIdentifier());
        repo.setEventIdentifier(record.getEventIdentifier());
        repo.setHostIdentifier(record.getHostIdentifier());
        repo.setRemoteHostAddress(record.getRemoteHostAddress());
        repo.setNodeIdentifier(record.getNodeIdentifier());
        repo.setParameter(record.getParameter());
        repo.setMessage(RUtil.trimString(record.getMessage(), AuditService.MAX_MESSAGE_SIZE));
        if (record.getOutcome() != null) {
            repo.setOutcome(RUtil.getRepoEnumValue(record.getOutcome().createStatusType(),
                    ROperationResultStatus.class));
        }
        repo.setRequestIdentifier(record.getRequestIdentifier());
        repo.setTaskIdentifier(record.getTaskIdentifier());
        repo.setTaskOID(record.getTaskOid());
        repo.setResult(record.getResult());

        for (String resourceOid : record.getResourceOids()) {
            RTargetResourceOid targetResourceOid = RTargetResourceOid.toRepo(repo, resourceOid);
            targetResourceOid.setTransient(isTransient);
            repo.getResourceOids().add(targetResourceOid);
        }

        try {
            if (record.getTarget() != null) {
                PrismReferenceValue target = record.getTarget();
                repo.setTargetName(getOrigName(target));
                repo.setTargetOid(target.getOid());

                repo.setTargetType(ClassMapper.getHQLTypeForQName(target.getTargetType()));
            }
            if (record.getTargetOwner() != null) {
                PrismObject<? extends FocusType> targetOwner = record.getTargetOwner();
                repo.setTargetOwnerName(getOrigName(targetOwner));
                repo.setTargetOwnerOid(targetOwner.getOid());
                repo.setTargetOwnerType(ClassMapper.getHQLTypeForClass(targetOwner.getCompileTimeClass()));
            }
            if (record.getInitiator() != null) {
                PrismObject<? extends ObjectType> initiator = record.getInitiator();
                repo.setInitiatorName(getOrigName(initiator));
                repo.setInitiatorOid(initiator.getOid());
                repo.setInitiatorType(ClassMapper.getHQLTypeForClass(initiator.asObjectable().getClass()));
            }
            if (record.getAttorney() != null) {
                PrismObject<? extends FocusType> attorney = record.getAttorney();
                repo.setAttorneyName(getOrigName(attorney));
                repo.setAttorneyOid(attorney.getOid());
            }

            for (ObjectDeltaOperation<?> delta : record.getDeltas()) {
                if (delta == null) {
                    continue;
                }

                ObjectDelta<?> objectDelta = delta.getObjectDelta();
                for (ItemDelta<?, ?> itemDelta : objectDelta.getModifications()) {
                    ItemPath path = itemDelta.getPath();
                    CanonicalItemPath canonical = prismContext.createCanonicalItemPath(path, objectDelta.getObjectTypeClass());
                    for (int i = 0; i < canonical.size(); i++) {
                        RAuditItem changedItem = RAuditItem.toRepo(repo, canonical.allUpToIncluding(i).asString());
                        changedItem.setTransient(isTransient);
                        repo.getChangedItems().add(changedItem);
                    }
                }

                RObjectDeltaOperation rDelta = RObjectDeltaOperation.toRepo(repo, delta, prismContext, auditConfiguration);
                rDelta.setTransient(true);
                rDelta.setRecord(repo);
                repo.getDeltas().add(rDelta);
            }

            for (Map.Entry<String, Set<String>> propertyEntry : record.getProperties().entrySet()) {
                for (String propertyValue : propertyEntry.getValue()) {
                    RAuditPropertyValue val = RAuditPropertyValue.toRepo(
                            repo, propertyEntry.getKey(), RUtil.trimString(propertyValue, AuditService.MAX_PROPERTY_SIZE));
                    val.setTransient(isTransient);
                    repo.getPropertyValues().add(val);
                }
            }
            for (Map.Entry<String, Set<AuditReferenceValue>> referenceEntry : record.getReferences().entrySet()) {
                for (AuditReferenceValue referenceValue : referenceEntry.getValue()) {
                    RAuditReferenceValue val = RAuditReferenceValue.toRepo(repo, referenceEntry.getKey(), referenceValue);
                    val.setTransient(isTransient);
                    repo.getReferenceValues().add(val);
                }
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return repo;
    }

    public static AuditEventRecord fromRepo(RAuditEventRecord repo, PrismContext prismContext, boolean useUtf16) {

        AuditEventRecord audit = new AuditEventRecord();
        audit.setChannel(repo.getChannel());
        audit.setEventIdentifier(repo.getEventIdentifier());
        if (repo.getEventStage() != null) {
            audit.setEventStage(repo.getEventStage().getStage());
        }
        if (repo.getEventType() != null) {
            audit.setEventType(repo.getEventType().getType());
        }
        audit.setHostIdentifier(repo.getHostIdentifier());
        audit.setRemoteHostAddress(repo.getRemoteHostAddress());
        audit.setNodeIdentifier(repo.getNodeIdentifier());
        audit.setMessage(repo.getMessage());

        if (repo.getOutcome() != null) {
            audit.setOutcome(repo.getOutcome().getStatus());
        }
        audit.setParameter(repo.getParameter());
        audit.setResult(repo.getResult());
        audit.setSessionIdentifier(repo.getSessionIdentifier());
        audit.setRequestIdentifier(repo.getRequestIdentifier());
        audit.setTaskIdentifier(repo.getTaskIdentifier());
        audit.setTaskOid(repo.getTaskOID());
        if (repo.getTimestamp() != null) {
            audit.setTimestamp(repo.getTimestamp().getTime());
        }

        for (RTargetResourceOid resourceOID : repo.getResourceOids()) {
            audit.getResourceOids().add(resourceOID.getResourceOid());
        }

        List<ObjectDeltaOperation<?>> odos = new ArrayList<>();
        for (RObjectDeltaOperation rodo : repo.getDeltas()) {
            try {
                ObjectDeltaOperation odo = RObjectDeltaOperation.fromRepo(rodo, prismContext, useUtf16);
                odos.add(odo);
            } catch (Exception ex) {

                // TODO: for now thi is OK, if we cannot parse detla, just skipp
                // it.. Have to be resolved later;
            }
        }

        audit.addDeltas(odos);

        for (RAuditPropertyValue rPropertyValue : repo.getPropertyValues()) {
            audit.addPropertyValue(rPropertyValue.getName(), rPropertyValue.getValue());
        }
        for (RAuditReferenceValue rRefValue : repo.getReferenceValues()) {
            audit.addReferenceValue(rRefValue.getName(), rRefValue.fromRepo(prismContext));
        }

        audit.setRepoId(repo.getId());

        return audit;
        // initiator, attorney, target, targetOwner

    }

    public static SingleSqlQuery toRepo(AuditEventRecord record, Map<String, String> customColumn)
            throws DtoTranslationException {

        Objects.requireNonNull(record, "Audit event record must not be null.");
        InsertQueryBuilder insertBuilder = new InsertQueryBuilder(TABLE_NAME);
        if (record.getRepoId() != null) {
            insertBuilder.addParameter(ID_COLUMN_NAME, record.getRepoId());
        }
        insertBuilder.addParameter(CHANNEL_COLUMN_NAME, record.getChannel());
        insertBuilder.addParameter(TIMESTAMP_VALUE_COLUMN_NAME, new Timestamp(record.getTimestamp()));
        insertBuilder.addParameter(EVENT_STAGE_COLUMN_NAME, RAuditEventStage.toRepo(record.getEventStage()));
        insertBuilder.addParameter(EVENT_TYPE_COLUMN_NAME, RAuditEventType.toRepo(record.getEventType()));
        insertBuilder.addParameter(SESSION_IDENTIFIER_COLUMN_NAME, record.getSessionIdentifier());
        insertBuilder.addParameter(EVENT_IDENTIFIER_COLUMN_NAME, record.getEventIdentifier());
        insertBuilder.addParameter(HOST_IDENTIFIER_COLUMN_NAME, record.getHostIdentifier());
        insertBuilder.addParameter(REMOTE_HOST_ADDRESS_COLUMN_NAME, record.getRemoteHostAddress());
        insertBuilder.addParameter(NODE_IDENTIFIER_COLUMN_NAME, record.getNodeIdentifier());
        insertBuilder.addParameter(PARAMETER_COLUMN_NAME, record.getParameter());
        insertBuilder.addParameter(MESSAGE_COLUMN_NAME, RUtil.trimString(record.getMessage(), AuditService.MAX_MESSAGE_SIZE));
        if (record.getOutcome() != null) {
            insertBuilder.addParameter(OUTCOME_COLUMN_NAME, RUtil.getRepoEnumValue(record.getOutcome().createStatusType(),
                    ROperationResultStatus.class));
        } else {
            insertBuilder.addParameter(OUTCOME_COLUMN_NAME, null);
        }
        insertBuilder.addParameter(REQUEST_IDENTIFIER_COLUMN_NAME, record.getRequestIdentifier());
        insertBuilder.addParameter(TASK_IDENTIFIER_COLUMN_NAME, record.getTaskIdentifier());
        insertBuilder.addParameter(TASK_OID_COLUMN_NAME, record.getTaskOid());
        insertBuilder.addParameter(RESULT_COLUMN_NAME, record.getResult());

        try {
            if (record.getTarget() != null) {
                PrismReferenceValue target = record.getTarget();
                insertBuilder.addParameter(TARGET_NAME_COLUMN_NAME, getOrigName(target));
                insertBuilder.addParameter(TARGET_OID_COLUMN_NAME, target.getOid());
                insertBuilder.addParameter(TARGET_TYPE_COLUMN_NAME, ClassMapper.getHQLTypeForQName(target.getTargetType()));
            }
            if (record.getTargetOwner() != null) {
                PrismObject<? extends FocusType> targetOwner = record.getTargetOwner();
                insertBuilder.addParameter(TARGET_OWNER_NAME_COLUMN_NAME, getOrigName(targetOwner));
                insertBuilder.addParameter(TARGET_OWNER_OID_COLUMN_NAME, targetOwner.getOid());
                insertBuilder.addParameter(TARGET_OWNER_TYPE_COLUMN_NAME, ClassMapper.getHQLTypeForClass(targetOwner.getCompileTimeClass()));
            }
            if (record.getInitiator() != null) {
                PrismObject<? extends ObjectType> initiator = record.getInitiator();
                insertBuilder.addParameter(INITIATOR_NAME_COLUMN_NAME, getOrigName(initiator));
                insertBuilder.addParameter(INITIATOR_OID_COLUMN_NAME, initiator.getOid());
                insertBuilder.addParameter(INITIATOR_TYPE_COLUMN_NAME, ClassMapper.getHQLTypeForClass(initiator.asObjectable().getClass()));
            }
            if (record.getAttorney() != null) {
                PrismObject<? extends FocusType> attorney = record.getAttorney();
                insertBuilder.addParameter(ATTORNEY_NAME_COLUMN_NAME, getOrigName(attorney));
                insertBuilder.addParameter(ATTORNEY_OID_COLUMN_NAME, attorney.getOid());
            }

            if (!customColumn.isEmpty()) {
                for (Entry<String, String> property : record.getCustomColumnProperty().entrySet()) {
                    if (!customColumn.containsKey(property.getKey())) {
                        throw new IllegalArgumentException("Audit event record table don't contains column for property " + property.getKey());
                    }
                    insertBuilder.addParameter(customColumn.get(property.getKey()), property.getValue());
                }
            }

        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }

        return insertBuilder.build();
    }

    public static AuditEventRecord fromRepo(ResultSet resultSet) throws SQLException {

        AuditEventRecord audit = new AuditEventRecord();
        audit.setChannel(resultSet.getString(CHANNEL_COLUMN_NAME));
        audit.setEventIdentifier(resultSet.getString(EVENT_IDENTIFIER_COLUMN_NAME));
        if (resultSet.getObject(EVENT_STAGE_COLUMN_NAME) != null) {
            audit.setEventStage(RAuditEventStage.values()[resultSet.getInt(EVENT_STAGE_COLUMN_NAME)].getStage());
        }
        if (resultSet.getObject(EVENT_TYPE_COLUMN_NAME) != null) {
            audit.setEventType(RAuditEventType.values()[resultSet.getInt(EVENT_TYPE_COLUMN_NAME)].getType());
        }
        audit.setHostIdentifier(resultSet.getString(HOST_IDENTIFIER_COLUMN_NAME));
        audit.setRemoteHostAddress(resultSet.getString(REMOTE_HOST_ADDRESS_COLUMN_NAME));
        audit.setNodeIdentifier(resultSet.getString(NODE_IDENTIFIER_COLUMN_NAME));
        audit.setMessage(resultSet.getString(MESSAGE_COLUMN_NAME));

        if (resultSet.getObject(OUTCOME_COLUMN_NAME) != null) {
            audit.setOutcome(
                    ROperationResultStatus.values()[resultSet.getInt(OUTCOME_COLUMN_NAME)].getStatus());
        }
        audit.setParameter(resultSet.getString(PARAMETER_COLUMN_NAME));
        audit.setResult(resultSet.getString(RESULT_COLUMN_NAME));
        audit.setSessionIdentifier(resultSet.getString(SESSION_IDENTIFIER_COLUMN_NAME));
        audit.setRequestIdentifier(resultSet.getString(REQUEST_IDENTIFIER_COLUMN_NAME));
        audit.setTaskIdentifier(resultSet.getString(TASK_IDENTIFIER_COLUMN_NAME));
        audit.setTaskOid(resultSet.getString(TASK_OID_COLUMN_NAME));
        if (resultSet.getTimestamp(TIMESTAMP_VALUE_COLUMN_NAME) != null) {
            audit.setTimestamp(resultSet.getTimestamp(TIMESTAMP_VALUE_COLUMN_NAME).getTime());
        }

        audit.setRepoId(resultSet.getLong(ID_COLUMN_NAME));

        return audit;
    }

    private static String getOrigName(PrismObject object) {
        PolyString name = (PolyString) object.getPropertyRealValue(ObjectType.F_NAME, PolyString.class);
        return name != null ? name.getOrig() : null;
    }

    private static String getOrigName(PrismReferenceValue refval) {
        if (refval.getObject() != null) {
            return getOrigName(refval.getObject());
        }
        PolyString name = refval.getTargetName();
        return name != null ? name.getOrig() : null;
    }

    public void merge(RAuditEventRecord repoRecord) {
        this.id = repoRecord.id;
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
