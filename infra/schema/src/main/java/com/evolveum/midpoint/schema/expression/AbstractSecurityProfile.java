/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractSecurityProfileType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Abstract "standalone" security profile, identified by an ID.
 *
 * Corresponds to {@link AbstractSecurityProfileType}.
 */
public abstract class AbstractSecurityProfile implements Serializable {

    @NotNull private final String identifier;

    @NotNull private final AccessDecision defaultDecision;

    AbstractSecurityProfile(@NotNull String identifier, @NotNull AccessDecision defaultDecision) {
        this.identifier = identifier;
        this.defaultDecision = defaultDecision;
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public @NotNull AccessDecision getDefaultDecision() {
        return defaultDecision;
    }
}
