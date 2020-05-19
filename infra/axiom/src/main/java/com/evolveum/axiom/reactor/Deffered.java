package com.evolveum.axiom.reactor;

import java.util.Optional;

import com.google.common.base.Preconditions;

class Deffered<T> extends Depedency.Delegated<T> {

    private Object ret;

    Deffered(Depedency<T> delegate) {
        ret = delegate;
    }

    @Override
    Depedency<T> delegate() {
        if(ret instanceof Depedency<?>) {
            return (Depedency<T>) ret;
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
