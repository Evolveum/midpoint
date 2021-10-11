/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.expression;

import java.util.HashMap;
import java.util.Map;

/**
 * @author semancik
 *
 */
public class ExpressionProfiles {

    private final Map<String,ExpressionProfile> profiles = new HashMap<>();

    public ExpressionProfile getProfile(String identifier) {
        return profiles.get(identifier);
    }

    public void add(ExpressionProfile profile) {
        profiles.put(profile.getIdentifier(), profile);
    }

    public int size() {
        return profiles.size();
    }
}
