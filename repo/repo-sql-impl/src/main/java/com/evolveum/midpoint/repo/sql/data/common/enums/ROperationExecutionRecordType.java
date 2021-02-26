/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.enums;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType;

@JaxbType(type = OperationExecutionRecordTypeType.class)
public enum ROperationExecutionRecordType implements SchemaEnum<OperationExecutionRecordTypeType> {

    SIMPLE(OperationExecutionRecordTypeType.SIMPLE),
    COMPLEX(OperationExecutionRecordTypeType.COMPLEX);

    private final OperationExecutionRecordTypeType recordType;

    ROperationExecutionRecordType(OperationExecutionRecordTypeType recordType) {
        this.recordType = recordType;
    }

    @Override
    public OperationExecutionRecordTypeType getSchemaValue() {
        return recordType;
    }

    public static @Nullable ROperationExecutionRecordType fromSchemaValue(
            @Nullable OperationExecutionRecordTypeType jaxb) {
        if (jaxb == null) {
            return null;
        }
        for (ROperationExecutionRecordType st : values()) {
            if (jaxb == st.recordType) {
                return st;
            }
        }
        throw new IllegalArgumentException("Unknown operation execution record type '" + jaxb + "'.");
    }
}
