package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.lang.spi.AxiomStatementBuilder;
import com.evolveum.axiom.reactor.Requirement;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class StatementContextResult<V> implements Requirement<AxiomStatement<V>> {

    private V value;
    private final List<StatementContextImpl<?>> childrenList = new ArrayList<>();
    private final Multimap<AxiomIdentifier, StatementContext<?>> children = HashMultimap.create();
    private AxiomStatementBuilder<V> builder;

    private final List<StatementRuleContextImpl<V>> rules = new ArrayList<>();

    public StatementContextResult(AxiomItemDefinition definition, AxiomStatementBuilder<V> builder) {
        super();
        this.builder = builder;
    }

    public void setValue(V value) {
        this.value = value;
        this.builder.setValue(value);
    }

    public void add(StatementContextImpl<?> childCtx) {
        childrenList.add(childCtx);
        children.put(childCtx.identifier(), childCtx);
        builder.add(childCtx.identifier(), childCtx.asLazy());
    }
    public V value() {
        return value;
    }

    public Collection<StatementContext<?>> get(AxiomIdentifier identifier) {
        return children.get(identifier);
    }

    @Override
    public boolean isSatisfied() {
        for (StatementRuleContextImpl<?> rule : rules) {
            if(!rule.isApplied()) {
                return false;
            }
        }
        for (StatementContextImpl<?> rule : childrenList) {
            if(!rule.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public AxiomStatement<V> get() {
        return builder.get();
    }

    public void addRule(StatementRuleContextImpl<V> rule) {
        this.rules.add(rule);
    }

    @Override
    public RuleErrorMessage errorMessage() {
        // TODO Auto-generated method stub
        return null;
    }
}
