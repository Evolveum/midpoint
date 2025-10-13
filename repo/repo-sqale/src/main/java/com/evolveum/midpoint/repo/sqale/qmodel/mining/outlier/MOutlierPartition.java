/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import java.util.UUID;

public class MOutlierPartition extends MContainer {

    public UUID clusterRefOid;
    public MObjectType clusterRefTargetType;
    public Integer clusterRefRelationId;
    public Double overallConfidence;

}
