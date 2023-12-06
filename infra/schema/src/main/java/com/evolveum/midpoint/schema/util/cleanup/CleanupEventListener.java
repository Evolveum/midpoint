/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cleanup;

/**
 * Listener that can be used to react on cleanup events created for items that are marked
 * with action {@link CleanupPathAction#ASK}.
 */
@FunctionalInterface
public interface CleanupEventListener {

    /**
     * Method that allows consumers to react on cleanup event marked with action {@link CleanupPathAction#ASK}.
     *
     * @return true if the item should be removed, false otherwise
     */
    boolean onCleanupItem(CleanupEvent event);
}
