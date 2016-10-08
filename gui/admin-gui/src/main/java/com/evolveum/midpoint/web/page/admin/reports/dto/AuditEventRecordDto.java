package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by honchar.
 */
public class AuditEventRecordDto extends Selectable {
    private Long timestamp;

    private String eventIdentifier;
    private String sessionIdentifier;
    private String taskIdentifier;
    private String taskOID;
    private String hostIdentifier;
    private PrismObject<UserType> initiator;
    private PrismReferenceValue target;
    private PrismObject<UserType> targetOwner;
    private AuditEventType eventType;
    private AuditEventStage eventStage;
    private Collection<ObjectDeltaOperation<? extends ObjectType>> deltas;
    private String channel;
    private OperationResultStatus outcome;

    public AuditEventRecordDto(){

    }

    public AuditEventRecordDto(AuditEventRecord record){
        this.timestamp = record.getTimestamp();
    }

    public Long getTimestamp() {
        return timestamp;
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

    public void setDeltas(Collection<ObjectDeltaOperation<? extends ObjectType>> deltas) {
        this.deltas = deltas;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public OperationResultStatus getOutcome() {
        return outcome;
    }

    public void setOutcome(OperationResultStatus outcome) {
        this.outcome = outcome;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // result (e.g. number of entries, returned object, business result of workflow task or process instance - approved, rejected)
    private String result;

    private String parameter;

    private String message;

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


}
