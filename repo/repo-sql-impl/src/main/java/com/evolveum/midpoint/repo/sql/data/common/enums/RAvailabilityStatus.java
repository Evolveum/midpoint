package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;

public enum RAvailabilityStatus {
	
	UP(AvailabilityStatusType.UP),
    DOWN(AvailabilityStatusType.DOWN);

    private AvailabilityStatusType status;

    private RAvailabilityStatus(AvailabilityStatusType status) {
        this.status = status;
    }

    public AvailabilityStatusType getStatus() {
        return status;
    }

    public static RAvailabilityStatus toRepoType(AvailabilityStatusType status) {
        if (status == null) {
            return null;
        }

        for (RAvailabilityStatus repo : RAvailabilityStatus.values()) {
            if (status.equals(repo.getStatus())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown failed operation type " + status);
    }


}
