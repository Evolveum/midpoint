package com.evolveum.axiom.lang.impl;

import java.util.Collection;

import com.evolveum.axiom.reactor.Rule;

public class RuleContextImpl implements Rule<StatementContextImpl<?>, StatementRuleContextImpl<?>> {

    private final StatementRule delegate;

    public RuleContextImpl(StatementRule delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public boolean applicableTo(StatementContextImpl<?> context) {
        return delegate.isApplicableTo(context.definition());
    }

    @Override
    public Collection<StatementRuleContextImpl<?>> applyTo(StatementContextImpl<?> context) {
        StatementRuleContextImpl<?> actionBuilder = context.addRule(delegate);
        delegate.apply(actionBuilder);
        return actionBuilder.build();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public StatementRule<?> delegate() {
        return delegate;
    }

}
