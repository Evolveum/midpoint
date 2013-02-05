package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;

public enum RAvailabilityStatusType {
	
	UP(AvailabilityStatusType.UP),
    DOWN(AvailabilityStatusType.DOWN);

    private AvailabilityStatusType status;

    private RAvailabilityStatusType(AvailabilityStatusType status) {
        this.status = status;
    }

    public AvailabilityStatusType getStatus() {
        return status;
    }

    public static RAvailabilityStatusType toRepoType(AvailabilityStatusType status) {
        if (status == null) {
            return null;
        }

        for (RAvailabilityStatusType repo : RAvailabilityStatusType.values()) {
            if (status.equals(repo.getStatus())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown failed operation type " + status);
    }


}
