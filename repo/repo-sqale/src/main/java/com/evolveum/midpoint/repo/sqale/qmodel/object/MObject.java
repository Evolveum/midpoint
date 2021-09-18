/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;

/**
 * Querydsl "row bean" type related to {@link QObject}.
 * It is also used for other mappings/objects types with no additional columns in their tables.
 */
public class MObject {

    public UUID oid;
    // generated column must be null for insert/updates, but trigger fixes it, so we can use it!
    public MObjectType objectType;
    public String nameOrig;
    public String nameNorm;
    public byte[] fullObject;
    public UUID tenantRefTargetOid;
    public MObjectType tenantRefTargetType;
    public Integer tenantRefRelationId;
    public String lifecycleState;
    public Long containerIdSeq; // next available container ID (for PCV of multi-valued containers)
    public Integer version;
    // complex DB fields
    public Integer[] policySituations;
    public String[] subtypes;
    public String fullTextInfo;
    public Jsonb ext;
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

    public PolyString getName() {
        return new PolyString(nameOrig, nameNorm);
    }

    @Override
    public String toString() {
        return SqaleUtils.toString(this);
    }
}
