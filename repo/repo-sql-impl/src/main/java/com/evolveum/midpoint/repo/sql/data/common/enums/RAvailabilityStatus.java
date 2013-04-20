package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;

public enum RAvailabilityStatus implements SchemaEnum<AvailabilityStatusType> {

    UP(AvailabilityStatusType.UP),
    DOWN(AvailabilityStatusType.DOWN);

    private AvailabilityStatusType status;

    private RAvailabilityStatus(AvailabilityStatusType status) {
        this.status = status;
    }

    @Override
    public AvailabilityStatusType getSchemaValue() {
        return status;
    }
}
