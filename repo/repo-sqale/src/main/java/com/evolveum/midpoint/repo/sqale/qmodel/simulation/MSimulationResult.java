/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
