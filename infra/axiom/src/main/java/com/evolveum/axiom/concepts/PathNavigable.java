package com.evolveum.axiom.concepts;

import java.util.Collection;
import java.util.Optional;

public interface PathNavigable<V,K,P extends Path<K>> {

    Optional<? extends PathNavigable<V,K,P>> resolve(K key);

    Optional<? extends PathNavigable<V, K, P>> resolve(P key);
}

