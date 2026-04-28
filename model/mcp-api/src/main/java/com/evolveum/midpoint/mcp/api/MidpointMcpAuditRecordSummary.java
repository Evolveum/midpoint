/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Compact audit row for {@link MidpointMcpAuditSearchResult}. {@code id} is the midPoint {@code eventIdentifier}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MidpointMcpAuditRecordSummary {

    private String id;
    private String timestamp;
    private String eventType;
    private String eventStage;
    private String outcome;
    private MidpointMcpAuditInitiatorView initiator;
    private MidpointMcpAuditTargetView target;
    private String channel;
    private MidpointMcpAuditTaskView task;
    private String node;
    private String message;
    private String summary;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventStage() {
        return eventStage;
    }

    public void setEventStage(String eventStage) {
        this.eventStage = eventStage;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public MidpointMcpAuditInitiatorView getInitiator() {
        return initiator;
    }

    public void setInitiator(MidpointMcpAuditInitiatorView initiator) {
        this.initiator = initiator;
    }

    public MidpointMcpAuditTargetView getTarget() {
        return target;
    }

    public void setTarget(MidpointMcpAuditTargetView target) {
        this.target = target;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public MidpointMcpAuditTaskView getTask() {
        return task;
    }

    public void setTask(MidpointMcpAuditTaskView task) {
        this.task = task;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
