package com.evolveum.axiom.concepts;

import java.util.Optional;

public interface Navigable<K, N extends Navigable<K,N>> {

    Optional<? extends N> resolve(K key);

}
