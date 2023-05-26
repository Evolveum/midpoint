/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.enums;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

@JaxbType(type = OperationResultStatusType.class)
public enum ROperationResultStatus implements SchemaEnum<OperationResultStatusType> {

    SUCCESS(OperationResultStatusType.SUCCESS),
    WARNING(OperationResultStatusType.WARNING),
    PARTIAL_ERROR(OperationResultStatusType.PARTIAL_ERROR),
    FATAL_ERROR(OperationResultStatusType.FATAL_ERROR),
    NOT_APPLICABLE(OperationResultStatusType.NOT_APPLICABLE),
    IN_PROGRESS(OperationResultStatusType.IN_PROGRESS),
    UNKNOWN(OperationResultStatusType.UNKNOWN),
    HANDLED_ERROR(OperationResultStatusType.HANDLED_ERROR);

    private final OperationResultStatusType status;

    ROperationResultStatus(OperationResultStatusType status) {
        this.status = status;
        RUtil.register(this);
    }

    public OperationResultStatus getStatus() {
        return OperationResultStatus.parseStatusType(status);
    }

    @Override
    public OperationResultStatusType getSchemaValue() {
        return status;
    }

    public static @Nullable ROperationResultStatus from(
            @Nullable OperationResultStatus status) {
        return fromSchemaValue(OperationResultStatus.createStatusType(status));
    }

    public static @Nullable ROperationResultStatus fromSchemaValue(
            @Nullable OperationResultStatusType jaxb) {
        if (jaxb == null) {
            return null;
        }
        for (ROperationResultStatus st : values()) {
            if (jaxb == st.status) {
                return st;
            }
        }
        throw new IllegalArgumentException("Unknown operation result state '" + jaxb + "'.");
    }
}
