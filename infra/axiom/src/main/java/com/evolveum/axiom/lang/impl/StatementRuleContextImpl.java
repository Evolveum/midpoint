package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.impl.AxiomStatementRule.Context;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.DependantAction;
import com.google.common.collect.ImmutableList;
public class StatementRuleContextImpl<V> implements AxiomStatementRule.Context<V>, DependantAction<AxiomSemanticException> {

    private final StatementContextImpl<V> context;
    private final String rule;
    private final List<Dependency<?>> dependencies = new ArrayList<>();
    private AxiomStatementRule.Action<V> action;
    private Supplier<RuleErrorMessage> errorReport = () -> null;
    private boolean applied = false;
    private Exception error;

    public StatementRuleContextImpl(StatementContextImpl<V> context, String rule) {
        this.context = context;
        this.rule = rule;
    }

    @Override
    public <V> Optional<V> optionalChildValue(AxiomItemDefinition child, Class<V> type) {
        return (Optional) context.firstChild(child).flatMap(v -> v.optionalValue());
    }

    @Override
    public Dependency.Search<AxiomStatement<?>> requireGlobalItem(AxiomIdentifier space,
            IdentifierSpaceKey key) {
        return requirement(Dependency.retriableDelegate(() -> {
            StatementContextImpl<?> maybe = context.lookup(space, key);
            if(maybe != null) {
                return (Dependency) maybe.asRequirement();
            }
            return null;
        }));
    }

    private <V,X extends Dependency<V>> X requirement(X req) {
        this.dependencies.add(req);
        return req;
    }

    @Override
    public StatementRuleContextImpl<V> apply(AxiomStatementRule.Action<V> action) {
        this.action = action;
        context.registerRule(this);
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

    @Override
    public <V> V requiredChildValue(AxiomItemDefinition supertypeReference, Class<V> type)
            throws AxiomSemanticException {
        AxiomStatementContext ctx = context.firstChild(supertypeReference).get();
        return (V) ctx.requireValue(type);
    }

    @Override
    public V requireValue() throws AxiomSemanticException {
        return context.requireValue();
    }

    public boolean isApplied() {
        return applied;
    }

    @Override
    public AxiomStatementRule.Context<V> errorMessage(Supplier<RuleErrorMessage> errorFactory) {
        this.errorReport = errorFactory;
        return this;
    }

    RuleErrorMessage errorMessage() {
        if(error != null) {
            return RuleErrorMessage.from(context.startLocation(), error.getMessage());
        }

        return errorReport.get();
    }

    @Override
    public String toString() {
        return context.toString() + ":" + rule;
    }

    @Override
    public AxiomTypeDefinition typeDefinition() {
        return context.definition().type();
    }

    @Override
    public Optional<V> optionalValue() {
        return context.optionalValue();
    }

    @Override
    public RuleErrorMessage error(String format, Object... arguments) {
        return RuleErrorMessage.from(context.startLocation(), format, arguments);
    }

    @Override
    public Dependency<AxiomStatement<?>> requireChild(AxiomItemDefinition required) {
        return requirement(context.requireChild(required));
    }

    public Dependency<AxiomStatement<?>> optionalChild(AxiomItemDefinition required) {
        return requirement(context.optionalChild(required));
    }

    @Override
    public Dependency<NamespaceContext> requireNamespace(AxiomIdentifier name, IdentifierSpaceKey namespaceId) {
        return requirement(context.root().requireNamespace(name, namespaceId));
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
        return context.definition();
    }

    @Override
    public Dependency<AxiomStatementContext<?>> modify(AxiomIdentifier space, IdentifierSpaceKey key) {
        return requirement(Dependency.retriableDelegate(() -> {
            StatementContextImpl<?> maybe = context.lookup(space, key);
            if(maybe != null) {
                return Dependency.immediate(maybe);
            }
            return null;
        }));
    }

    @Override
    public Dependency<AxiomStatement<?>> require(AxiomStatementContext<?> ext) {
        return requirement((Dependency) impl(ext).asRequirement());
    }

    private StatementContextImpl<?> impl(AxiomStatementContext<?> ext) {
        return (StatementContextImpl<?>) ext;
    }

    @Override
    public Context<V> newAction() {
        return new StatementRuleContextImpl<V>(context, rule);
    }

}
