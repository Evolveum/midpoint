/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
