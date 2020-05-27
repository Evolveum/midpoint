package com.evolveum.axiom.lang.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.reactor.Dependency;
import com.evolveum.axiom.reactor.DependantAction;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
public class ValueActionImpl<V> implements AxiomStatementRule.ActionBuilder<V>, DependantAction<AxiomSemanticException> {

    private final ValueContext<V> context;
    private final String rule;
    private final List<Dependency<?>> dependencies = new ArrayList<>();
    private AxiomStatementRule.Action<V> action;
    private Supplier<? extends Exception> errorReport = () -> null;
    private boolean applied = false;
    private Exception error;

    public ValueActionImpl(ValueContext<V> context, String rule) {
        this.context = context;
        this.rule = rule;
    }



    @Override
    public <V,X extends Dependency<V>> X require(X req) {
        this.dependencies.add(req);
        return req;
    }

    @Override
    public ValueActionImpl<V> apply(AxiomStatementRule.Action<V> action) {
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

    public Collection<ValueActionImpl<?>> build() {
        if(action != null) {
            return ImmutableList.of(this);
        }
        return Collections.emptyList();
    }



    @Override
    public Dependency<AxiomValue<?>> require(AxiomValueContext<?> ext) {
        return require((Dependency<AxiomValue<?>>) ext);
    }



    @Override
    public AxiomSemanticException error(String message, Object... arguments) {
        return AxiomSemanticException.create(context.startLocation(), message, arguments);
    }

    public String name() {
        return rule;
    }


}
