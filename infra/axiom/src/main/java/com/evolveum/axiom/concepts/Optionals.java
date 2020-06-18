/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
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
