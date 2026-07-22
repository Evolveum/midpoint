/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import com.evolveum.midpoint.model.common.expression.script.mel.MelComparable;
import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.CelType;
import dev.cel.common.types.NullableType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.common.values.NullValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Radovan Semancik
 */
public class ObjectDeltaOperationCelValue extends AbstractStructuredCelValue<Object> implements MidPointValueProducer<ObjectDeltaOperationType> {

    public static final String OBJECT_DELTA_OPERATION_PACKAGE_NAME = ObjectDeltaOperationType.class.getTypeName();
    private static final String F_OBJECT_OID = ObjectDeltaOperationType.F_OBJECT_OID.getLocalPart();
    private static final String F_OBJECT_DELTA = ObjectDeltaOperationType.F_OBJECT_DELTA.getLocalPart();
    private static final String F_EXECUTION_RESULT = ObjectDeltaOperationType.F_EXECUTION_RESULT.getLocalPart();
    private static final String F_OBJECT_NAME = ObjectDeltaOperationType.F_OBJECT_NAME.getLocalPart();
    private static final String F_RESOURCE_OID = ObjectDeltaOperationType.F_RESOURCE_OID.getLocalPart();
    private static final String F_RESOURCE_NAME = ObjectDeltaOperationType.F_RESOURCE_NAME.getLocalPart();
    private static final String F_SHADOW_KIND = ObjectDeltaOperationType.F_SHADOW_KIND.getLocalPart();
    private static final String F_SHADOW_INTENT = ObjectDeltaOperationType.F_SHADOW_INTENT.getLocalPart();
    public static final CelType CEL_TYPE = createCelType();

    private final ObjectDeltaOperationType objectDeltaOperation;

    ObjectDeltaOperationCelValue(ObjectDeltaOperationType objectDeltaOperation) {
        this.objectDeltaOperation = objectDeltaOperation;
    }

    public static ObjectDeltaOperationCelValue create(ObjectDeltaOperationType objectDeltaOperation) {
        return new ObjectDeltaOperationCelValue(objectDeltaOperation);
    }

    protected Map<String, Object> createMapValue() {
        Map<String, Object> value = new HashMap<>();
        value.put(F_OBJECT_OID, wrap(objectDeltaOperation.getObjectOid()));
        value.put(F_OBJECT_DELTA, wrapDelta(objectDeltaOperation.getObjectDelta()));
        value.put(F_EXECUTION_RESULT, wrapResult(objectDeltaOperation.getExecutionResult()));
        value.put(F_OBJECT_NAME, wrapPolystring(objectDeltaOperation.getObjectName()));
        value.put(F_RESOURCE_OID, wrap(objectDeltaOperation.getResourceOid()));
        value.put(F_RESOURCE_NAME, wrap(objectDeltaOperation.getResourceName()));
        value.put(F_SHADOW_KIND, wrapKind(objectDeltaOperation.getShadowKind()));
        value.put(F_SHADOW_INTENT, wrap(objectDeltaOperation.getShadowIntent()));
        return value;
    }

    private Object wrapDelta(ObjectDeltaType objectDelta) {
        return objectDelta == null ? NullValue.NULL_VALUE : ObjectDeltaCelValue.create(objectDelta);
    }

    private Object wrapResult(OperationResultType executionResult) {
        return executionResult == null ? NullValue.NULL_VALUE : OperationResultCelValue.create(executionResult);
    }

    private Object wrapKind(ShadowKindType shadowKind) {
        return shadowKind == null ? NullValue.NULL_VALUE : shadowKind.value();
    }

    @Override
    public ObjectDeltaOperationType getJavaValue() {
        return objectDeltaOperation;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        final ImmutableSet<String> fieldNames = ImmutableSet.of(F_OBJECT_OID, F_OBJECT_DELTA, F_EXECUTION_RESULT,
                F_OBJECT_NAME, F_RESOURCE_OID, F_RESOURCE_NAME, F_SHADOW_KIND, F_SHADOW_INTENT);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_OBJECT_OID.equals(fieldName) || F_OBJECT_NAME.equals(fieldName)
                    || F_RESOURCE_OID.equals(fieldName) || F_RESOURCE_NAME.equals(fieldName)
                    || F_SHADOW_KIND.equals(fieldName) || F_SHADOW_INTENT.equals(fieldName)) {
                return Optional.of(NullableType.create(SimpleType.STRING));
            } else if (F_OBJECT_DELTA.equals(fieldName)) {
                return Optional.of(NullableType.create(ObjectDeltaCelValue.CEL_TYPE));
            } else if (F_EXECUTION_RESULT.equals(fieldName)) {
                return Optional.of(NullableType.create(OperationResultCelValue.CEL_TYPE));
            } else {
                throw new IllegalStateException("Illegal request for ObjectDeltaOperation field " + fieldName);
            }
        };
        return StructType.create(OBJECT_DELTA_OPERATION_PACKAGE_NAME, fieldNames, fieldResolver);
    }

}
