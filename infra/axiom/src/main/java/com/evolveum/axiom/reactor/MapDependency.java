package com.evolveum.axiom.reactor;

import java.util.function.Function;

public class MapDependency<I,O> extends DelegatedDependency<O> {

    private Dependency<I> delegate;
    private Function<I, O> mapping;

    public MapDependency(Dependency<I> delegate, Function<I, O> mapping) {
        super();
        this.delegate = delegate;
        this.mapping = mapping;
    }

    @Override
    Dependency delegate() {
        return delegate;
    }

    @Override
    public O get() {
        return mapping.apply(delegate.get());
    }

}
