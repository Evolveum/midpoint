/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import dev.cel.common.values.CelValue;
import dev.cel.common.values.NullValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.expression.script.mel.MelComparable;
import com.evolveum.midpoint.prism.polystring.PolyString;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractStructuredCelValue<V> extends CelValue implements Map<String,V> {

    private Map<String, V> mapValue = null;

    protected abstract Map<String, V> createMapValue();

    public Map<String, V> value() {
        if (mapValue == null) {
            mapValue = createMapValue();
        }
        return mapValue;
    }

    @Override
    public boolean isZeroValue() { return false; }

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
    public V get(Object key) {
        return value().get(key);
    }

    @Override
    public @Nullable V put(String key, V value) { throw createUnsupportedMutationException(); }

    @Override
    public V remove(Object key) { throw createUnsupportedMutationException(); }

    @Override
    public void putAll(@NotNull Map<? extends String, ? extends V> m) { throw createUnsupportedMutationException(); }

    @Override
    public void clear() { throw createUnsupportedMutationException(); }

    @Override
    public @NotNull Set<String> keySet() {
        return value().keySet();
    }

    @Override
    public @NotNull Collection<V> values() {
        return value().values();
    }

    @Override
    public @NotNull Set<Entry<String, V>> entrySet() {
        return value().entrySet();
    }

    private UnsupportedOperationException createUnsupportedMutationException() {
        return new UnsupportedOperationException("Mutation of structured values is not supported in MEL");
    }

    protected Object wrapPolystring(PolyStringType polyStringType) {
        return polyStringType == null ? NullValue.NULL_VALUE : PolyStringCelValue.create(polyStringType.toPolyString());
    }

    protected Object wrapQName(QName qname) {
        return qname == null ? NullValue.NULL_VALUE : QNameCelValue.create(qname);
    }

    protected Object wrapPrismObjectType(ObjectType objectType) {
        return objectType == null ? NullValue.NULL_VALUE : ObjectCelValue.create(objectType.asPrismObject());
    }

    protected <O extends Objectable> Object wrapPrismObject(PrismObject<O> object) {
        return object == null ? NullValue.NULL_VALUE : ObjectCelValue.create(object);
    }

    protected Object wrap(Object o) {
        return o == null ? NullValue.NULL_VALUE : o;
    }

    protected Object wrapItemPath(ItemPathType pathType) {
        return pathType == null ? NullValue.NULL_VALUE : wrapItemPath(pathType.getItemPath());
    }

    protected Object wrapItemPath(ItemPath path) {
        return path == null ? NullValue.NULL_VALUE : ItemPathCelValue.create(path);
    }
}

