package com.evolveum.concepts;

import java.util.List;

import com.google.common.collect.ImmutableList;

public interface PathNavigable<V,S> {

    PathNavigation<V, S> resolve(Iterable<S> path);

    static <N,S> PathNavigable<N, S> from(N node, PathNavigator<N, S> pathNavigator) {
        return path -> {
                var nav = PathNavigation.stackedFromRoot(node, ImmutableList.copyOf(path));
                return nav.navigate(pathNavigator);
        };
    }

    static <N,S> PathNavigable.Hierarchical<N, S> hierarchy(PathNavigator<N, S> pathNavigator, List<N> parents) {
        return path -> {
            var nav = PathNavigation.stackedFromParents(parents, path);
            return nav.navigate(pathNavigator);
        };
    }

    interface Hierarchical<V,S> extends PathNavigable<V, S> {

        @Override
        PathNavigation.Stacked<V, S> resolve(Iterable<S> path);

    }
}
