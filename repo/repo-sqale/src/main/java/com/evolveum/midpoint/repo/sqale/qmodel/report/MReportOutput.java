/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.report;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

/**
 * Querydsl "row bean" type related to {@link QReportOutput}.
 */
public class MReportOutput extends MObject {

    public UUID reportRefTargetOid;
    public Integer reportRefTargetType;
    public Integer reportRefRelationId;
}
