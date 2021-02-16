/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.time.Instant;
import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QAssignment}.
 */
public class MAssignment {

    public UUID ownerOid;
    public Integer cid;
    public Integer ownerType;
    public Integer assignmentOwner; // TODO necessary?
    public String lifecycleState;
    public Integer orderValue;
    public UUID orgRefTargetOid;
    public Integer orgRefTargetType;
    public Integer orgRefRelationId;
    public UUID resourceRefTargetOid;
    public Integer resourceRefTargetType;
    public Integer resourceRefRelationId;
    public UUID targetRefTargetOid;
    public Integer targetRefTargetType;
    public Integer targetRefRelationId;
    public UUID tenantRefTargetOid;
    public Integer tenantRefTargetType;
    public Integer tenantRefRelationId;
    public Integer extId;
    public String extOid;
    public byte[] ext; // TODO JSONB?
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
    public Integer creatorRefTargetType;
    public Integer creatorRefRelationId;
    public Integer createChannelId;
    public Instant createTimestamp;
    public UUID modifierRefTargetOid;
    public Integer modifierRefTargetType;
    public Integer modifierRefRelationId;
    public Integer modifyChannelId;
    public Instant modifyTimestamp;
}
