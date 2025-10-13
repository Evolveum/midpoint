/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationIntentType;

/**
 * @author semancik
 *
 */
public enum SynchronizationIntent {

    /**
     * New account that should be added (and linked)
     */
    ADD,

    /**
     * Existing account that should be deleted (and unlinked)
     */
    DELETE,

    /**
     * Existing account that is kept as it is (remains linked).
     */
    KEEP,

    /**
     * Existing account that should be unlinked (but NOT deleted). By unlinking we mean physically removing
     * a value from `linkRef`. This intent should be used only when really needed.
     */
    UNLINK,

    /**
     * Existing account that belongs to the user and needs to be synchronized.
     * This may include deleting, archiving or disabling the account.
     */
    SYNCHRONIZE;

    public SynchronizationIntentType toSynchronizationIntentType() {
        switch(this) {
            case ADD: return SynchronizationIntentType.ADD;
            case DELETE: return SynchronizationIntentType.DELETE;
            case KEEP: return SynchronizationIntentType.KEEP;
            case UNLINK: return SynchronizationIntentType.UNLINK;
            case SYNCHRONIZE: return SynchronizationIntentType.SYNCHRONIZE;
            default: throw new AssertionError("Unknown value of SynchronizationIntent: " + this);
        }
    }

    public static SynchronizationIntent fromSynchronizationIntentType(SynchronizationIntentType value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case ADD: return ADD;
            case DELETE: return DELETE;
            case KEEP: return KEEP;
            case UNLINK: return UNLINK;
            case SYNCHRONIZE: return SYNCHRONIZE;
            default: throw new AssertionError("Unknown value of SynchronizationIntentType: " + value);
        }
    }
}
