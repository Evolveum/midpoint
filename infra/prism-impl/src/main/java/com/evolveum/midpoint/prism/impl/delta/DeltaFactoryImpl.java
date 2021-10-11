/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.DeltaMapTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 *
 */
public class DeltaFactoryImpl implements DeltaFactory {

    @NotNull private final PrismContext prismContext;
    @NotNull private final PropertyDeltaFactoryImpl propertyDeltaFactory;
    @NotNull private final ReferenceDeltaFactoryImpl referenceDeltaFactory;
    @NotNull private final ContainerDeltaFactoryImpl containerDeltaFactory;
    @NotNull private final ObjectDeltaFactoryImpl objectDeltaFactory;

    public DeltaFactoryImpl(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
        this.propertyDeltaFactory = new PropertyDeltaFactoryImpl(prismContext);
        this.referenceDeltaFactory = new ReferenceDeltaFactoryImpl(prismContext);
        this.containerDeltaFactory = new ContainerDeltaFactoryImpl(prismContext);
        this.objectDeltaFactory = new ObjectDeltaFactoryImpl(prismContext);
    }

    @Deprecated // please use DeltaBuilder instead
    @Override
    public Property property() {
        return propertyDeltaFactory;
    }

    @Deprecated // please use DeltaBuilder instead
    @Override
    public Reference reference() {
        return referenceDeltaFactory;
    }

    @Deprecated // please use DeltaBuilder instead
    @Override
    public Container container() {
        return containerDeltaFactory;
    }

    @Override
    public Object object() {
        return objectDeltaFactory;
    }

    @Override
    public <T> DeltaSetTriple<T> createDeltaSetTriple() {
        return new DeltaSetTripleImpl<>();
    }

    @Override
    public <V> DeltaSetTriple<V> createDeltaSetTriple(Collection<V> zeroSet, Collection<V> plusSet, Collection<V> minusSet) {
        return new DeltaSetTripleImpl<>(zeroSet, plusSet, minusSet);
    }

    @Override
    public <V extends PrismValue> PrismValueDeltaSetTriple<V> createPrismValueDeltaSetTriple() {
        return new PrismValueDeltaSetTripleImpl<>();
    }

    @Override
    public <V extends PrismValue> PrismValueDeltaSetTriple<V> createPrismValueDeltaSetTriple(Collection<V> zeroSet,
            Collection<V> plusSet, Collection<V> minusSet) {
        return new PrismValueDeltaSetTripleImpl<>(zeroSet, plusSet, minusSet);
    }

    @Override
    public <K, V> DeltaMapTriple<K, V> createDeltaMapTriple() {
        return new DeltaMapTripleImpl<>();
    }
}
