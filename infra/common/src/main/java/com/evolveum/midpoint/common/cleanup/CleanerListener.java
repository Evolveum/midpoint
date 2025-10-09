/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.cleanup;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismReference;

/**
 * Listener that can be used to react on cleanup events created for items that are marked
 * with action {@link CleanupPathAction#ASK}.
 */
public interface CleanerListener {

    /**
     * Method that allows consumers to react on cleanup event marked with action {@link CleanupPathAction#ASK}.
     *
     * @return true if the item should be removed, false otherwise
     */
    default boolean onConfirmOptionalCleanup(CleanupEvent<Item<?, ?>> event) {
        return true;
    }

    /**
     * Method that allows consumers to clean up references, e.g. oids, filteres, etc.
     *
     * @param event
     */
    default void onReferenceCleanup(CleanupEvent<PrismReference> event) {
        // intentionally empty
    }

    /**
     * Method that allows consumers to decide whether item should be removed
     *
     * @return true if the item should be removed, false otherwise. If null is returned, the default behaviour is used.
     */
    default Boolean onItemCleanup(CleanupEvent<Item<?, ?>> event) {
        return null;
    }
}
