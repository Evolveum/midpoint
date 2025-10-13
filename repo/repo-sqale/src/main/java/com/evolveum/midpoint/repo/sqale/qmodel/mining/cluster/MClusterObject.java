/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import java.util.UUID;

public class MClusterObject extends MObject {

    public UUID parentRefTargetOid;
    public MObjectType parentRefTargetType;
    public Integer parentRefRelationId;


}
