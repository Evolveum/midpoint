/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel;

import java.util.*;

import com.evolveum.midpoint.prism.polystring.PolyString;

import dev.cel.common.values.MapValue;
import dev.cel.common.values.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Radovan Semancik
 */
public class PolyStringMapValue implements Map<String,String> {

    private final PolyString polystring;

    PolyStringMapValue(PolyString polystring) {
        this.polystring = polystring;
    }

    public static PolyStringMapValue create(PolyString polystring) {
        return new PolyStringMapValue(polystring);
    }

    public Map<String, String> value() {
        return Map.of(PolyString.F_ORIG.getLocalPart(), polystring.getOrig(),
                PolyString.F_NORM.getLocalPart(), polystring.getNorm());
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
}
