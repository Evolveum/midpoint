package com.evolveum.concepts;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.errorprone.annotations.ForOverride;

public abstract class PathNavigation<V, S> {

    protected final int nextSegment;

    public PathNavigation(int nextSegment) {
        this.nextSegment = nextSegment;
    }

    public static <V,S> PathNavigation<V, S> from(V root, List<S> segments) {
        return new Simple<>(root, segments,0);
    }

    @SafeVarargs
    public static <V,S> PathNavigation<V, S> of(V root, S... segments) {
        return from(root, ImmutableList.copyOf(segments));
    }

    public PathNavigation<V, S> navigate(PathNavigator<V, S> navigation) {
        PathNavigation<V, S> current = this;
        while (current.hasUnresolvedSegment()) {
            S segment = current.unresolvedSegment();
            PathNavigation<V, S> next = current.resolveNext(navigation, segment);
            if( next == current) {
                // We were unable to resolve next
                break;
            }
            current = next;
        }
        return current;
    }

    @ForOverride
    protected PathNavigation<V, S> resolveNext(PathNavigator<V, S> navigation, S segment) {

        @Nullable
        V value = navigation.navigate(lastFound(), segment);
        if (value != null) {
            return newFound(value, nextSegment + 1);
        }
        return this;
    }

    public abstract @NotNull V lastFound();

    public boolean isFound() {
        return !hasUnresolvedSegment();
    }

    public V get() {
        Preconditions.checkState(isFound(), "Result was not found.");
        return lastFound();
    }

    public abstract List<S> segments();

    protected S unresolvedSegment() {
        return segments().get(nextSegment);
    }


    public List<S> resolvedSegments() {
        return segments().subList(0, nextSegment);
    }

    public List<S> unresolvedSegments() {
        if (hasUnresolvedSegment()) {
            return segments().subList(nextSegment, segments().size());
        }
        return ImmutableList.of();
    }

    protected abstract PathNavigation<V, S>  newFound(V next, int nextSegment);

    protected boolean hasUnresolvedSegment() {
        return nextSegment < segments().size();
    }

    private static class Simple<V, S> extends PathNavigation<V, S> {

        private @NotNull V current;
        private List<S> segments;

        public Simple(V current, List<S> segments,int nextSegment) {
            super(nextSegment);
            this.segments = ImmutableList.copyOf(segments);
            this.current = current;
        }

        @Override
        protected PathNavigation<V, S> newFound(V next, int nextSegment) {
            return new Simple<>(next, segments, nextSegment);
        }

        @Override
        public @NotNull V lastFound() {
            return current;
        }

        @Override
        public List<S> segments() {
            return segments;
        }
    }

    public abstract static class Stacked<V,S> extends PathNavigation<V, S> {

        Stacked(int nextSegment) {
            super(nextSegment);
        }

        @Override
        public PathNavigation.Stacked<V, S> navigate(PathNavigator<V, S> navigation) {
            return (PathNavigation.Stacked<V, S>) super.navigate(navigation);
        }

        @Override
        protected PathNavigation<V, S>  resolveNext(PathNavigator<V, S> navigation, S segment) {
            if (navigation.isReturn(segment)) {
                Stacked<V,S> previous = previous();
                if (previous != null) {
                    return previous.asNext(nextSegment + 1);
                }
            }
            return super.resolveNext(navigation, segment);
        }

        protected abstract PathNavigation<V, S> asNext(int nextSegment);

        protected abstract Stacked<V, S> previous();

        @Override
        protected PathNavigation<V, S> newFound(V next, int nextSegment) {
            return new Nested<>(this, next, nextSegment);
        }

        public List<V> allResolved() {
            Builder<V> builder = ImmutableList.builder();
            addSegments(builder);
            return builder.build();
        }

        @ForOverride
        protected void addSegments(Builder<V> builder) {
            if(previous() != null) {
                previous().addSegments(builder);
            }
            builder.add(lastFound());
        }
    }


    private static class Root<V,S> extends Stacked<V, S> {

        private final V current;
        private final List<S> segments;

        public Root(V current, List<S> segments, int nextSegment) {
            super(nextSegment);
            this.segments = segments;
            this.current = current;
        }

        @Override
        public List<S> segments() {
            return segments;
        }

        @Override
        protected Stacked<V, S> previous() {
            return null;
        }

        @Override
        protected PathNavigation<V, S> asNext(int nextSegment) {
            return new Root<>(current, segments, nextSegment);
        }

        @Override
        public @NotNull V lastFound() {
            return current;
        }

    }

    private static class Nested<V,S> extends Stacked<V,S> {

        private final Stacked<V, S> previous;
        private final V current;

        public Nested(Stacked<V,S> previous, V current, int nextSegment) {
            super(nextSegment);
            this.previous = previous;
            this.current = current;
        }

        @Override
        public List<S> segments() {
            return previous.segments();
        }

        @Override
        protected Stacked<V, S> previous() {
            return previous;
        }

        @Override
        protected PathNavigation<V, S> asNext(int nextSegment) {
            return new Nested<>(previous, current, nextSegment);
        }

        @Override
        public @NotNull V lastFound() {
            return current;
        }

    }

    private static class ParentStack<V,S> extends Stacked<V, S> {

        private List<V> parents;
        private List<S> segments;
        private int parent;

        public ParentStack(List<V> parents, List<S> segments, int nextSegment) {
            this(parents, segments, nextSegment, parents.size() - 1);
        }


        public ParentStack(List<V> parents, List<S> segments, int nextSegment, int parent) {
            super(nextSegment);
            this.parents = parents;
            this.segments = segments;
            this.parent = parent;
        }


        @Override
        public @NotNull V lastFound() {
            return parents.get(parent);
        }

        @Override
        protected Stacked<V, S> previous() {
            return new ParentStack<>(parents, segments, nextSegment, parent - 1);
        }

        @Override
        public List<S> segments() {
            return segments;
        }

        @Override
        protected PathNavigation<V, S> asNext(int nextSegment) {
            return new ParentStack<>(parents, segments, nextSegment, parent);
        }

        @Override
        protected void addSegments(Builder<V> builder) {
            builder.addAll(parents.subList(0, parent + 1));
        }

    }


    public static <V,S> PathNavigation.Stacked<V, S> stackedFromRoot(V node, Iterable<S> path) {
        return new Root<>(node, ImmutableList.copyOf(path), 0);
    }

    public static <V,S> PathNavigation.Stacked<V, S> stackedFromParents(List<V> parents, Iterable<S> path) {
        return new ParentStack<>(parents, ImmutableList.copyOf(path),0);
    }
}
