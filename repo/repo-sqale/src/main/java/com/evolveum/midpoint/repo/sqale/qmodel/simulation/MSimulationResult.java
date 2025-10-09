/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

public class MSimulationResult extends MObject {

    public Boolean partitioned;
    public UUID rootTaskRefTargetOid;
    public MObjectType rootTaskRefTargetType;
    public Integer rootTaskRefRelationId;
    public Instant startTimestamp;
    public Instant endTimestamp;
}
