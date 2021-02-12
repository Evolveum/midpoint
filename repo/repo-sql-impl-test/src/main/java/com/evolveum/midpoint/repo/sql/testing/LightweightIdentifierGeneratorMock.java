/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Simple mock identifier generator to satisfy spring dependencies.
 *
 * @author lazyman
 */
public class LightweightIdentifierGeneratorMock implements LightweightIdentifierGenerator {

    private int sequence;

    @Override
    public @NotNull LightweightIdentifier generate() {
        return new LightweightIdentifier(System.currentTimeMillis(), 0, ++sequence);
    }
}
