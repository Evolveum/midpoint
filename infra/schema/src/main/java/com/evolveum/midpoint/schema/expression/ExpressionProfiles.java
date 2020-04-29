/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author semancik
 *
 */
public class ExpressionProfiles {

    private final Map<String,ExpressionProfile> profiles = new ConcurrentHashMap<>();

    public ExpressionProfile getProfile(String identifier) {
        return profiles.get(identifier);
    }

    public void add(ExpressionProfile profile) {
        profiles.put(profile.getIdentifier(), profile);
    }

    public int size() {
        return profiles.size();
    }

    public Map<String, ExpressionProfile> getProfiles() {
        return Collections.unmodifiableMap(profiles);
    }
}
