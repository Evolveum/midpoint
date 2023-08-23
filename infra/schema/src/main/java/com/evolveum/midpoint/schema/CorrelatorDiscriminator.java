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

/**
 * Used to select relevant correlator among multiple ones defined - currently only in object template.
 */
public class CorrelatorDiscriminator {

    /**
     * Correlator identifier. Always null when dealing with synchronization correlators.
     * TODO what about nullness in the case of identity recovery?
     */
    @Nullable private final String correlatorIdentifier;
    @NotNull private final CorrelationUseType use;

    private CorrelatorDiscriminator(@Nullable String correlatorIdentifier, @NotNull CorrelationUseType use) {
        this.correlatorIdentifier = correlatorIdentifier;
        this.use = use;
    }

    public static CorrelatorDiscriminator forSynchronization() {
        return new CorrelatorDiscriminator(null, CorrelationUseType.SYNCHRONIZATION);
    }

    public static CorrelatorDiscriminator forIdentityRecovery(String identifier) {
        return new CorrelatorDiscriminator(identifier, CorrelationUseType.IDENTITY_RECOVERY);
    }

    public boolean match(@NotNull CompositeCorrelatorType correlator) {
        if (Objects.requireNonNullElse(correlator.getUse(), CorrelationUseType.SYNCHRONIZATION) != use) {
            return false;
        }
        if (use == CorrelationUseType.SYNCHRONIZATION) {
            assert correlatorIdentifier == null;
            // We intentionally DO NOT check the correlator name against identifier in discriminator (which is null anyway).
            // The reason is that there should be a single synchronization-use correlator, and we we don't want to skip
            // it if it is (by chance) named.
            return true;
        }
        return Objects.equals(correlator.getName(), correlatorIdentifier);
    }
}
