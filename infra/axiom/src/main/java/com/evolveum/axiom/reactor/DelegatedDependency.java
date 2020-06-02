package com.evolveum.axiom.reactor;

import com.google.common.base.Preconditions;

public abstract class DelegatedDependency<T>  extends AbstractDependency<T>  {

    abstract Dependency<T> delegate();

    @Override
    public boolean isSatisfied() {
        return delegate().isSatisfied();
    }

    @Override
    public boolean isRequired() {
        return delegate().isRequired();
    }

    @Override
    public T get() {
        Preconditions.checkState(isSatisfied(), "Requirement was not satisfied");
        return delegate().get();
    }

    @Override
    public Exception errorMessage() {
        Exception maybe = super.errorMessage();
        return maybe != null ? maybe : delegate().errorMessage();
    }
}
