package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItem;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomItemValue;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.DependantAction;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
public class StatementRuleContextImpl<V> implements AxiomStatementRule.Lookup<V>, AxiomStatementRule.ActionBuilder<V>, DependantAction<AxiomSemanticException> {

    private final ValueContext<V> context;
    private final String rule;
    private final List<Dependency<?>> dependencies = new ArrayList<>();
    private AxiomStatementRule.Action<V> action;
    private Supplier<? extends Exception> errorReport = () -> null;
    private boolean applied = false;
    private Exception error;

    public StatementRuleContextImpl(ValueContext<V> context, String rule) {
        this.context = context;
        this.rule = rule;
    }
    @Override
    public Dependency.Search<AxiomItemValue<?>> global(AxiomIdentifier space,
            IdentifierSpaceKey key) {
        return Dependency.retriableDelegate(() -> {
            ValueContext<?> maybe = context.lookup(space, key);
            if(maybe != null) {
                return (Dependency) maybe;
            }
            return null;
        });
    }

    @Override
    public Dependency.Search<AxiomItemValue<?>> namespaceValue(AxiomIdentifier space,
            IdentifierSpaceKey key) {
        return Dependency.retriableDelegate(() -> {
            ValueContext<?> maybe = context.lookup(space, key);
            if(maybe != null) {
                return (Dependency) maybe;
            }
            return null;
        });
    }

    @Override
    public <V,X extends Dependency<V>> X require(X req) {
        this.dependencies.add(req);
        return req;
    }

    @Override
    public StatementRuleContextImpl<V> apply(AxiomStatementRule.Action<V> action) {
        this.action = action;
        context.dependsOnAction(this);
        return this;
    }

    public boolean canProcess() {
        for (Dependency<?> requirement : dependencies) {
            if (!requirement.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void apply() throws AxiomSemanticException {
        if(!applied && error == null) {
            this.action.apply(context);
            this.applied = true;
        }
    }

    public boolean isApplied() {
        return applied;
    }


    @Override
    public Exception errorMessage() {
        if(error != null) {
            return error;
        }

        return errorReport.get();
    }

    @Override
    public String toString() {
        return context.toString() + ":" + rule;
    }

    @Override
    public Dependency<NamespaceContext> namespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId) {
        return (context.rootImpl().requireNamespace(name, namespaceId));
    }

    @Override
    public boolean successful() {
        return applied;
    }

    public Collection<Dependency<?>> requirements() {
        return dependencies;
    }

    @Override
    public void fail(Exception e) throws AxiomSemanticException {
        this.error = e;
    }


    @Override
    public Collection<Dependency<?>> dependencies() {
        return dependencies;
    }

    @Override
    public Optional<AxiomSemanticException> error() {
        return Optional.empty();
    }

    public boolean isDefined() {
        return action != null;
    }

    public Collection<StatementRuleContextImpl<?>> build() {
        if(action != null) {
            return ImmutableList.of(this);
        }
        return Collections.emptyList();
    }

    @Override
    public AxiomItemDefinition itemDefinition() {
        return context.parent().definition();
    }

    @Override
    public Dependency<AxiomValueContext<?>> modify(AxiomIdentifier space, IdentifierSpaceKey key) {
        return (Dependency.retriableDelegate(() -> {
            ValueContext<?> maybe = context.lookup(space, key);
            if(maybe != null) {
                return Dependency.immediate(maybe);
            }
            return null;
        }));
    }

    @Override
    public Dependency<AxiomItemValue<?>> require(AxiomValueContext<?> ext) {
        return require((Dependency<AxiomItemValue<?>>) ext);
    }

    @Override
    public boolean isMutable() {
        return context.isMutable();
    }

    @Override
    public AxiomSemanticException error(String message, Object... arguments) {
        return new AxiomSemanticException(context.startLocation() + Strings.lenientFormat(message, arguments));
    }
    @Override
    public <T> Dependency<AxiomItem<T>> child(AxiomItemDefinition namespace, Class<T> valueType) {
        return (context.requireChild(namespace.name()));
    }
    @Override
    public V currentValue() {
        return context.currentValue();
    }
    @Override
    public Dependency<V> finalValue() {
        return context.map(v -> v.get());
    }
    public String name() {
        return rule;
    }

}
