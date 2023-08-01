/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationUseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CorrelatorDiscriminator {

    /**
     * Correlator identifier. It is either a name of the correlator object or null.
     * Null is supported for synchronization.
     */
    @Nullable private final String correlatorIdentifier;
    @NotNull private final CorrelationUseType use;

    public CorrelatorDiscriminator(@Nullable String correlatorIdentifier, @NotNull CorrelationUseType use) {
        this.correlatorIdentifier = correlatorIdentifier;
        this.use = use;
    }

    public boolean match(@NotNull CompositeCorrelatorType correlator) {
        return Objects.equals(correlator.getName(), correlatorIdentifier)
                && correlator.getUse() == use;
    }
}

