/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;

/**
 * @author lazyman
 */
@JaxbType(type = FailedOperationTypeType.class)
public enum RFailedOperationType implements SchemaEnum<FailedOperationTypeType> {

    DELETE(FailedOperationTypeType.DELETE),
    ADD(FailedOperationTypeType.ADD),
    GET(FailedOperationTypeType.GET),
    MODIFY(FailedOperationTypeType.MODIFY);

    private FailedOperationTypeType operation;

    RFailedOperationType(FailedOperationTypeType operation) {
        this.operation = operation;
        RUtil.register(this);
    }

    @Override
    public FailedOperationTypeType getSchemaValue() {
        return operation;
    }
}
