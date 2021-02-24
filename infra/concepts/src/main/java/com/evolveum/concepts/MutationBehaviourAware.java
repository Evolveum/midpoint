/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.concepts;

/**
 *
 * @author tony
 *
 * @param <M> Final version of mutation behaviour
 *
 * @see Immutable
 * @see Mutable
 * @see Freezable
 */
public interface MutationBehaviourAware<M extends MutationBehaviourAware<M>> {

    /**
     * Return true if object is currently mutable (can change publicly visible state)
     * @return true if object is currently mutable
     */
    boolean mutable();
}
