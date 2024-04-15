/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

/**
 * Cleanup action to be taken when a path is encountered.
 */
public enum CleanupPathAction {

    /**
     * Item is removed from the object.
     */
    REMOVE,

    /**
     * Item is not removed from the object.
     */
    IGNORE,

    /**
     * Consumer is asked what to do.
     *
     * @see CleanerListener
     * @see ObjectCleaner#setListener(CleanerListener)
     */
    ASK;
}
