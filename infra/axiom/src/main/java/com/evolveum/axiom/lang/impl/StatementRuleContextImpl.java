package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.sun.net.httpserver.Authenticator.Result;
public class StatementRuleContextImpl<V> implements StatementRuleContext<V> {

    private final StatementContextImpl<V> context;
    private final StatementRule<V> rule;
    private final List<Requirement<?>> requirements = new ArrayList<>();
    private Action<V> action;
    private Supplier<RuleErrorMessage> errorReport = () -> null;
    private boolean applied = false;

    public StatementRuleContextImpl(StatementContextImpl<V> context, StatementRule<V> rule) {
        this.context = context;
        this.rule = rule;
    }

    public StatementRule<V> rule() {
        return rule;
    }

    @Override
    public <V> Optional<V> optionalChildValue(AxiomItemDefinition child, Class<V> type) {
        return (Optional) context.firstChild(child).flatMap(v -> v.optionalValue());
    }

    @Override
    public Requirement<AxiomStatement<?>> requireGlobalItem(AxiomIdentifier space,
            IdentifierSpaceKey key) {
        return requirement(context.reactor().requireGlobalItem(space, key));
    }

    private <V> Requirement<V> requirement(Requirement<V> req) {
        this.requirements.add(req);
        return req;
    }

    @Override
    public StatementRuleContextImpl<V> apply(Action<V> action) {
        this.action = action;
        context.registerRule(this);
        return this;
    }

    public boolean canProcess() {
        for (Requirement<?> requirement : requirements) {
            if (!requirement.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    public void perform() throws AxiomSemanticException {
        this.action.apply(context);
        this.applied = true;
    }

    @Override
    public <V> V requiredChildValue(AxiomItemDefinition supertypeReference, Class<V> type)
            throws AxiomSemanticException {
        StatementContext ctx = context.firstChild(supertypeReference).get();
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
    public StatementRuleContext<V> errorMessage(Supplier<RuleErrorMessage> errorFactory) {
        this.errorReport = errorFactory;
        return this;
    }

    RuleErrorMessage errorMessage() {
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
    public Requirement<AxiomStatement<?>> requireChild(AxiomItemDefinition required) {
        return context.requireChild(required);
    }

}
