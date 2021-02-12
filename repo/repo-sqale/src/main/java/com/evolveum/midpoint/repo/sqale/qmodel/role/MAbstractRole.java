/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.focus.MFocus;

/**
 * Querydsl "row bean" type related to {@link QAbstractRole}.
 */
public class MAbstractRole extends MFocus {

    public String approvalProcess;
    public Boolean autoassignEnabled;
    public String displayNameNorm;
    public String displayNameOrig;
    public String identifier;
    public UUID ownerRefTargetOid;
    public Integer ownerRefTargetType;
    public Integer ownerRefRelationId;
    public Boolean requestable;
    public String riskLevel;
}
