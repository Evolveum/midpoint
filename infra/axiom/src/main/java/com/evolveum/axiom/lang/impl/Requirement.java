package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.lang.api.stmt.AxiomStatement;
import com.google.common.base.Preconditions;


public interface Requirement<T> {

    boolean isSatisfied();
    public T get();

    public RuleErrorMessage errorMessage();

    public static <T> Requirement<T> unsatisfied() {
        return new Unsatified<>();
    }

    public static <T> Requirement<T> immediate(T value) {
        return new Immediate<>(value);
    }

    public static <T> Requirement<T> from(Supplier<T> supplier) {
        return new Suppliable<>(supplier);
    }

    public static abstract class Abstract<V> implements Requirement<V> {


        private Supplier<RuleErrorMessage> errorMessage;

        @Override
        public Requirement<V> unsatisfiedMessage(Supplier<RuleErrorMessage> unsatisfiedMessage) {
            errorMessage = unsatisfiedMessage;
            return this;
        }


        @Override
        public RuleErrorMessage errorMessage() {
            if(errorMessage != null) {
                return errorMessage.get();
            }
            return null;
        }
    }


    public static final class Immediate<V> extends Abstract<V> {

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

    public static final class Suppliable<V> extends Abstract<V> {

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

    public static final class Unsatified<V>  extends Abstract<V> {

        @Override
        public boolean isSatisfied() {
            return false;
        }

        @Override
        public V get() {
            throw new IllegalStateException("Requirement not satisfied");
        }
    }

    public abstract class Delegated<T>  extends Abstract<T>  {

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

    default Requirement<T> unsatisfiedMessage(Supplier<RuleErrorMessage> unsatisfiedMessage) {
        return this;
    }
    static <T> Requirement<T> from(Optional<T> maybe) {
        if(maybe.isPresent()) {
            return immediate(maybe.get());
        }
        return unsatisfied();
    }
}
