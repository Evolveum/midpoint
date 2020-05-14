package com.evolveum.axiom.lang.impl;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.base.Preconditions;


public interface Requirement<T> {

    static final Requirement UNSATISFIED = new Unsatified<>();

    boolean isSatisfied();
    public T get();

    public static <T> Requirement<T> unsatisfied() {
        return UNSATISFIED;
    }

    public static <T> Requirement<T> immediate(T value) {
        return new Immediate<>(value);
    }

    public static <T> Requirement<T> from(Supplier<T> supplier) {
        return new Suppliable<>(supplier);
    }

    public static final class Immediate<V> implements Requirement<V> {

        private final V value;

        @Override
        public boolean isSatisfied() {
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

    public static final class Suppliable<V> implements Requirement<V> {

        private final Supplier<V> value;

        @Override
        public boolean isSatisfied() {
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

    public static final class Unsatified<V> implements Requirement<V> {

        @Override
        public boolean isSatisfied() {
            return false;
        }

        @Override
        public V get() {
            throw new IllegalStateException("Requirement not satisfied");
        }
    }

    public abstract class Delegated<T> implements Requirement<T> {

        abstract Requirement<T> delegate();

        @Override
        public boolean isSatisfied() {
            return delegate().isSatisfied();
        }

        @Override
        public T get() {
            Preconditions.checkState(isSatisfied(), "Requirement was not satisfied");
            return delegate().get();
        }
    }

    public final class RetriableDelegate<T> extends Delegated<T> {

        Object maybeDelegate;

        public RetriableDelegate(Supplier<Requirement<T>> lookup) {
            maybeDelegate = lookup;
        }

        @Override
        Requirement<T> delegate() {
            if(maybeDelegate instanceof Requirement<?>) {
                return (Requirement) maybeDelegate;
            }
            if(maybeDelegate instanceof Supplier<?>) {
                Requirement<?> result = ((Supplier<Requirement<?>>) maybeDelegate).get();
                if(result != null) {
                    maybeDelegate = result;
                    return (Requirement) result;
                }

            }
            return unsatisfied();
        }

    }

    static <T> Requirement<T> retriableDelegate(Supplier<Requirement<T>> lookup) {
        return new RetriableDelegate(lookup);
    }


}
