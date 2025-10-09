/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

/** On what object state is the constraint evaluated? */
public enum ObjectState {

    /** The state before the operation, i.e. `objectOld`. */
    BEFORE,

    /** Expected (or real) state after the operation, i.e. `objectNew`. */
    AFTER
}
