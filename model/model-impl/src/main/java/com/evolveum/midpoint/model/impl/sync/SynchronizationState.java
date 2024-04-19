/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.AbstractCorrelationResult;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

public record SynchronizationState<C extends Containerable> (
        @NotNull SynchronizationSituationType situation,
        @Nullable C owner) {

    // FIXME fix the generics here
    public static <C extends Containerable> SynchronizationState<C> fromCorrelationResult(
            @NotNull AbstractCorrelationResult<?> correlationResult) {
        SynchronizationSituationType state;
        C owner = switch (correlationResult.getSituation()) {
            case EXISTING_OWNER -> {
                state = SynchronizationSituationType.UNLINKED;
                //noinspection unchecked
                yield (C) correlationResult.getOwner();
            }
            case NO_OWNER -> {
                state = SynchronizationSituationType.UNMATCHED;
                yield null;
            }
            case UNCERTAIN, ERROR -> {
                state = SynchronizationSituationType.DISPUTED;
                yield null;
            }
        };
        return new SynchronizationState<>(state, owner);
    }
}
