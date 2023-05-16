/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
