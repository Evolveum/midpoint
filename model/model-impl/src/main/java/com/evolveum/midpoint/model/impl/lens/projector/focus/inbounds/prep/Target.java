/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * The target i.e. the focus into which the output of mappings will be put.
 *
 * @param <F> type of the object
 */
abstract class Target<F extends FocusType> {

    /**
     * Current focus.
     *
     * - For pre-mappings this will be the empty object.
     * - TODO for clockwork: should we use current or new?
     */
    final PrismObject<F> focus;

    /** Focus definition. */
    @NotNull final PrismObjectDefinition<F> focusDefinition;

    Target(PrismObject<F> focus, @NotNull PrismObjectDefinition<F> focusDefinition) {
        this.focus = focus;
        this.focusDefinition = focusDefinition;
    }

    /**
     * Returns true if the focus object is being deleted. Not applicable to pre-mappings.
     */
    abstract boolean isFocusBeingDeleted();
}
