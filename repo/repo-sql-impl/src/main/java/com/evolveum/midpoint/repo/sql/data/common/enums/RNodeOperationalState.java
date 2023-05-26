/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;

@JaxbType(type = NodeOperationalStateType.class)
public enum RNodeOperationalState implements SchemaEnum<NodeOperationalStateType> {

    UP(NodeOperationalStateType.UP),
    DOWN(NodeOperationalStateType.DOWN),
    STARTING(NodeOperationalStateType.STARTING);

    private final NodeOperationalStateType stateType;

    RNodeOperationalState(NodeOperationalStateType stateType) {
        this.stateType = stateType;
        RUtil.register(this);
    }

    @Override
    public NodeOperationalStateType getSchemaValue() {
        return stateType;
    }
}
