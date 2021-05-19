/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
