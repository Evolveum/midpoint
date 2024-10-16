/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
