/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomItem;
import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AxiomBuilderStreamTarget.ItemBuilder;
import com.evolveum.axiom.lang.api.AxiomBuiltIn;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.concepts.SourceLocation;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

public class ItemContext<V> extends AbstractContext<ValueContext<?>> implements AxiomItemContext<V>, Supplier<AxiomItem<V>>, Dependency<AxiomItem<V>>, ItemBuilder {

    private final AxiomName name;
    Collection<Dependency<AxiomValue<V>>> values = new ArrayList<>();
    private final AxiomItemDefinition definition;

    public ItemContext(ValueContext<?> sourceContext, AxiomName name, AxiomItemDefinition definition, SourceLocation loc) {
        super(sourceContext, loc, sourceContext);
        this.name = name;
        this.definition = definition;
    }

    @Override
    public AxiomName name() {
        return name;
    }

    @Override
    public ValueContext<V> startValue(Object value, SourceLocation loc) {
        ValueContext<V> valueCtx = new ValueContext<>(this, (V) value, loc);
        values.add(valueCtx);
        return valueCtx;
    }

    @Override
    public void endNode(SourceLocation loc) {
        //root().applyRules(this);
    }

    public AxiomTypeDefinition type() {
        return definition.typeDefinition();
    }

    @Override
    protected Optional<AxiomItemDefinition> childItemDef(AxiomName id) {
        return type().itemDefinition(id);
    }

    @Override
    public boolean isSatisfied() {
        for (Dependency<AxiomValue<V>> value : values) {
            if(!(value instanceof ValueContext.ReferenceDependency)) {
                if(!value.isSatisfied()) {
                    return false;
                }
            } else if(value instanceof ValueContext)  {
                if(!value.isSatisfied()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public AxiomItem<V> get() {
        return AxiomItem.from(definition, Collections2.transform(values, v -> v.get()));
    }

    @Override
    public Exception errorMessage() {
        return null;
    }

    public AxiomItemDefinition definition() {
        return definition;
    }

    @Override
    public AxiomValueContext<V> addValue(V value) {
        ValueContext<V> ret = startValue(value, SourceLocation.runtime());
        ret.endValue(SourceLocation.runtime());
        //values.add(Dependency.immediate(AxiomItemValue.from(definition.typeDefinition(), value)));
        return ret;
    }

    @Override
    public void addOperationalValue(AxiomValueContext<?> value) {
        values.add((ValueContext<V>) value);
    }

    @Override
    public void addOperationalValue(AxiomValueReference<V> value) {
        Preconditions.checkState(value instanceof ValueContext.Reference);
        values.add(((ValueContext.Reference) value).asDependency());
    }

    @Override
    public V onlyValue() {
        Preconditions.checkState(values.size() == 1);
        return values.iterator().next().get().value();
    }

    @Override
    public AxiomValueContext<V> only() {
        Preconditions.checkState(values.size() == 1);
        return (AxiomValueContext<V>) values.iterator().next();
    }

    public Dependency<AxiomValue<V>> onlyValue0() {
        if(values.size() == 1) {
            return values.iterator().next();
        }
        return null;
    }

    public void merge(Collection<? extends AxiomValue<?>> values) {
        for(AxiomValue<?> value : values) {
            addCompletedValue(value);
        }
    }

    @Override
    public void addCompletedValue(AxiomValue<?> value) {
        ValueContext<?> valueCtx = startValue(value.value(),SourceLocation.runtime());
        valueCtx.replace(value);
        valueCtx.endValue(SourceLocation.runtime());
    }

    @Override
    public Optional<? extends AxiomValueContext<V>> value(AxiomValueIdentifier id) {
        throw new IllegalStateException("Item is not indexed");
    }

    public void mergeCompleted(Collection<? extends AxiomValue<?>> values) {

    }

    @Override
    public AxiomTypeDefinition currentType() {
        return definition.typeDefinition();
    }

    @Override
    public AxiomTypeDefinition currentInfra() {
        return AxiomBuiltIn.Type.AXIOM_VALUE;
    }

}
