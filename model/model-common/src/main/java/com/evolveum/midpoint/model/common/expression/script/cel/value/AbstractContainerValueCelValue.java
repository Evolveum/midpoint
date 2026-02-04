/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.value;

import com.evolveum.midpoint.model.common.expression.script.cel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.cel.DynType;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import dev.cel.common.types.CelType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Radovan Semancik
 */
public abstract class AbstractContainerValueCelValue<C extends Containerable> extends CelValue implements Map<String,Object> {

    private final PrismContainerValue<C> containerValue;

    AbstractContainerValueCelValue(PrismContainerValue<C> containerValue) {
        this.containerValue = containerValue;
    }

    @Override
    public Object value() {
        return containerValue;
    }

    @Override
    public boolean isZeroValue() {
        return isEmpty();
    }

    public PrismContainerValue<C> getContainerValue() {
        return containerValue;
    }

    @Override
    public int size() { return containerValue.size(); }

    @Override
    public boolean isEmpty() {
        return containerValue.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            return containerValue.containsItem(keyToPath(key),true);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    @Override
    public Object get(Object key) {
        Item<PrismValue, ItemDefinition<?>> item = containerValue.findItem(keyToPath(key));
        return CelTypeMapper.toListMapValue(item);
    }

    protected ItemPath keyToPath(Object key) {
        return PrismContext.get().path(key);
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
    public void putAll(@NotNull Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public @NotNull Set<String> keySet() {
        return containerValue.getItemNames().stream().map(QName::getLocalPart).collect(Collectors.toSet());
    }

    @Override
    public @NotNull Collection<Object> values() {
        //noinspection SimplifyStreamApiCallChains
        return entrySet().stream().map(Entry::getValue).collect(Collectors.toSet());
    }

    @Override
    public @NotNull Set<Entry<String, Object>> entrySet() {
        // TODO
        throw new UnsupportedOperationException("entrySet");
    }

}
