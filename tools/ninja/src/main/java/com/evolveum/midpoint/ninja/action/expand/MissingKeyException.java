/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import java.util.HashSet;
import java.util.Set;

public class MissingKeyException extends RuntimeException {

    private Set<String> keys;

    public MissingKeyException(String key, String message) {
        this(Set.of(key), message);
    }

    public MissingKeyException(Set<String> keys, String message) {
        super(message);

        this.keys = keys;
    }

    public Set<String> getKeys() {
        if (keys == null) {
            keys = new HashSet<>();
        }
        return keys;
    }
}
