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
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Querydsl "row bean" type related to {@link QTask}.
 */
public class MTask extends MObject {

    public String taskIdentifier;
    public TaskBindingType binding;
    public String category;
    public Instant completionTimestamp;
    public TaskExecutionStateType executionStatus;
    public byte[] fullResult;
    public Integer handlerUriId;
    public Instant lastRunStartTimestamp;
    public Instant lastRunFinishTimestamp;
    public String node;
    public UUID objectRefTargetOid;
    public MObjectType objectRefTargetType;
    public Integer objectRefRelationId;
    public UUID ownerRefTargetOid;
    public MObjectType ownerRefTargetType;
    public Integer ownerRefRelationId;
    public String parent;
    public TaskRecurrenceType recurrence;
    public OperationResultStatusType resultStatus;
    public ThreadStopActionType threadStopAction;
    public TaskWaitingReasonType waitingReason;
    public String[] dependentTaskIdentifiers;
}
