/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.prism.polystring.PolyString;
import dev.cel.common.values.MapValue;
import dev.cel.common.values.StringValue;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Radovan Semancik
 */
public class PolyStringCelMapValue extends MapValue<StringValue,StringValue> {

    private final PolyString polystring;

    PolyStringCelMapValue(PolyString polystring) {
        this.polystring = polystring;
    }

    public static PolyStringCelMapValue create(PolyString polystring) {
        return new PolyStringCelMapValue(polystring);
    }

    @Override
    public Map<StringValue, StringValue> value() {
        return Map.of(StringValue.create(PolyString.F_ORIG.getLocalPart()), StringValue.create(polystring.getOrig()),
                StringValue.create(PolyString.F_NORM.getLocalPart()), StringValue.create(polystring.getNorm()));
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
    public @NotNull Set<StringValue> keySet() {
        return value().keySet();
    }

    @Override
    public @NotNull Collection<StringValue> values() {
        return value().values();
    }

    @Override
    public @NotNull Set<Entry<StringValue, StringValue>> entrySet() {
        return value().entrySet();
    }
}
