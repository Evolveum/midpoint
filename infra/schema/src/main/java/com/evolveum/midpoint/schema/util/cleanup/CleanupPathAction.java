package com.evolveum.midpoint.schema.util.cleanup;

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
     * @see CleanupEventListener
     * @see CleanupActionProcessor#setListener(CleanupEventListener)
     */
    ASK;
}
