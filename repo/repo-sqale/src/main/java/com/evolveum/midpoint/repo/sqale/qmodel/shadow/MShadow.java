/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * Querydsl "row bean" type related to {@link QShadow}.
 */
public class MShadow extends MObject {

    public Integer objectClassId;
    public UUID resourceRefTargetOid;
    public MObjectType resourceRefTargetType;
    public Integer resourceRefRelationId;
    public String intent;
    public ShadowKindType kind;
    public Integer attemptNumber;
    public Boolean dead;
    public Boolean exist;
    public Instant fullSynchronizationTimestamp;
    public Integer pendingOperationCount;
    public String primaryIdentifierValue;
    public SynchronizationSituationType synchronizationSituation;
    public Instant synchronizationTimestamp;
}
