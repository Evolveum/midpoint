/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
