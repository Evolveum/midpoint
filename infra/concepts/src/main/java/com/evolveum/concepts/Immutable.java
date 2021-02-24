/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.concepts;

public interface Immutable extends MutationBehaviourAware<Immutable> {

    /**
     * Always return false.
     */
    @Override
    default boolean mutable() {
        return true;
    }

    static boolean isImmutable(Object object) {
        return KnownImmutables.isImmutable(object);
    }
}
