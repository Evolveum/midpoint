package com.evolveum.axiom.reactor;

import java.util.Optional;
import java.util.function.Supplier;

import com.evolveum.axiom.lang.impl.RuleErrorMessage;
import com.google.common.base.Preconditions;


public interface Depedency<T> {

    boolean isSatisfied();
    public T get();

    public RuleErrorMessage errorMessage();

    public static <T> Depedency<T> unsatisfied() {
        return new Unsatified<>();
    }

    public static <T> Depedency<T> immediate(T value) {
        return new Immediate<>(value);
    }

    public static <T> Depedency<T> from(Supplier<T> supplier) {
        return new Suppliable<>(supplier);
    }

    public static <T> Depedency<T> deffered(Depedency<T> original) {
        return new Deffered<>(original);
    }

    default Depedency<T> unsatisfiedMessage(Supplier<RuleErrorMessage> unsatisfiedMessage) {
        return this;
    }

    interface Search<T> extends Depedency<T> {

        default Depedency.Search<T> notFound(Supplier<RuleErrorMessage> unsatisfiedMessage) {
            return this;
        }

    }


    public static abstract class Abstract<V> implements Depedency<V> {


        private Supplier<RuleErrorMessage> errorMessage;

        @Override
        public Depedency<V> unsatisfiedMessage(Supplier<RuleErrorMessage> unsatisfiedMessage) {
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

        abstract Depedency<T> delegate();

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

    public final class RetriableDelegate<T> extends Delegated<T> implements Search<T> {

        private Object maybeDelegate;
        private Supplier<RuleErrorMessage> notFound;

        public RetriableDelegate(Supplier<Depedency<T>> lookup) {
            maybeDelegate = lookup;
        }

        @Override
        Depedency<T> delegate() {
            if(maybeDelegate instanceof Depedency<?>) {
                return (Depedency) maybeDelegate;
            }
            if(maybeDelegate instanceof Supplier<?>) {
                Depedency<?> result = ((Supplier<Depedency<?>>) maybeDelegate).get();
                if(result != null) {
                    maybeDelegate = result;
                    return (Depedency) result;
                }

            }
            return unsatisfied();
        }

        @Override
        public Search<T> notFound(Supplier<RuleErrorMessage> unsatisfiedMessage) {
            notFound = unsatisfiedMessage;
            return this;
        }

        @Override
        public RuleErrorMessage errorMessage() {
            if(maybeDelegate instanceof Supplier && notFound != null) {
                return notFound.get();
            }
            return super.errorMessage();
        }

    }

    static <T> Search<T> retriableDelegate(Supplier<Depedency<T>> lookup) {
        return new RetriableDelegate(lookup);
    }

    static <T> Depedency<T> from(Optional<T> maybe) {
        if(maybe.isPresent()) {
            return immediate(maybe.get());
        }
        return unsatisfied();
    }
    static <T> Depedency<T> orNull(T value) {
        if(value != null) {
            return immediate(value);
        }
        return null;
    }
}
