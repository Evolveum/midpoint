/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;

public abstract class AbstractCorrelationResult<C extends Containerable> implements Serializable, DebugDumpable {

    /** What is the result of the correlation? */
    @NotNull final CorrelationSituationType situation;

    /** The correlated owner. Non-null if and only if {@link #situation} is {@link CorrelationSituationType#EXISTING_OWNER}. */
    @Nullable final C owner;

    AbstractCorrelationResult(@NotNull CorrelationSituationType situation, @Nullable C owner) {
        this.situation = situation;
        this.owner = owner;
    }

    public @NotNull CorrelationSituationType getSituation() {
        return situation;
    }

    public @Nullable C getOwner() {
        return owner;
    }
}
