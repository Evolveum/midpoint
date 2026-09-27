/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.prism.Safe;

/**
 * User-provided extension to be called from Velocity scripts.
 *
 * It will get injected into Velocity context for all Velocity scripts evaluated by the system
 * (as part of the expression evaluation framework).
 *
 * There is only a single shared instance of this extension, instantiated automatically by the system.
 *
 * The class implementing this interface must have a public no-arg constructor.
 *
 * The class implementing this interface must be pointed to in `config.xml` as `expressions/customVelocityExtensionClassName`.
 *
 * All methods that are to be called from Velocity scripts running in the safe mode must be annotated with {@link Safe}.
 *
 * NOTE: This is a temporary/experimental solution. In the future we may allow calling e.g. library functions from
 * Velocity scripts. It will be definitely more flexible.
 */
@NullMarked
@Safe
@Experimental
public interface CustomVelocityExtension {

    /** Name of variable under which this extension will be available in the Velocity context. */
    String getVariableName();

}
