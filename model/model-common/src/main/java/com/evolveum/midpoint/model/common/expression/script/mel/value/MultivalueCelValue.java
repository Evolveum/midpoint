/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.prism.*;

import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Radovan Semancik
 */
public class MultivalueCelValue<I extends Item> extends CelValue implements List<Object> {

    public static final CelType CEL_TYPE = SimpleType.DYN;

    private final I item;
    private List<Object> processedValues = null;

    MultivalueCelValue(I item) {
        this.item = item;
    }

    public static <I extends Item> MultivalueCelValue<I> create(I item) {
        return new MultivalueCelValue<>(item);
    }

    private List<Object> getProcessedValues() {
        if (processedValues == null) {
            processValues();
        }
        return processedValues;
    }

    private void processValues() {
        processedValues = new ArrayList<>(item.size());
        for (PrismValue value : (List<PrismValue>)item.getValues()) {
            if (value instanceof PrismPropertyValue<?> pval) {
                processedValues.add(CelTypeMapper.toCelValue((Object)pval.getValue()));
            } else if (value instanceof PrismContainerValue<?> cval) {
                processedValues.add(ContainerValueCelValue.create(cval));
            } else if (value instanceof PrismReferenceValue rval) {
                processedValues.add(ReferenceCelValue.create(rval));
            } else {
                throw new IllegalStateException("Unknown value "+value);
            }
        }
    }

    @Override
    public Object value() {
        return item;
    }

    @Override
    public boolean isZeroValue() {
        return isEmpty();
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    public I getItem() {
        return item;
    }

    @Override
    public int size() { return item.size(); }

    @Override
    public boolean isEmpty() {
        return item.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        // TODO: may be more complex than that, needs more tests
        return getProcessedValues().contains(o);
    }

    @Override
    public @NotNull Iterator<Object> iterator() {
        return getProcessedValues().iterator();
    }

    @Override
    public @NotNull Object[] toArray() {
        return getProcessedValues().toArray();
    }

    @Override
    public @NotNull <T> T[] toArray(@NotNull T[] a) {
        return getProcessedValues().toArray(a);
    }

    @Override
    public boolean add(Object o) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public boolean remove(Object key) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        // TODO: may be more complex than that, needs more tests
        return getProcessedValues().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<?> c) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public Object get(int index) {
        return getProcessedValues().get(index);
    }

    @Override
    public Object set(int index, Object element) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public void add(int index, Object element) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public Object remove(int index) {
        throw new UnsupportedOperationException("Mutation of prism objects is not supported in CEL");
    }

    @Override
    public int indexOf(Object o) {
        return getProcessedValues().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return getProcessedValues().lastIndexOf(o);
    }

    @Override
    public @NotNull ListIterator<Object> listIterator() {
        return getProcessedValues().listIterator();
    }

    @Override
    public @NotNull ListIterator<Object> listIterator(int index) {
        return getProcessedValues().listIterator(index);
    }

    @Override
    public @NotNull List<Object> subList(int fromIndex, int toIndex) {
        return getProcessedValues().subList(fromIndex, toIndex);
    }

}
