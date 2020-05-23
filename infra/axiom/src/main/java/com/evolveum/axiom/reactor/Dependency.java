package com.evolveum.axiom.reactor;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;


public interface Dependency<T> {

    default boolean isRequired() {
        return true;
    }
    boolean isSatisfied();
    public T get();

    public Exception errorMessage();

    public static <T> Dependency<T> unsatisfied() {
        return new Unsatified<>();
    }

    public static <T> Dependency<T> immediate(T value) {
        return new Immediate<>(value);
    }

    public static <T> Dependency<T> optional(Dependency<T> dependency) {
        return new OptionalDep<T>(dependency);
    }

    public static <T> Dependency<T> from(Supplier<T> supplier) {
        return new Suppliable<>(supplier);
    }

    public static <T> Dependency<T> deffered(Dependency<T> original) {
        return new Deffered<>(original);
    }

    default Dependency<T> unsatisfied(Supplier<? extends Exception> unsatisfiedMessage) {
        return this;
    }

    interface Search<T> extends Dependency<T> {

        default Dependency.Search<T> notFound(Supplier<? extends Exception> unsatisfiedMessage) {
            return this;
        }

    }

    public static abstract class Abstract<V> implements Dependency<V> {


        private Supplier<? extends Exception> errorMessage;

        @Override
        public Dependency<V> unsatisfied(Supplier<? extends Exception> unsatisfiedMessage) {
            errorMessage = unsatisfiedMessage;
            return this;
        }


        @Override
        public Exception errorMessage() {
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

        abstract Dependency<T> delegate();

        @Override
        public boolean isSatisfied() {
            return delegate().isSatisfied();
        }

        @Override
        public boolean isRequired() {
            return delegate().isRequired();
        }

        @Override
        public T get() {
            Preconditions.checkState(isSatisfied(), "Requirement was not satisfied");
            return delegate().get();
        }
    }

    public final class OptionalDep<T> extends Delegated<T> {

        private final Dependency<T> delegate;

        public OptionalDep(Dependency<T> delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public boolean isRequired() {
            return false;
        }

        @Override
        Dependency<T> delegate() {
            return delegate;
        }

    }

    public final class RetriableDelegate<T> extends Delegated<T> implements Search<T> {

        private Object maybeDelegate;
        private Supplier<? extends Exception> notFound;

        public RetriableDelegate(Supplier<Dependency<T>> lookup) {
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

    static <T> Search<T> retriableDelegate(Supplier<Dependency<T>> lookup) {
        return new RetriableDelegate(lookup);
    }

    static <T> Dependency<T> from(Optional<T> maybe) {
        if(maybe.isPresent()) {
            return immediate(maybe.get());
        }
        return unsatisfied();
    }
    static <T> Dependency<T> orNull(T value) {
        if(value != null) {
            return immediate(value);
        }
        return null;
    }
    static boolean allSatisfied(Collection<? extends Dependency<?>> outstanding) {
        for (Dependency<?> dependency : outstanding) {
            if(!dependency.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    default <R> Dependency<R> map(Function<T,R> map) {
        return new Suppliable<R>(() -> {
            if(isSatisfied()) {
                return map.apply(get());
            }
            return null;
        });
    }

    default <R> Dependency<R> flatMap(Function<T,Dependency<R>> map) {
        return new RetriableDelegate<R>(() -> {
            if(isSatisfied()) {
                return map.apply(get());
            }
            return null;
        });
    }

}
