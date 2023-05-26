/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
