package com.evolveum.midpoint.schema.util.cleanup;

import java.util.Arrays;

/**
 * Cleanup action to be taken when a path is encountered.
 */
public enum CleanupPathAction {

    /**
     * Item is removed from the object.
     */
    REMOVE("remove"),

    /**
     * Item is not removed from the object.
     */
    IGNORE("ignore"),

    /**
     * Consumer is asked what to do.
     *
     * @see CleanupEventListener
     * @see CleanupActionProcessor#setListener(CleanupEventListener)
     */
    ASK("ask");

    private final String value;

    CleanupPathAction(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static CleanupPathAction getState(String value) {
        if (value == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(s -> value.equals(s.value()))
                .findFirst()
                .orElse(null);
    }
}
