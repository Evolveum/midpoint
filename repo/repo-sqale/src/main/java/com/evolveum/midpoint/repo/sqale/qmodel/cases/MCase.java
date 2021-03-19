/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

/**
 * Querydsl "row bean" type related to {@link QCase}.
 */
public class MCase extends MObject {

    public String state;
    public Instant closeTimestamp;
    public UUID objectRefTargetOid;
    public MObjectType objectRefTargetType;
    public Integer objectRefRelationId;
    public UUID parentRefTargetOid;
    public MObjectType parentRefTargetType;
    public Integer parentRefRelationId;
    public UUID requestorRefTargetOid;
    public MObjectType requestorRefTargetType;
    public Integer requestorRefRelationId;
    public UUID targetRefTargetOid;
    public MObjectType targetRefTargetType;
    public Integer targetRefRelationId;
}
