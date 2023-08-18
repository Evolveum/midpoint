/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import java.util.UUID;

public class MClusterObject extends MObject {

    //TODO change long to decimal

    public String riskLevel;
    public Integer usersCount;
    public Integer rolesCount;

    public Long membershipDensity;
    public Long detectedReductionMetric;
    public Long membershipMean;

    public UUID parentRefTargetOid;
    public MObjectType parentRefTargetType;
    public Integer parentRefRelationId;


}
