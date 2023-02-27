/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

/** On what object state is the constraint evaluated? */
public enum ObjectState {

    /** The state before the operation, i.e. `objectOld`. */
    BEFORE,

    /** Expected (or real) state after the operation, i.e. `objectNew`. */
    AFTER
}
