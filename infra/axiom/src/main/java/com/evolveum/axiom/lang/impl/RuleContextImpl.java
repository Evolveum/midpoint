package com.evolveum.axiom.lang.impl;

import java.util.Collection;

import com.evolveum.axiom.reactor.Rule;

public class RuleContextImpl implements Rule<ValueContext<?>, StatementRuleContextImpl<?>> {

    private final AxiomStatementRule delegate;

    public RuleContextImpl(AxiomStatementRule delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public boolean applicableTo(ValueContext<?> context) {
        return delegate.isApplicableTo(context.parent().definition());
    }

    @Override
    public Collection<StatementRuleContextImpl<?>> applyTo(ValueContext<?> context) {
        StatementRuleContextImpl<?> actionBuilder = context.addAction(delegate.name());
        delegate.apply(actionBuilder, actionBuilder);
        return actionBuilder.build();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public AxiomStatementRule<?> delegate() {
        return delegate;
    }

}
