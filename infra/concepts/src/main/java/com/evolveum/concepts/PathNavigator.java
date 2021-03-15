package com.evolveum.concepts;

import java.util.Optional;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import com.evolveum.concepts.func.FailableBiFunction;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;


public interface PathNavigator<N,S> {

    @Nullable N navigate(N node, S selector);

    default boolean isReturn(S selector) {
        return false;
    }

    static <N,S> PathNavigator<N,S> from(PathNavigator<N, S> object) {
        return object;
    }

    static <N,S> PathNavigator<N, S> fromNullable(BiFunction<N, S, N> function) {
        return function::apply;
    }

    static <N,S> PathNavigator<N, S> fromOptionable(BiFunction<N, S, Optional<N>> function) {
        return (n,s) -> function.apply(n, s).orElse(null);
    }

    static <N,S,E extends Exception> PathNavigator<N, S> fromFailable(Class<E> notFound, FailableBiFunction<N,S,N,E> function) {
        return (n,s) -> {
            try {
                return function.apply(n, s);
            } catch (Exception e) {
                if(notFound.isInstance(e)) {
                    return null;
                }
                Throwables.throwIfUnchecked(e);
                // Should not happen, but here to make compiler happy
                throw new IllegalStateException("function throwed unhandled checked exception",e);
            }
        };
    }

    default PathNavigable<N, S> startIn(N node) {
        return PathNavigable.from(node, this);
    }

    default PathNavigable<N, S> toNavigable(N node) {
        return PathNavigable.from(node, this);
    }

    default PathNavigator<N,S> withReturnSegment(S returnSegment) {
        return new PathNavigator<N, S>() {

            @Override
            public @Nullable N navigate(N node, S selector) {
                return PathNavigator.this.navigate(node, selector);
            }

            @Override
            public boolean isReturn(S selector) {
                return Objects.equal(returnSegment, selector);
            }
        };
    }

    default PathNavigable.Hierarchical<N, S> toNavigable(N tree, @SuppressWarnings("unchecked") N... nested) {
        return PathNavigable.hierarchy(this, ImmutableList.<N>builder().add(tree).add(nested).build());
    }

}
