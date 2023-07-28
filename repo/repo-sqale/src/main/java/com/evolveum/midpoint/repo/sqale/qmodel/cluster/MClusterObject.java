/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import java.util.UUID;

public class MClusterObject extends MObject {

    public String[] elements;
    public Integer elementsCount;
    public Integer pointsCount;
    public UUID parentRefTargetOid;
    public MObjectType parentRefTargetType;

    public Integer parentRefRelationId;

    public String[] defaultDetection;
    public String pointsDensity;
    public String pointsMean;
    public Integer pointsMinOccupation;
    public Integer pointsMaxOccupation;
    public String riskLevel;

}
