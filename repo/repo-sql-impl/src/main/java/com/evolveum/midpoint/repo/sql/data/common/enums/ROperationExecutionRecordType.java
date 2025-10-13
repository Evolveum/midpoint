/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType;

@JaxbType(type = OperationExecutionRecordTypeType.class)
public enum ROperationExecutionRecordType implements SchemaEnum<OperationExecutionRecordTypeType> {

    SIMPLE(OperationExecutionRecordTypeType.SIMPLE),
    COMPLEX(OperationExecutionRecordTypeType.COMPLEX);

    private final OperationExecutionRecordTypeType schemaValue;

    ROperationExecutionRecordType(OperationExecutionRecordTypeType schemaValue) {
        this.schemaValue = schemaValue;
        RUtil.register(this);
    }

    @Override
    public OperationExecutionRecordTypeType getSchemaValue() {
        return schemaValue;
    }
}
