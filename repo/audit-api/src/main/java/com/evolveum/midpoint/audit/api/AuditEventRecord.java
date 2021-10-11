/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.audit.api;

import java.beans.Transient;
import java.text.SimpleDateFormat;
import java.util.*;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPropertyType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

/**
 * Audit event record describes a single event (usually data change) in a format suitable for audit.
 *
 * @author Radovan Semancik
 *
 */
public class AuditEventRecord implements DebugDumpable {

    /**
     * TODO: what is this?
     */
    private Long repoId;

    /**
     * Timestamp when the event occured.
     * Timestamp in millis.
     */
    private Long timestamp;

    /**
     * Unique identification of the event.
     * Every record should have unique event identifier.
     */
    private String eventIdentifier;

    /**
     * <p>
     * Identifier of a request (operation). All the records that are result of
     * processing of a single request should have the same identifier.
     * In usual case there should be be a single request-stage record
     * and one or more execution-stage records with the same request
     * identifier.
     * </p>
     * <p>
     * Please note that this is quite different than task identifier.
     * A single task can make many requests. E.g. a typical reconciliation
     * task will make thousands of operations. All of the audit records from
     * all of those operations will have the same task identifier. But each
     * operation will have a separate request identifier.
     * </p>
     */
    private String requestIdentifier;

    /**
     * Identification of (interactive) session in which the event occured.
     */
    private String sessionIdentifier;

    // channel???? (e.g. web gui, web service, ...)

    /**
     * <p>
     * Task identifier. Operations are executed in a context of a task.
     * This field is an identifier of the task. It is not (necessarily) an
     * OID of the task, as an operation may be executed in an non-persistent
     * (lightweight) task. This field should be populated for all audit records,
     * perhaps except very special system-level records that are executed outside
     * of a task.
     * </p>
     * <p>
     * Please note that this is quite different than request identifier.
     * A single task can make many requests. E.g. a typical reconciliation
     * task will make thousands of operations. All of the audit records from
     * all of those operations will have the same task identifier. But each
     * operation will have a separate request identifier.
     * </p>
     */
    private String taskIdentifier;

    /**
     * Task OID. This field is used for records that are executed in the context
     * of a persistent task.
     */
    private String taskOid;

    private String hostIdentifier;        // local node name as obtained from the networking stack
    private String nodeIdentifier;        // midPoint cluster node identifier (NodeType.nodeIdentifier)
    private String remoteHostAddress;    // remote host address as obtained from the networking stack

    /**
     * Initiator is the (legal) entity on behalf of whom is the action executed.
     * It is the subject of the operation. Authorizations of the initiator are used
     * to evaluate access to the operation. This is the entity who is formally responsible
     * for the operation. Although initiator is always a user in midPoint 3.7 and earlier,
     * the initiator may be an organization in later midPoint versions.
     */
    private PrismObject<? extends FocusType> initiator;

    /**
     * Attorney is the (physical) object who has executed the action. This is the user
     * (or similar object, e.g. service) that has logged-in to the user interface or REST
     * or other mechanism. Figuratively speaking, this is the user that pressed the button
     * to execute the action.
     *
     * For the vast majority of cases, this is really an object of UserType. But sometimes it can be
     * a ServiceType (or, very occasionally, maybe RoleType or OrgType - but this does not make
     * much sense).
     */
    private PrismObject<? extends FocusType> attorney;

    /**
     *  (primary) target (object, the thing acted on): store OID, type, name.
     *  This is reference instead of full object, because sometimes we have just
     *  the OID of the object and reading full object would be too expensive.
     *  The reference can store OID but it can also store whole object if
     *  that is available.
     *  OPTIONAL
     */
    private PrismReferenceValue target;

    // user that the target "belongs to"????: store OID, name
    private PrismObject<FocusType> targetOwner;

    // event type
    private AuditEventType eventType;

    // event stage (request, execution)
    private AuditEventStage eventStage;

    // delta
    private final Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = new ArrayList<>();

    // delta order (primary, secondary)

    private String channel;

    // outcome (success, failure)
    private OperationResultStatus outcome;

    // result (e.g. number of entries, returned object, business result of workflow task or process instance - approved, rejected)
    private String result;

    private String parameter;

    private String message;

    /**
     *  Resource OIDs. This field is used for resource OIDs of target, when target is FocusType or Shadowtype.
     */
    private Set<String> resourceOids = new HashSet<>();

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final Map<String, Set<String>> properties = new HashMap<>();

    private final Map<String, Set<AuditReferenceValue>> references = new HashMap<>();

    private final Map<String, String> customColumnProperty = new HashMap<>();

    // Just a hint for audit service proxy: these OID need not to be resolved, as they are known to not exist.
    private final Set<String> nonExistingReferencedObjects = new HashSet<>();

    public AuditEventRecord() {
    }

    public AuditEventRecord(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public AuditEventRecord(AuditEventType eventType, AuditEventStage eventStage) {
        this.eventType = eventType;
        this.eventStage = eventStage;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void clearTimestamp() {
        timestamp = null;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventIdentifier() {
        return eventIdentifier;
    }

    public void setEventIdentifier(String eventIdentifier) {
        this.eventIdentifier = eventIdentifier;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    public void setSessionIdentifier(String sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    public String getRequestIdentifier() {
        return requestIdentifier;
    }

    public void setRequestIdentifier(String requestIdentifier) {
        this.requestIdentifier = requestIdentifier;
    }

    public String getTaskIdentifier() {
        return taskIdentifier;
    }

    public void setTaskIdentifier(String taskIdentifier) {
        this.taskIdentifier = taskIdentifier;
    }

    public String getTaskOid() {
        return taskOid;
    }

    public void setTaskOid(String taskOid) {
        this.taskOid = taskOid;
    }

    public String getHostIdentifier() {
        return hostIdentifier;
    }

    public void setHostIdentifier(String hostIdentifier) {
        this.hostIdentifier = hostIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public void setNodeIdentifier(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    public String getRemoteHostAddress() {
        return remoteHostAddress;
    }

    public void setRemoteHostAddress(String remoteHostAddress) {
        this.remoteHostAddress = remoteHostAddress;
    }

    /**
     * Initiator is the (legal) entity on behalf of whom is the action executed.
     * It is the subject of the operation. Authorizations of the initiator are used
     * to evaluate access to the operation. This is the entity who is formally responsible
     * for the operation. Although initiator is always a user in midPoint 3.7 and earlier,
     * the initiator may be an organization in later midPoint versions.
     */
    public PrismObject<? extends FocusType> getInitiator() {
        return initiator;
    }

    public void setInitiator(PrismObject<? extends FocusType> initiator) {
        this.initiator = initiator;
    }

    /**
     * Attorney is the (physical) user who have executed the action.
     * This is the user that have logged-in to the user interface. This is the user that
     * pressed the button to execute the action. This is always identity of a user and
     * it will always be a user. It cannot be a company or any other virtual entity.
     */
    public PrismObject<? extends FocusType> getAttorney() {
        return attorney;
    }

    public void setAttorney(PrismObject<? extends FocusType> attorney) {
        this.attorney = attorney;
    }

    public PrismReferenceValue getTarget() {
        return target;
    }

    public void setTarget(PrismReferenceValue target) {
        this.target = target;
    }

    // Compatibility and convenience
    public void setTarget(PrismObject<?> targetObject, PrismContext prismContext) {
        if (targetObject != null) {
            this.target = ObjectTypeUtil.createObjectRef((ObjectType) targetObject.asObjectable(), prismContext).asReferenceValue();
        } else {
            this.target = null;
        }
    }

    public PrismObject<FocusType> getTargetOwner() {
        return targetOwner;
    }

    public void setTargetOwner(PrismObject<FocusType> targetOwner) {
        this.targetOwner = targetOwner;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public AuditEventStage getEventStage() {
        return eventStage;
    }

    public void setEventStage(AuditEventStage eventStage) {
        this.eventStage = eventStage;
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> getDeltas() {
        return deltas;
    }

    public void addDelta(ObjectDeltaOperation<? extends ObjectType> delta) {
        deltas.add(delta);
    }

    public void addDeltas(Collection<ObjectDeltaOperation<? extends ObjectType>> deltasToAdd) {
        deltas.addAll(deltasToAdd);
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void clearDeltas() {
        deltas.clear();
    }

    public OperationResultStatus getOutcome() {
        return outcome;
    }

    public void setOutcome(OperationResultStatus outcome) {
        this.outcome = outcome;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    @Transient
    public Long getRepoId() {
        return repoId;
    }

    public void setRepoId(Long repoId) {
        this.repoId = repoId;
    }

    public Map<String, Set<String>> getProperties() {
        return properties;
    }

    public Set<String> getPropertyValues(String name) {
        return properties.get(name);
    }

    public Map<String, Set<AuditReferenceValue>> getReferences() {
        return references;
    }

    public Set<AuditReferenceValue> getReferenceValues(String name) {
        return references.get(name);
    }

    public Map<String, String> getCustomColumnProperty() {
        return customColumnProperty;
    }

    public Set<String> getResourceOids() {
        return resourceOids;
    }

    public void setResourceOids(Set<String> resourceOids) {
        this.resourceOids = resourceOids;
    }

    public void addResourceOid(String resourceOid) {
        this.resourceOids.add(resourceOid);
    }

    public void addPropertyValue(String key, String value) {
        properties.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    public void addPropertyValueIgnoreNull(String key, Object value) {
        if (value != null) {
            addPropertyValue(key, String.valueOf(value));
        }
    }

    public void addReferenceValueIgnoreNull(String key, ObjectReferenceType value) {
        if (value != null) {
            addReferenceValue(key, value.asReferenceValue());
        }
    }

    public void addReferenceValue(String key, @NotNull AuditReferenceValue value) {
        Validate.notNull(value, "Reference value must not be null");
        references.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    public void addReferenceValue(String key, @NotNull PrismReferenceValue prv) {
        Validate.notNull(prv, "Reference value must not be null");
        addReferenceValue(key, new AuditReferenceValue(prv));
    }

    public void addReferenceValues(String key, @NotNull List<ObjectReferenceType> values) {
        values.forEach(v -> addReferenceValue(key, v.asReferenceValue()));
    }

    public void checkConsistence() {
        if (initiator != null) {
            initiator.checkConsistence();
        }
        if (target != null && target.getObject() != null) {
            target.getObject().checkConsistence();
        }
        if (targetOwner != null) {
            targetOwner.checkConsistence();
        }
        ObjectDeltaOperation.checkConsistence(deltas);
        //        //TODO: should this be here?
//        if (result != null && result.getStatus() != null) {
//            if (result.getStatus() != outcome) {
//                throw new IllegalStateException("Status in result (" + result.getStatus() + ") differs from outcome (" + outcome + ")");
//            }
//        }
    }

    public AuditEventRecordType createAuditEventRecordType(){
        return createAuditEventRecordType(false);
    }

    public AuditEventRecordType createAuditEventRecordType(boolean tolerateInconsistencies) {
        AuditEventRecordType auditRecordType = new AuditEventRecordType();
        auditRecordType.setChannel(channel);
        auditRecordType.setEventIdentifier(eventIdentifier);
        auditRecordType.setEventStage(AuditEventStage.fromAuditEventStage(eventStage));
        auditRecordType.setEventType(AuditEventType.fromAuditEventType(eventType));
        auditRecordType.setHostIdentifier(hostIdentifier);
        auditRecordType.setRemoteHostAddress(remoteHostAddress);
        auditRecordType.setNodeIdentifier(nodeIdentifier);
        auditRecordType.setInitiatorRef(ObjectTypeUtil.createObjectRef(initiator, true));
        auditRecordType.setAttorneyRef(ObjectTypeUtil.createObjectRef(attorney, true));
        auditRecordType.setMessage(message);
        auditRecordType.setOutcome(OperationResultStatus.createStatusType(outcome));
        auditRecordType.setParameter(parameter);
        auditRecordType.setResult(result);
        auditRecordType.setSessionIdentifier(sessionIdentifier);
        auditRecordType.setTargetOwnerRef(ObjectTypeUtil.createObjectRef(targetOwner, true));
        auditRecordType.setTargetRef(ObjectTypeUtil.createObjectRef(target, true));
        auditRecordType.setRequestIdentifier(requestIdentifier);
        auditRecordType.setTaskIdentifier(taskIdentifier);
        auditRecordType.setTaskOID(taskOid);
        auditRecordType.getResourceOid().addAll(resourceOids);
        auditRecordType.setTimestamp(MiscUtil.asXMLGregorianCalendar(timestamp));
        for (ObjectDeltaOperation delta : deltas) {
            ObjectDeltaOperationType odo = new ObjectDeltaOperationType();
            try {
                DeltaConvertor.toObjectDeltaOperationType(delta, odo, DeltaConversionOptions.createSerializeReferenceNames());
                auditRecordType.getDelta().add(odo);
            } catch (Exception e) {
                if (tolerateInconsistencies){
                    if (delta.getExecutionResult() != null){
                        // TODO does this even work? [med]
                        delta.getExecutionResult().setMessage("Could not show audit record, bad data in delta: " + delta.getObjectDelta());
                    } else {
                        OperationResult result = new OperationResult("Create audit event record type");
                        result.setMessage("Could not show audit record, bad data in delta: " + delta.getObjectDelta());
                        odo.setExecutionResult(result.createOperationResultType());
                    }
                    continue;
                } else {
                    throw new SystemException(e.getMessage(), e);
                }
            }
        }
        for (Map.Entry<String, Set<String>> propertyEntry : properties.entrySet()) {
            AuditEventRecordPropertyType propertyType = new AuditEventRecordPropertyType();
            propertyType.setName(propertyEntry.getKey());
            propertyType.getValue().addAll(propertyEntry.getValue());
            auditRecordType.getProperty().add(propertyType);
        }
        for (Map.Entry<String, Set<AuditReferenceValue>> referenceEntry : references.entrySet()) {
            AuditEventRecordReferenceType referenceType = new AuditEventRecordReferenceType();
            referenceType.setName(referenceEntry.getKey());
            referenceEntry.getValue().forEach(v -> referenceType.getValue().add(v.toXml()));
            auditRecordType.getReference().add(referenceType);
        }
        return auditRecordType;
    }

    public AuditEventRecord clone() {
        AuditEventRecord clone = new AuditEventRecord();
        clone.channel = this.channel;
        clone.deltas.addAll(MiscSchemaUtil.cloneObjectDeltaOperationCollection(this.deltas));
        clone.eventIdentifier = this.eventIdentifier;
        clone.eventStage = this.eventStage;
        clone.eventType = this.eventType;
        clone.hostIdentifier = this.hostIdentifier;
        clone.remoteHostAddress = this.remoteHostAddress;
        clone.nodeIdentifier = this.nodeIdentifier;
        clone.initiator = this.initiator;
        clone.attorney = this.attorney;
        clone.outcome = this.outcome;
        clone.sessionIdentifier = this.sessionIdentifier;
        clone.target = this.target;
        clone.targetOwner = this.targetOwner;
        clone.requestIdentifier = this.requestIdentifier;
        clone.taskIdentifier = this.taskIdentifier;
        clone.taskOid = this.taskOid;
        clone.timestamp = this.timestamp;
        clone.result = this.result;
        clone.parameter = this.parameter;
        clone.message = this.message;
        clone.properties.putAll(properties);        // TODO deep clone?
        clone.references.putAll(references);        // TODO deep clone?
        clone.resourceOids.addAll(resourceOids);
        clone.customColumnProperty.putAll(customColumnProperty);
        return clone;
    }

    @Override
    public String toString() {
        return "AUDIT[" + formatTimestamp(timestamp) + " eid=" + eventIdentifier
                + " sid=" + sessionIdentifier + ", rid=" + requestIdentifier + ", tid=" + taskIdentifier
                + " toid=" + taskOid + ", hid=" + hostIdentifier + ", nid=" + nodeIdentifier + ", raddr=" + remoteHostAddress
                + ", I=" + formatObject(initiator) + ", A=" + formatObject(attorney)
                + ", T=" + formatReference(target) + ", TO=" + formatObject(targetOwner) + ", et=" + eventType
                + ", es=" + eventStage + ", D=" + deltas + ", ch="+ channel +", o=" + outcome + ", r=" + result + ", p=" + parameter
                + ", m=" + message
                + ", cuscolprop=" + customColumnProperty
                + ", prop=" + properties
                + ", roid=" + resourceOids
                + ", ref=" + references + "]";
    }

    private String formatResult(OperationResult result) {
        if (result == null || result.getReturns() == null || result.getReturns().isEmpty()) {
            return "nothing";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : result.getReturns().keySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(key);
            sb.append("=");
            sb.append(result.getReturns().get(key));
        }
        return sb.toString();
    }

    private static String formatTimestamp(Long timestamp) {
        if (timestamp == null) {
            return "null";
        }
        return TIMESTAMP_FORMAT.format(new java.util.Date(timestamp));
    }

    private static String formatObject(PrismObject<? extends ObjectType> object) {
        if (object == null) {
            return "null";
        }
        return object.toString();
    }

    private String formatReference(PrismReferenceValue refVal) {
        if (refVal == null) {
            return "null";
        }
        if (refVal.getObject() != null) {
            return formatObject(refVal.getObject());
        }
        return refVal.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("AUDIT");
        sb.append("\n");
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Timestamp", formatTimestamp(timestamp), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Identifier", eventIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Session Identifier", sessionIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Request Identifier", requestIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Task Identifier", taskIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Task OID", taskOid, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Host Identifier", hostIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Node Identifier", nodeIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Remote Host Address", remoteHostAddress, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Initiator", formatObject(initiator), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Attorney", formatObject(attorney), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Target", formatReference(target), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Target Owner", formatObject(targetOwner), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Type", eventType, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Event Stage", eventStage, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Channel", channel, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Outcome", outcome, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Result", result, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Parameter", parameter, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Message", message, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Properties", properties, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Resource OIDs", resourceOids, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "References", references, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "Custom column properties", customColumnProperty, indent + 1);
        DebugUtil.debugDumpLabel(sb, "Deltas", indent + 1);
        if (deltas.isEmpty()) {
            sb.append(" none");
        } else {
            sb.append(" ").append(deltas.size()).append(" deltas\n");
            DebugUtil.debugDump(sb, deltas, indent + 2, false);
        }
        return sb.toString();
    }

    // a bit of hack - TODO place appropriately
    public static void adopt(AuditEventRecordType record, PrismContext prismContext) throws SchemaException {
        for (ObjectDeltaOperationType odo : record.getDelta()) {
            adopt(odo.getObjectDelta(), prismContext);
        }
    }

    public static void adopt(ObjectDeltaType delta, PrismContext prismContext) throws SchemaException {
        if (delta == null) {
            return;
        }
        if (delta.getObjectToAdd() != null) {
            prismContext.adopt(delta.getObjectToAdd().asPrismObject());
        }
        for (ItemDeltaType itemDelta : delta.getItemDelta()) {
            adopt(itemDelta, prismContext);
        }
    }

    private static void adopt(ItemDeltaType itemDelta, PrismContext prismContext) throws SchemaException {
        for (RawType value : itemDelta.getValue()) {
            if (value != null) {
                value.revive(prismContext);
            }
        }
        for (RawType value : itemDelta.getEstimatedOldValue()) {
            if (value != null) {
                value.revive(prismContext);
            }
        }
    }

    public void setInitiatorAndLoginParameter(PrismObject<? extends FocusType> initiator) {
        setInitiator(initiator);
        String parameter = null;
        if (initiator != null) {
            PolyStringType name = initiator.asObjectable().getName();
            if (name != null) {
                parameter = name.getOrig();
            }
        }
        setParameter(parameter);
    }

    @NotNull
    public Set<String> getNonExistingReferencedObjects() {
        return nonExistingReferencedObjects;
    }

    public void addNonExistingReferencedObject(String oid) {
        nonExistingReferencedObjects.add(oid);
    }
}
