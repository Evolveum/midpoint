/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
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
