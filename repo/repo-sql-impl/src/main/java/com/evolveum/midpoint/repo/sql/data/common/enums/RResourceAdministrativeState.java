/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAdministrativeStateType;

/**
 * @author lazyman
 */
@JaxbType(type = RResourceAdministrativeState.class)
public enum RResourceAdministrativeState implements SchemaEnum<ResourceAdministrativeStateType> {

    ENABLED(ResourceAdministrativeStateType.ENABLED),
    DISABLED(ResourceAdministrativeStateType.DISABLED);

    private ResourceAdministrativeStateType administrativeState;

    RResourceAdministrativeState(ResourceAdministrativeStateType administrativeState) {
        this.administrativeState = administrativeState;
        RUtil.register(this);
    }

    @Override
    public ResourceAdministrativeStateType getSchemaValue() {
        return administrativeState;
    }
}
