/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
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
    }

    @Override
    public FailedOperationTypeType getSchemaValue() {
        return operation;
    }
}
