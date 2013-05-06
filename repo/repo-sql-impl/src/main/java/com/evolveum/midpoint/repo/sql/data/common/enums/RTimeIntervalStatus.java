package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;

/**
 * @author lazyman
 */
public enum RTimeIntervalStatus implements SchemaEnum<TimeIntervalStatusType> {

    BEFORE(TimeIntervalStatusType.BEFORE),

    IN(TimeIntervalStatusType.IN),

    AFTER(TimeIntervalStatusType.AFTER);

    private TimeIntervalStatusType status;

    private RTimeIntervalStatus(TimeIntervalStatusType status) {
        this.status = status;
    }

    @Override
    public TimeIntervalStatusType getSchemaValue() {
        return status;
    }
}
