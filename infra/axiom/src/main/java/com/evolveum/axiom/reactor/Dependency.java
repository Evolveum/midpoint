package com.evolveum.axiom.reactor;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


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

    public static final class Immediate<V> extends AbstractDependency<V> {

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

    public static final class Suppliable<V> extends AbstractDependency<V> {

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

    public static final class Unsatified<V>  extends AbstractDependency<V> {

        @Override
        public boolean isSatisfied() {
            return false;
        }

        @Override
        public V get() {
            throw new IllegalStateException("Requirement not satisfied");
        }
    }

    public final class OptionalDep<T> extends DelegatedDependency<T> {

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

    static <T> Search<T> retriableDelegate(Supplier<Dependency<T>> lookup) {
        return new RetriableDependency(lookup);
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
        return new MapDependency<>(this, map);
    }

    default <R> Dependency<R> flatMap(Function<T,Dependency<R>> map) {
        return new FlatMapDependency<>(this, map);
    }
    static <T> Dependency<T> of(T from) {
        return Dependency.immediate(from);
    }

}
