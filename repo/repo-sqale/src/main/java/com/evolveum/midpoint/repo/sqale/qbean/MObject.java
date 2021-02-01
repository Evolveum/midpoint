/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qbean;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.qmodel.QObject;

/**
 * Querydsl "row bean" type related to {@link QObject}.
 */
public class MObject {

    public UUID oid;
    public int objectTypeClass; // defaults are set on DB level
    public String nameNorm;
    public String nameOrig;
    public byte[] fullObject;
    public Integer creatorRefRelationId;
    public UUID creatorRefTargetOid;
    public Integer creatorRefTargetType;
    public Integer createChannelId;
    public Instant createTimestamp;
    public Integer modifierRefRelationId;
    public UUID modifierRefTargetOid;
    public Integer modifierRefTargetType;
    public Integer modifyChannelId;
    public Instant modifyTimestamp;
    public Integer tenantRefRelationId;
    public UUID tenantRefTargetOid;
    public Integer tenantRefTargetType;
    public String lifecycleState;
    public Integer version;
    public byte[] ext;

    public PolyString getName() {
        return new PolyString(nameOrig, nameNorm);
    }
}
