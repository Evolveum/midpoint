package com.evolveum.axiom.reactor;

import java.util.function.Function;

public class FlatMapDependency<I,O> extends DelegatedDependency<O> {

    private Dependency delegate;
    private Function<I, Dependency<O>> mapping;

    public FlatMapDependency(Dependency<I> delegate, Function<I, Dependency<O>> mapping) {
        super();
        this.delegate = delegate;
        this.mapping = mapping;
    }

    @Override
    Dependency<O> delegate() {
        if(mapping != null && delegate.isSatisfied()) {
            delegate = mapping.apply((I) delegate.get());
            mapping = null;
        }
        return delegate;
    }

}
