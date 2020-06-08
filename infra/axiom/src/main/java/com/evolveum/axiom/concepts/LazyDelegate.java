package com.evolveum.axiom.concepts;

public abstract class LazyDelegate<T> extends AbstractLazy<T> {

    public LazyDelegate(Lazy.Supplier<T> supplier) {
        super(supplier);
    }

    protected T delegate() {
        return unwrap();
    }
}
