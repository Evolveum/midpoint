/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

/**
 * Querydsl "row bean" type related to {@link QAccessCertificationDefinition}.
 */
public class MAccessCertificationDefinition extends MObject {

    public Integer handlerUriId;
    public Instant lastCampaignStartedTimestamp;
    public Instant lastCampaignClosedTimestamp;
    public UUID ownerRefTargetOid;
    public MObjectType ownerRefTargetType;
    public Integer ownerRefRelationId;
}
