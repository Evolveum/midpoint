/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import java.time.Instant;
import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QCaseWorkItem}.
 */
public class MCaseWorkItem extends MContainer {

    public Instant closeTimestamp;
    public Instant createTimestamp;
    public Instant deadline;
    public UUID originalAssigneeRefTargetOid;
    public MObjectType originalAssigneeRefTargetType;
    public Integer originalAssigneeRefRelationId;
    public String outcome;
    public UUID performerRefTargetOid;
    public MObjectType performerRefTargetType;
    public Integer performerRefRelationId;
    public Integer stageNumber;
}
