/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.cluster;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

public class MParentClusterObject extends MObject {

    public String riskLevel;
    public Integer processedObjectsCount;
    public String meanDensity;
    public String[] roleAnalysisClusterRef;
    public String processMode;
    public String options;

}
