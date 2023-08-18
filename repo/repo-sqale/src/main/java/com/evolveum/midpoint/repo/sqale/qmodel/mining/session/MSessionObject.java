/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.session;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public class MSessionObject extends MObject {

    //TODO change long to decimal and add processMode?.
    public String riskLevel;

    public RoleAnalysisProcessModeType processMode;
    public Integer processedObjectCount;
    public Long density;
    public Integer clusterCount;

    public Long similarityOption;
    public Integer minMembersOption;
    public Integer overlapOption;

}
