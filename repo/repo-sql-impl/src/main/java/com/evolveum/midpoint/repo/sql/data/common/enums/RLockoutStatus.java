/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

@JaxbType(type = LockoutStatusType.class)
public enum RLockoutStatus implements SchemaEnum<LockoutStatusType> {

    NORMAL(LockoutStatusType.NORMAL),
    LOCKED(LockoutStatusType.LOCKED);

    private final LockoutStatusType schemaValue;

    RLockoutStatus(LockoutStatusType schemaValue) {
        this.schemaValue = schemaValue;
        RUtil.register(this);
    }

    @Override
    public LockoutStatusType getSchemaValue() {
        return schemaValue;
    }
}
