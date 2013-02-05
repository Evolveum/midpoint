package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAdministrativeStateType;

public enum RResourceAdministrativeState {
	
	ENABLED(ResourceAdministrativeStateType.ENABLED),
    DISABLED(ResourceAdministrativeStateType.DISABLED);

    private ResourceAdministrativeStateType administrativeState;

    private RResourceAdministrativeState(ResourceAdministrativeStateType administrativeState) {
        this.administrativeState = administrativeState;
    }

    public ResourceAdministrativeStateType getAdministrativeState() {
        return administrativeState;
    }

    public static RResourceAdministrativeState toRepoType(ResourceAdministrativeStateType status) {
        if (status == null) {
            return null;
        }

        for (RResourceAdministrativeState repo : RResourceAdministrativeState.values()) {
            if (status.equals(repo.getAdministrativeState())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown failed operation type " + status);
    }


}
