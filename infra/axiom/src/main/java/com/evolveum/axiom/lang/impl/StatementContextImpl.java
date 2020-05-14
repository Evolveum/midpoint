package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.axiom.concepts.Optionals;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.evolveum.axiom.lang.api.stmt.SourceLocation;
import com.evolveum.axiom.lang.impl.AxiomStatementImpl.Factory;
import com.evolveum.axiom.lang.impl.StatementRuleContext.Action;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;

public class StatementContextImpl<V> implements StatementContext<V>, StatementTreeBuilder {



    private final ModelReactorContext reactor;
    private final AxiomItemDefinition definition;
    private final StatementContextImpl<?> parent;

    private SourceLocation startLocation;
    private SourceLocation endLocation;
    private SourceLocation valueLocation;
    private Requirement<AxiomStatement<V>> result;


    StatementContextImpl(ModelReactorContext reactor, StatementContextImpl<?> parent, AxiomItemDefinition definition, SourceLocation loc) {
        this.parent = parent;
        this.reactor = reactor;
        this.definition = definition;
        this.startLocation = loc;
        AxiomStatementBuilder<V> builder = new AxiomStatementBuilder<>(definition.name(), reactor.typeFactory(definition.type()));
        this.result = new StatementContextResult<>(definition, builder);
    }

    @Override
    public void setValue(Object value) {
        mutableResult().setValue((V) value);
    }

    private StatementContextResult<V> mutableResult() {
        if(result instanceof StatementContextResult) {
            return (StatementContextResult) result;
        }
        throw new IllegalStateException("Result is not mutable");
    }

    boolean isChildAllowed(AxiomIdentifier child) {
        return definition.type().item(child).isPresent();
    }


    @SuppressWarnings("rawtypes")
    @Override
    public StatementContextImpl createChildNode(AxiomIdentifier identifier, SourceLocation loc) {
        StatementContextImpl<V> childCtx = new StatementContextImpl<V>(reactor, this, childDef(identifier).get(), loc);
        mutableResult().add(childCtx);
        return childCtx;
    }

    @Override
    public String toString() {
        return "Context(" + definition.name() + ")";
    }

    @Override
    public Optional<AxiomItemDefinition> childDef(AxiomIdentifier child) {
        return definition.type().item(child);
    }

    @Override
    public AxiomIdentifier identifier() {
        return definition.name();
    }

    @Override
    public V requireValue() throws AxiomSemanticException {
        return AxiomSemanticException.checkNotNull(mutableResult().value(), definition, "must have argument specified.");
    }

    @Override
    public AxiomItemDefinition definition() {
        return definition;
    }

    @Override
    public void registerAsGlobalItem(AxiomIdentifier typeName) throws AxiomSemanticException {
        reactor.registerGlobalItem(typeName, this);
    }


    @Override
    public <V> StatementContext<V> createEffectiveChild(AxiomIdentifier axiomIdentifier, V value) {
        StatementContextImpl<V> child = createChildNode(axiomIdentifier, null);
        child.setValue(value);
        return child;
    }


    public void registerRule(StatementRuleContextImpl<V> rule) {
        mutableResult().addRule(rule);
        this.reactor.addOutstanding(rule);
    }

    public SourceLocation startLocation() {
        return startLocation;
    }

    ModelReactorContext reactor() {
        return reactor;
    }

    public <V> Optional<StatementContext<V>> firstChild(AxiomItemDefinition child) {
        return Optionals.first(children(child.name()));
    }

    private <V> Collection<StatementContext<V>> children(AxiomIdentifier identifier) {
        return (Collection) mutableResult().get(identifier);
    }

    public void addRule(StatementRule<V> statementRule) throws AxiomSemanticException {
        statementRule.apply(new StatementRuleContextImpl<V>(this,statementRule));
    }

    @Override
    public Optional<V> optionalValue() {
        return Optional.ofNullable(mutableResult().value());
    }

    @Override
    public void replace(Requirement<AxiomStatement<?>> supplier) {
        this.result = (Requirement) supplier;
    }

    @Override
    public StatementContext<?> parent() {
        return parent;
    }

    @Override
    public void setValue(Object value, SourceLocation loc) {
        setValue(value);
        this.valueLocation = loc;
    }

    public Requirement<AxiomStatement<V>> asRequirement() {
        if (result instanceof StatementContextResult) {
            return new Deffered<>(result);
        }
        return result;
    }

    public Supplier<? extends AxiomStatement<?>> asLazy() {
        return Lazy.from(() -> result.get());
    }

    public boolean isSatisfied() {
        return result.isSatisfied();
    }



}
