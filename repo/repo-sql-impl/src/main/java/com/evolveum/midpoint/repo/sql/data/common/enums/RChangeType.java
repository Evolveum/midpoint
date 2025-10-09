/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.sql.util.RUtil;

/**
 * @author lazyman
 */
public enum RChangeType implements SchemaEnum<ChangeType> {

    ADD(ChangeType.ADD),

    DELETE(ChangeType.DELETE),

    MODIFY(ChangeType.MODIFY);

    private ChangeType type;

    RChangeType(ChangeType type) {
        this.type = type;
        RUtil.register(this);
    }

    @Override
    public ChangeType getSchemaValue() {
        return type;
    }
}
