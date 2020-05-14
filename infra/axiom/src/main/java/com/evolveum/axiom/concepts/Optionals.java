package com.evolveum.axiom.concepts;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class Optionals {

    public static <V> Optional<V>first(Collection<V> collection) {
        if(collection.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(collection.iterator().next());
    }

}
