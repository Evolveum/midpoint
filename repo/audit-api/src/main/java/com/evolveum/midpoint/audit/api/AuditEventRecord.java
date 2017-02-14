/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
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


	private Long repoId;
	/**
	 * Timestamp when the event occured.
	 * Timestamp in millis.
	 */
	private Long timestamp;
	
	/**
	 * Unique identification of the event.
	 */
	private String eventIdentifier;
	
	/**
	 * Identitification of (interactive) session in which the event occured.
	 */
	private String sessionIdentifier;
	
	// channel???? (e.g. web gui, web service, ...)
	
	// task ID (not OID!)
	private String taskIdentifier;
	private String taskOID;
	
	// host ID
	private String hostIdentifier;
	
	// initiator (subject, event "owner"): store OID, type(implicit?), name
	private PrismObject<UserType> initiator;

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
	private PrismObject<UserType> targetOwner;
		
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

	private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	private final Map<String, Set<String>> properties = new HashMap<>();

	private final Map<String, Set<AuditReferenceValue>> references = new HashMap<>();

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

	public String getTaskIdentifier() {
		return taskIdentifier;
	}

	public void setTaskIdentifier(String taskIdentifier) {
		this.taskIdentifier = taskIdentifier;
	}

	public String getTaskOID() {
		return taskOID;
	}

	public void setTaskOID(String taskOID) {
		this.taskOID = taskOID;
	}

	public String getHostIdentifier() {
		return hostIdentifier;
	}

	public void setHostIdentifier(String hostIdentifier) {
		this.hostIdentifier = hostIdentifier;
	}

	public PrismObject<UserType> getInitiator() {
		return initiator;
	}

	public void setInitiator(PrismObject<UserType> initiator) {
		this.initiator = initiator;
	}

	public PrismReferenceValue getTarget() {
		return target;
	}

	public void setTarget(PrismReferenceValue target) {
		this.target = target;
	}
	
	// Compatibility and convenience
	public void setTarget(PrismObject<?> targetObject) {
		PrismReferenceValue targetRef = new PrismReferenceValue();
		targetRef.setObject(targetObject);
		this.target = targetRef;
	}

	public PrismObject<UserType> getTargetOwner() {
		return targetOwner;
	}

	public void setTargetOwner(PrismObject<UserType> targetOwner) {
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
    	auditRecordType.setInitiatorRef(ObjectTypeUtil.createObjectRef(initiator, true));
    	auditRecordType.setMessage(message);
    	auditRecordType.setOutcome(OperationResultStatus.createStatusType(outcome));
    	auditRecordType.setParameter(parameter);
    	auditRecordType.setResult(result);
    	auditRecordType.setSessionIdentifier(sessionIdentifier);
    	auditRecordType.setTargetOwnerRef(ObjectTypeUtil.createObjectRef(targetOwner, true));
    	auditRecordType.setTargetRef(ObjectTypeUtil.createObjectRef(target, true));
    	auditRecordType.setTaskIdentifier(taskIdentifier);
    	auditRecordType.setTaskOID(taskOID);
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
    
    public static AuditEventRecord createAuditEventRecord(AuditEventRecordType auditEventRecordType) {
    	AuditEventRecord auditRecord = new AuditEventRecord();
    	auditRecord.setChannel(auditEventRecordType.getChannel());
    	auditRecord.setEventIdentifier(auditEventRecordType.getEventIdentifier());
    	auditRecord.setEventStage(AuditEventStage.toAuditEventStage(auditEventRecordType.getEventStage()));
    	auditRecord.setEventType(AuditEventType.toAuditEventType(auditEventRecordType.getEventType()));
    	auditRecord.setHostIdentifier(auditEventRecordType.getHostIdentifier());
    	auditRecord.setInitiator(getObjectFromObjectReferenceType(auditEventRecordType.getInitiatorRef()));
    	auditRecord.setMessage(auditEventRecordType.getMessage());
    	auditRecord.setOutcome(OperationResultStatus.parseStatusType(auditEventRecordType.getOutcome()));
    	auditRecord.setParameter(auditEventRecordType.getParameter());
    	auditRecord.setResult(auditEventRecordType.getResult());
    	auditRecord.setSessionIdentifier(auditEventRecordType.getSessionIdentifier());
    	auditRecord.setTarget(getReferenceValueFromObjectReferenceType(auditEventRecordType.getTargetRef()));
    	auditRecord.setTargetOwner(getObjectFromObjectReferenceType(auditEventRecordType.getTargetOwnerRef()));
    	auditRecord.setTaskIdentifier(auditEventRecordType.getTaskIdentifier());
    	auditRecord.setTaskOID(auditEventRecordType.getTaskOID());
    	auditRecord.setTimestamp(MiscUtil.asLong(auditEventRecordType.getTimestamp()));
		for (AuditEventRecordPropertyType propertyType : auditEventRecordType.getProperty()) {
			propertyType.getValue().forEach(v -> auditRecord.addPropertyValue(propertyType.getName(), v));
		}
		for (AuditEventRecordReferenceType referenceType : auditEventRecordType.getReference()) {
			referenceType.getValue().forEach(v -> auditRecord.addReferenceValue(referenceType.getName(), AuditReferenceValue.fromXml(v)));
		}
		return auditRecord;
    }
    
    private static PrismReferenceValue getReferenceValueFromObjectReferenceType(ObjectReferenceType refType) {
    	if (refType == null) {
    		return null;
    	}
    	PrismReferenceValue refVal = new PrismReferenceValue(refType.getOid());
    	refVal.setTargetType(refType.getType());
    	refVal.setTargetName(refType.getTargetName());
		return refVal;
	}

	private static PrismObject getObjectFromObjectReferenceType(ObjectReferenceType ref){
    	if (ref == null){
    		return null;
    	}
    	
    	PrismReferenceValue prismRef = ref.asReferenceValue();
    	return prismRef.getObject();
    }
	
	public AuditEventRecord clone() {
		AuditEventRecord clone = new AuditEventRecord();
		clone.channel = this.channel;
		clone.deltas.addAll(MiscSchemaUtil.cloneObjectDeltaOperationCollection(this.deltas));
		clone.eventIdentifier = this.eventIdentifier;
		clone.eventStage = this.eventStage;
		clone.eventType = this.eventType;
		clone.hostIdentifier = this.hostIdentifier;
		clone.initiator = this.initiator;
		clone.outcome = this.outcome;
		clone.sessionIdentifier = this.sessionIdentifier;
		clone.target = this.target;
		clone.targetOwner = this.targetOwner;
		clone.taskIdentifier = this.taskIdentifier;
		clone.taskOID = this.taskOID;
		clone.timestamp = this.timestamp;
        clone.result = this.result;
        clone.parameter = this.parameter;
        clone.message = this.message;
        clone.properties.putAll(properties);		// TODO deep clone?
        clone.references.putAll(references);		// TODO deep clone?
		return clone;
	}

	@Override
	public String toString() {
		return "AUDIT[" + formatTimestamp(timestamp) + " eid=" + eventIdentifier
				+ " sid=" + sessionIdentifier + ", tid=" + taskIdentifier
				+ " toid=" + taskOID + ", hid=" + hostIdentifier + ", I=" + formatObject(initiator)
				+ ", T=" + formatReference(target) + ", TO=" + formatObject(targetOwner) + ", et=" + eventType
				+ ", es=" + eventStage + ", D=" + deltas + ", ch="+ channel +", o=" + outcome + ", r=" + result + ", p=" + parameter
                + ", m=" + message
                + ", prop=" + properties
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
	public String debugDump() {
		return debugDump(0);
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
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Task Identifier", taskIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Task OID", taskOID, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Host Identifier", hostIdentifier, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "Initiator", formatObject(initiator), indent + 1);
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
        DebugUtil.debugDumpWithLabelToStringLn(sb, "References", references, indent + 1);
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

}
