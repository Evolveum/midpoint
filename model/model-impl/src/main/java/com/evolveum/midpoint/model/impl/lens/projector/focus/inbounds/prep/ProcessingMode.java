/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

/**
 * How should we process given {@link MappedItem} ?
 *
 * TODO find a better name for the enum and its values
 */
enum ProcessingMode {

    /**
     * We will go from a priori delta ("relative mode"). This usually happens when a delta comes from a sync notification
     * or if there is a primary projection delta. Or a delta was computed in previous waves.
     *
     * We also will use the absolute state (full shadow), if it's known. We won't explicitly load it if it is missing.
     */
    A_PRIORI_DELTA,

    /** We will go from the absolute state - but only if it's known. So no loading just for this. */
    ABSOLUTE_STATE_IF_KNOWN,

    /** Mapping(s) will not be processed. */
    NONE
}
