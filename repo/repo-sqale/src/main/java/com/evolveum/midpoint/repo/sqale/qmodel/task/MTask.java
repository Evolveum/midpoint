/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWaitingReasonType;

/**
 * Querydsl "row bean" type related to {@link QTask}.
 */
public class MTask extends MObject {

    public Integer binding;
    public String category;
    public Instant completionTimestamp;
    public TaskExecutionStatusType executionStatus;
    public byte[] fullResult;
    public Integer handlerUriId;
    public Instant lastRunFinishTimestamp;
    public Instant lastRunStartTimestamp;
    public String node;
    public UUID objectRefTargetOid;
    public Integer objectRefTargetType;
    public Integer objectRefRelationId;
    public UUID ownerRefTargetOid;
    public Integer ownerRefTargetType;
    public Integer ownerRefRelationId;
    public String parent;
    public Integer recurrence;
    public OperationResultStatusType resultStatus;
    public String taskIdentifier;
    public Integer threadStopAction;
    public TaskWaitingReasonType waitingReason;
}
