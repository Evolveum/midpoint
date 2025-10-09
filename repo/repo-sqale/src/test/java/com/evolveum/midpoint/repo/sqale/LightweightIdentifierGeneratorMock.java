/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale;

import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

/**
 * Simple mock identifier generator to satisfy Spring dependencies.
 */
public class LightweightIdentifierGeneratorMock implements LightweightIdentifierGenerator {

    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public @NotNull LightweightIdentifier generate() {
        return new LightweightIdentifier(System.currentTimeMillis(), 0, sequence.incrementAndGet());
    }
}
