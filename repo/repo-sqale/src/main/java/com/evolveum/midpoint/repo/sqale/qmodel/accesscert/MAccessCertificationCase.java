/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

/**
 * Querydsl "row bean" type related to {@link QAccessCertificationCase}.
 */
public class MAccessCertificationCase extends MContainer {

    // activation columns
    public ActivationStatusType administrativeStatus;
    public Instant archiveTimestamp;
    public String disableReason;
    public Instant disableTimestamp;
    public ActivationStatusType effectiveStatus;
    public Instant enableTimestamp;
    public Instant validFrom;
    public Instant validTo;
    public Instant validityChangeTimestamp;
    public TimeIntervalStatusType validityStatus;

    public String currentStageOutcome;
    public byte[] fullObject;
    public Integer campaignIteration;
    public UUID objectRefTargetOid;
    public MObjectType objectRefTargetType;
    public Integer objectRefRelationId;
    public UUID orgRefTargetOid;
    public MObjectType orgRefTargetType;
    public Integer orgRefRelationId;
    public String outcome;
    public Instant remediedTimestamp;
    public Instant currentStageDeadline;
    public Instant currentStageCreateTimestamp;
    public Integer stageNumber;
    public UUID targetRefTargetOid;
    public MObjectType targetRefTargetType;
    public Integer targetRefRelationId;
    public UUID tenantRefTargetOid;
    public MObjectType tenantRefTargetType;
    public Integer tenantRefRelationId;

}
