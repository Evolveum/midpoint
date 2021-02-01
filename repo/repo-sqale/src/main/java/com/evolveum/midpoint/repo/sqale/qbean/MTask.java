/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qbean;

import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.qmodel.QTask;

/**
 * Querydsl "row bean" type related to {@link QTask}.
 */
public class MTask extends MObject {

    public Integer binding;
    public String category;
    public Instant completionTimestamp;
    public Integer executionStatus;
    public byte[] fullResult;
    public String handlerUri;
    public Instant lastRunFinishTimestamp;
    public Instant lastRunStartTimestamp;
    public String node;
    public String objectRefTargetOid;
    public Integer objectRefTargetType;
    public Integer objectRefRelationId;
    public String ownerRefTargetOid;
    public Integer ownerRefTargetType;
    public Integer ownerRefRelationId;
    public String parent;
    public Integer recurrence;
    public Integer status;
    public String taskIdentifier;
    public Integer threadStopAction;
    public Integer waitingReason;
}
