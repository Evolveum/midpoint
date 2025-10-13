/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;

/**
 * @author lazyman
 */
@JaxbType(type = AvailabilityStatusType.class)
public enum RAvailabilityStatus implements SchemaEnum<AvailabilityStatusType> {

    UP(AvailabilityStatusType.UP),
    BROKEN(AvailabilityStatusType.BROKEN),
    DOWN(AvailabilityStatusType.DOWN);

    private AvailabilityStatusType status;

    RAvailabilityStatus(AvailabilityStatusType status) {
        this.status = status;
        RUtil.register(this);
    }

    @Override
    public AvailabilityStatusType getSchemaValue() {
        return status;
    }
}
