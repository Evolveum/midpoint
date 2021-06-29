/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;

import java.time.Instant;
import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QAccessCertificationCampaign}.
 */
public class MAccessCertificationCampaign extends MObject {

    public UUID definitionRefTargetOid;
    public MObjectType definitionRefTargetType;
    public Integer definitionRefRelationId;
    public Instant endTimestamp;
    public Integer handlerUriId;
    public Integer campaignIteration;
    public UUID ownerRefTargetOid;
    public MObjectType ownerRefTargetType;
    public Integer ownerRefRelationId;
    public Integer stageNumber;
    public Instant startTimestamp;
    public AccessCertificationCampaignStateType state;
}
