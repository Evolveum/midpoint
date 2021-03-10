/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;

/**
 * Querydsl "row bean" type related to {@link QAssignment}.
 */
public class MAssignment extends MContainer {

    public MObjectType ownerType;
    public String lifecycleState;
    public Integer orderValue;
    public UUID orgRefTargetOid;
    public MObjectType orgRefTargetType;
    public Integer orgRefRelationId;
    public UUID targetRefTargetOid;
    public MObjectType targetRefTargetType;
    public Integer targetRefRelationId;
    public UUID tenantRefTargetOid;
    public MObjectType tenantRefTargetType;
    public Integer tenantRefRelationId;
    public Integer extId;
    public String extOid;
    public byte[] ext; // TODO JSONB?
    // construction
    public UUID resourceRefTargetOid;
    public MObjectType resourceRefTargetType;
    public Integer resourceRefRelationId;
    // activation
    public Integer administrativeStatus;
    public Integer effectiveStatus;
    public Instant enableTimestamp;
    public Instant disableTimestamp;
    public String disableReason;
    public Integer validityStatus;
    public Instant validFrom;
    public Instant validTo;
    public Instant validityChangeTimestamp;
    public Instant archiveTimestamp;
    // metadata
    public UUID creatorRefTargetOid;
    public MObjectType creatorRefTargetType;
    public Integer creatorRefRelationId;
    public Integer createChannelId;
    public Instant createTimestamp;
    public UUID modifierRefTargetOid;
    public MObjectType modifierRefTargetType;
    public Integer modifierRefRelationId;
    public Integer modifyChannelId;
    public Instant modifyTimestamp;
}
