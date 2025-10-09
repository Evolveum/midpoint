/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

/**
 * @author lazyman
 */
public enum RTimeIntervalStatus implements SchemaEnum<TimeIntervalStatusType> {

    BEFORE(TimeIntervalStatusType.BEFORE),

    IN(TimeIntervalStatusType.IN),

    AFTER(TimeIntervalStatusType.AFTER);

    private TimeIntervalStatusType status;

    RTimeIntervalStatus(TimeIntervalStatusType status) {
        this.status = status;
        RUtil.register(this);
    }

    @Override
    public TimeIntervalStatusType getSchemaValue() {
        return status;
    }
}
