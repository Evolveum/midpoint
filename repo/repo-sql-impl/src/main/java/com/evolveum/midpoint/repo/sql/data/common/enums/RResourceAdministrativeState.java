package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAdministrativeStateType;

public enum RResourceAdministrativeState implements SchemaEnum<ResourceAdministrativeStateType> {

    ENABLED(ResourceAdministrativeStateType.ENABLED),
    DISABLED(ResourceAdministrativeStateType.DISABLED);

    private ResourceAdministrativeStateType administrativeState;

    private RResourceAdministrativeState(ResourceAdministrativeStateType administrativeState) {
        this.administrativeState = administrativeState;
    }

    @Override
    public ResourceAdministrativeStateType getSchemaValue() {
        return administrativeState;
    }
}
