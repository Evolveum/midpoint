package com.evolveum.axiom.reactor;

import java.util.function.Supplier;

import com.evolveum.axiom.reactor.Dependency.Search;

public final class RetriableDependency<T> extends DelegatedDependency<T> implements Search<T> {

    private Object maybeDelegate;
    private Supplier<? extends Exception> notFound;

    public RetriableDependency(Supplier<Dependency<T>> lookup) {
        maybeDelegate = lookup;
    }

    @Override
    Dependency<T> delegate() {
        if(maybeDelegate instanceof Dependency<?>) {
            return (Dependency) maybeDelegate;
        }
        if(maybeDelegate instanceof Supplier<?>) {
            Dependency<?> result = ((Supplier<Dependency<?>>) maybeDelegate).get();
            if(result != null) {
                maybeDelegate = result;
                return (Dependency) result;
            }
        }
        return Dependency.unsatisfied();
    }

    @Override
    public Search<T> notFound(Supplier<? extends Exception> unsatisfiedMessage) {
        notFound = unsatisfiedMessage;
        return this;
    }

    @Override
    public Exception errorMessage() {
        if(maybeDelegate instanceof Supplier && notFound != null) {
            return notFound.get();
        }
        Exception maybeFound = super.errorMessage();
        if(maybeFound == null && maybeDelegate instanceof Dependency<?>) {
            maybeFound = ((Dependency<?>)maybeDelegate).errorMessage();
        }
        return maybeFound;
    }
}
