/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * An indexed set of {@link ExpressionProfile} objects.
 *
 * @author semancik
 */
public class ExpressionProfiles {

    @NotNull private final Map<String, ExpressionProfile> profiles;

    public ExpressionProfiles(List<ExpressionProfile> expressionProfiles) {
        profiles = expressionProfiles.stream()
                .collect(Collectors.toUnmodifiableMap(
                        p -> p.getIdentifier(), p -> p));
    }

    public @NotNull ExpressionProfile getProfile(@NotNull String identifier) throws ConfigurationException {
        return MiscUtil.configNonNull(
                profiles.get(identifier),
                "No expression profile with identifier '%s'", identifier);
    }

    public int size() {
        return profiles.size();
    }

    public @NotNull Map<String, ExpressionProfile> getProfiles() {
        return profiles;
    }
}
