/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.evolveum.midpoint.model.common.expression.script.cel.CelTypeMapper;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public class ObjectReferenceCelValue extends MidPointCelValue<PrismReferenceValue> implements Map<String,Object> {

    public static final String OBJECT_REFERENCE_PACKAGE_NAME = ObjectReferenceType.class.getTypeName();
    private static final String F_OID = ObjectReferenceType.F_OID.getLocalPart();
    private static final String F_RELATION = ObjectReferenceType.F_RELATION.getLocalPart();
    private static final String F_TYPE = ObjectReferenceType.F_TYPE.getLocalPart();
    public static final CelType CEL_TYPE = createObjectReferenceType();
    private final PrismReferenceValue objectReferenceValue;

    ObjectReferenceCelValue(PrismReferenceValue objectReferenceValue) {
        this.objectReferenceValue = objectReferenceValue;
    }

    public static ObjectReferenceCelValue create(PrismReferenceValue objectReference) {
        return new ObjectReferenceCelValue(objectReference);
    }

    public Map<String, Object> value() {
        return Map.of(F_OID, objectReferenceValue.getOid(),
                F_RELATION, CelTypeMapper.toCelValue(objectReferenceValue.getRelation()));
    }

    @Override
    public PrismReferenceValue getJavaValue() {
        return objectReferenceValue;
    }

    @Override
    public boolean isZeroValue() {
        return isEmpty();
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    public PrismReferenceValue getObjectReferenceValue() {
        return objectReferenceValue;
    }

    public String getOid() {
        return objectReferenceValue.getOid();
    }

    public QName getRelation() {
        return objectReferenceValue.getRelation();
    }

    @Override
    public int size() {
        return value().size();
    }

    @Override
    public boolean isEmpty() {
        return value().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return value().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return value().containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return value().get(key);
    }

    @Override
    public @Nullable String put(String key, Object value) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public @NotNull Set<String> keySet() {
        return value().keySet();
    }

    @Override
    public @NotNull Collection<Object> values() {
        return value().values();
    }

    @Override
    public @NotNull Set<Entry<String, Object>> entrySet() {
        return value().entrySet();
    }

    private static CelType createObjectReferenceType() {
        ImmutableSet<String> fieldNames = ImmutableSet.of(F_OID, F_RELATION, F_TYPE);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_OID.equals(fieldName)) {
                return Optional.of(SimpleType.STRING);
                // TODO
//            }
//                    || OBJECT_REFERENCE_NORM.equals(fieldName)) {
//                return Optional.of(SimpleType.STRING);
            } else {
                throw new IllegalStateException("Illegal request for objectReference field " + fieldName);
            }
        };
        return StructType.create(OBJECT_REFERENCE_PACKAGE_NAME, fieldNames, fieldResolver);
    }

}

