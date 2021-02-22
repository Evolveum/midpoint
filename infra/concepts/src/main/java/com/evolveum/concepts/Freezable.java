/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.concepts;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;

public interface Freezable extends MutationBehaviourAware<Freezable> {


    /**
     *
     * @return true if object is mutable (was not frozen)
     */
    @Override
    boolean mutable();

    /**
     * Freezes object, all subsequent modifications will result in error.
     *
     * @return This (for fluent API)
     */
    @NotNull Freezable freeze();

    /**
     * Checks if object is not frozen
     *
     * @throws IllegalStateException If object is frozen
     */
    default void checkMutable() throws IllegalStateException {
        Preconditions.checkState(mutable(),"Object is frozen");
    }

}
