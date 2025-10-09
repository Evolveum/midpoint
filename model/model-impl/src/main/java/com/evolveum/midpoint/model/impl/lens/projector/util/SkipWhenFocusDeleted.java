/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.util;

/**
 * Should we skip the processing if the focus is going to be deleted?
 */
public enum SkipWhenFocusDeleted {

    /**
     * No. Processor should be always executed.
     */
    NONE,

    /**
     * Yes. Processor should be skipped if the primary delta is DELETE.
     * TODO Do we really need this? It was created by inspecting existing code.
     *  Maybe NONE + PRIMARY_OR_SECONDARY (i.e. simple false/true flag) is sufficient.
     */
    PRIMARY,

    /**
     * Yes. Processor should be skipped if the primary or secondary delta is DELETE.
     */
    PRIMARY_OR_SECONDARY
}
