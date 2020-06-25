/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.reactor;

import java.util.Optional;

import com.google.common.base.Preconditions;

class Deffered<T> extends DelegatedDependency<T> {

    private Object ret;

    Deffered(Dependency<T> delegate) {
        ret = delegate;
    }

    @Override
    Dependency<T> delegate() {
        if(ret instanceof Dependency<?>) {
            return (Dependency<T>) ret;
        }
        return null;
    }

    @Override
    public boolean isSatisfied() {
        if(delegate() != null) {
            if(delegate().isSatisfied()) {
                ret = delegate().get();
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public T get() {
        Preconditions.checkState(isSatisfied(), "Requirement was not satisfied");
        return (T) ret;
    }
}
