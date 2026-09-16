/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.CelType;
import dev.cel.common.types.NullableType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.common.values.NullValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * @author Radovan Semancik
 */
public class OperationResultCelValue extends AbstractStructuredCelValue<Object> implements MidPointValueProducer<OperationResultType> {

    public static final String OPERATION_RESULT_PACKAGE_NAME = OperationResultType.class.getTypeName();
    private static final String F_OPERATION = OperationResultType.F_OPERATION.getLocalPart();
    private static final String F_STATUS = OperationResultType.F_STATUS.getLocalPart();
    private static final String F_MESSAGE = OperationResultType.F_MESSAGE.getLocalPart();
    public static final CelType CEL_TYPE = createCelType();

    private final OperationResultType operationResult;

    OperationResultCelValue(OperationResultType operationResult) {
        this.operationResult = operationResult;
    }

    public static OperationResultCelValue create(OperationResultType operationResult) {
        return new OperationResultCelValue(operationResult);
    }

    protected Map<String, Object> createMapValue() {
        Map<String, Object> value = new HashMap<>();
        value.put(F_OPERATION, wrap(operationResult.getOperation()));
        value.put(F_STATUS, wrapStatus(operationResult.getStatus()));
        value.put(F_MESSAGE, wrap(operationResult.getMessage()));
        return value;
    }

    private Object wrapStatus(OperationResultStatusType status) {
        return status == null ? NullValue.NULL_VALUE : status.value();
    }

    @Override
    public OperationResultType getJavaValue() {
        return operationResult;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        final ImmutableSet<String> fieldNames = ImmutableSet.of(F_STATUS);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_STATUS.equals(fieldName) || F_OPERATION.equals(fieldName)
                    || F_MESSAGE.equals(fieldName) ) {
                return Optional.of(NullableType.create(SimpleType.STRING));
            } else {
                throw new IllegalStateException("Illegal request for OperationResult field " + fieldName);
            }
        };
        return StructType.create(OPERATION_RESULT_PACKAGE_NAME, fieldNames, fieldResolver);
    }

}
