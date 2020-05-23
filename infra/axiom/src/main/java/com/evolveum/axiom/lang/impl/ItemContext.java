package com.evolveum.axiom.lang.impl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.spi.AxiomItemStreamTreeBuilder;
import com.evolveum.axiom.lang.spi.SourceLocation;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

public class ItemContext<V> extends AbstractContext<ValueContext<?>> implements AxiomItemContext<V>, Supplier<AxiomItem<V>>, Dependency<AxiomItem<V>>, AxiomItemStreamTreeBuilder.ItemBuilder {

    private final AxiomIdentifier name;
    Collection<Dependency<AxiomItemValue<V>>> values = new ArrayList<>();
    private final AxiomItemDefinition definition;

    public ItemContext(ValueContext<?> sourceContext, AxiomIdentifier name, AxiomItemDefinition definition, SourceLocation loc) {
        super(sourceContext, loc, sourceContext);
        this.name = name;
        this.definition = definition;
    }

    @Override
    public AxiomIdentifier name() {
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
    protected Optional<AxiomItemDefinition> childDef(AxiomIdentifier id) {
        return type().itemDefinition(id);
    }

    @Override
    public boolean isSatisfied() {
        return Dependency.allSatisfied(values);
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
    public V onlyValue() {
        Preconditions.checkState(values.size() == 1);
        return values.iterator().next().get().get();
    }

}
