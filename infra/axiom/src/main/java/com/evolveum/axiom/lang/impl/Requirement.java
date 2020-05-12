package com.evolveum.axiom.lang.impl;

import java.util.function.Supplier;

import com.evolveum.axiom.lang.api.stmt.AxiomStatement;


public abstract class Requirement<T> {

    abstract boolean isSatisfied();
    abstract public T get();

    public static <T> Requirement<T> immediate(T value) {
        return new Immediate<>(value);
    }

    public static <T> Requirement<T> from(Supplier<T> supplier) {
        return new Suppliable<>(supplier);
    }

    private static final class Immediate<V> extends Requirement<V> {

        private final V value;

        @Override
        boolean isSatisfied() {
            return true;
        }

        public Immediate(V value) {
            super();
            this.value = value;
        }

        @Override
        public V get() {
            return value;
        }

    }

    private static final class Suppliable<V> extends Requirement<V> {

        private final Supplier<V> value;

        @Override
        boolean isSatisfied() {
            return value.get() != null;
        }

        public Suppliable(Supplier<V> value) {
            super();
            this.value = value;
        }

        @Override
        public V get() {
            return value.get();
        }

    }


}
