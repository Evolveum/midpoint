/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

/**
 * Notified once about the outcome of the whole authentication sequence, as opposed to the outcome of a single module.
 * Implemented by authentication providers of modules that have to act when the sequence is decided, e.g. a module
 * with a one-time credential spends it only when the whole sequence succeeded.
 *
 * The listeners of all modules of the sequence are notified, {@code moduleAuthentication} is the processed state
 * of the listener's own module, null when the module was not reached.
 */
public interface AuthenticationSequenceListener {

    default void sequenceSucceeded(
            @NotNull MidpointAuthentication mpAuthentication, @Nullable ModuleAuthentication moduleAuthentication) {
    }

    default void sequenceFailed(
            @NotNull MidpointAuthentication mpAuthentication, @Nullable ModuleAuthentication moduleAuthentication) {
    }
}
