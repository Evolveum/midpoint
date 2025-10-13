/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

public class MOutlier extends MObject {

    public UUID targetObjectRefTargetOid;
    public MObjectType targetObjectRefTargetType;
    public Integer targetObjectRefRelationId;
    public Double overallConfidence;
}
