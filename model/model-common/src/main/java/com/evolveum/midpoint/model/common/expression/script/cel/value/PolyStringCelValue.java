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

import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.parser.Operator;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntimeBuilder;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;

import org.jetbrains.annotations.Nullable;

/**
 * @author Radovan Semancik
 */
public class PolyStringCelValue extends MidPointCelValue<PolyString> implements Map<String,String> {

    public static final String POLYSTRING_PACKAGE_NAME = PolyString.class.getTypeName();
    private static final String F_ORIG = PolyString.F_ORIG.getLocalPart();
    private static final String F_NORM = PolyString.F_NORM.getLocalPart();
    public static final CelType CEL_TYPE = createPolystringType();

    private final PolyString polystring;

    PolyStringCelValue(PolyString polystring) {
        this.polystring = polystring;
    }

    public static PolyStringCelValue create(PolyString polystring) {
        return new PolyStringCelValue(polystring);
    }

    public Map<String, String> value() {
        return Map.of(F_ORIG, polystring.getOrig(),
                F_NORM, polystring.getNorm());
    }

    @Override
    public PolyString getJavaValue() {
        return polystring;
    }

    @Override
    public boolean isZeroValue() {
        return isEmpty();
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    public PolyString getPolystring() {
        return polystring;
    }

    public String getOrig() {
        return polystring.getOrig();
    }

    public String getNorm() {
        return polystring.getNorm();
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
    public String get(Object key) {
        return value().get(key);
    }

    @Override
    public @Nullable String put(String key, String value) {
        return value().put(key,value);
    }

    @Override
    public String remove(Object key) {
        return value().remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends String> m) {
        value().putAll(m);
    }

    @Override
    public void clear() {
        value().clear();
    }

    @Override
    public @NotNull Set<String> keySet() {
        return value().keySet();
    }

    @Override
    public @NotNull Collection<String> values() {
        return value().values();
    }

    @Override
    public @NotNull Set<Entry<String, String>> entrySet() {
        return value().entrySet();
    }

    private static CelType createPolystringType() {
        ImmutableSet<String> fieldNames = ImmutableSet.of(F_ORIG, F_NORM);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_ORIG.equals(fieldName) || F_NORM.equals(fieldName)) {
                return Optional.of(SimpleType.STRING);
            } else {
                throw new IllegalStateException("Illegal request for polystring field " + fieldName);
            }
        };
        return StructType.create(POLYSTRING_PACKAGE_NAME, fieldNames, fieldResolver);
    }


}

