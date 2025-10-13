/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface LightweightIdentifierGenerator {

    @NotNull LightweightIdentifier generate();

}
