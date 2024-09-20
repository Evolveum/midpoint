/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

public class MOutlierObject extends MObject {

    public UUID targetObjectRefTargetOid;
    public MObjectType targetObjectRefTargetType;
    public Integer targetObjectRefRelationId;

    public UUID targetClusterRefTargetOid;
    public MObjectType targetClusterRefTargetType;
    public Integer targetClusterRefRelationId;


}
