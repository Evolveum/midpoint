/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.util;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BasicLightweightIdentifierGenerator implements LightweightIdentifierGenerator {

    private int sequence;

    @Override
    public @NotNull LightweightIdentifier generate() {
        return new LightweightIdentifier(System.currentTimeMillis(), 0, ++sequence);
    }
}
