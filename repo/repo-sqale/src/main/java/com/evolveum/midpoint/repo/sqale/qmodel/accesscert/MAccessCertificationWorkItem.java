/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
