/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import java.time.Instant;
import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QAccessCertificationWorkItem}.
 */
public class MAccessCertificationWorkItem extends MContainer {

    public Long accessCertCaseCid;

    public Instant closeTimestamp;
    public Integer campaignIteration;
    public String outcome;
    public Instant outputChangeTimestamp;
    public UUID performerRefTargetOid;
    public MObjectType performerRefTargetType;
    public Integer performerRefRelationId;
    public Integer stageNumber;
}
