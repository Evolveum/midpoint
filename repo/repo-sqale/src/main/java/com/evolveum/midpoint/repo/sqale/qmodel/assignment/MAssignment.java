/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReferenceOwner;
import com.evolveum.midpoint.repo.sqlbase.querydsl.Jsonb;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

/**
 * Querydsl "row bean" type related to {@link QAssignment}.
 */
public class MAssignment extends MContainer implements MReferenceOwner<MAssignmentReference> {

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
    public Integer[] policySituations;
    public Jsonb ext;
    // construction
    public UUID resourceRefTargetOid;
    public MObjectType resourceRefTargetType;
    public Integer resourceRefRelationId;
    // activation
    public ActivationStatusType administrativeStatus;
    public ActivationStatusType effectiveStatus;
    public Instant enableTimestamp;
    public Instant disableTimestamp;
    public String disableReason;
    public TimeIntervalStatusType validityStatus;
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

    @Override
    public MAssignmentReference createReference() {
        MAssignmentReference row = new MAssignmentReference();
        row.ownerOid = ownerOid;
        row.assignmentCid = cid;
        return row;
    }
}
