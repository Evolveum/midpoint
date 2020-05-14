package com.evolveum.axiom.lang.impl;

import java.util.Optional;

import com.google.common.base.Preconditions;

class Deffered<T> extends Requirement.Delegated<T> {

    private Object ret;

    Deffered(Requirement<T> delegate) {
        ret = delegate;
    }

    @Override
    Requirement<T> delegate() {
        if(ret instanceof Requirement<?>) {
            return (Requirement<T>) ret;
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
