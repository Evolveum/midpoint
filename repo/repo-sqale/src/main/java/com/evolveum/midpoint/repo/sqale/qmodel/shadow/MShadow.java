/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;
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
    public String tag;
    public ShadowKindType kind;
    public Boolean dead;
    public Boolean exist;
    public Instant fullSynchronizationTimestamp;
    public Integer pendingOperationCount;
    public String primaryIdentifierValue;
    public SynchronizationSituationType synchronizationSituation;
    public Instant synchronizationTimestamp;
    public Jsonb attributes;
    // correlation
    public Instant correlationStartTimestamp;
    public Instant correlationEndTimestamp;
    public Instant correlationCaseOpenTimestamp;
    public Instant correlationCaseCloseTimestamp;
    public CorrelationSituationType correlationSituation;

    public Integer disableReasonId;
    public Instant enableTimestamp;
    public Instant disableTimestamp;
    public Instant lastLoginTimestamp;
}
