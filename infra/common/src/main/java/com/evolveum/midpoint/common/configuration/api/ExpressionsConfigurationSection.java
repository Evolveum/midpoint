/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import com.evolveum.midpoint.schema.expression.CustomVelocityExtension;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

/**
 * Provides typed access to the "expressions" section of the config.xml file.
 */
@NullMarked
public interface ExpressionsConfigurationSection {

    /** @see MidpointConfiguration#isSafeExpressionsOnly() */
    boolean safeExpressionsOnly();

    /** Whether we should allow running Velocity in safe mode only. If {@code true}, full Velocity will be forbidden. */
    boolean safeVelocityExpressionsOnly();

    /**
     * Whether we should initialize Velocity engine with legacy settings (as in midPoint 4.10 and earlier).
     *
     * BEWARE: Incompatible with new safe mode. If you set this to {@code true}, you cannot use `safe-velocity` scripting
     * language.
     */
    boolean legacyVelocityEngine();

    /** Instance of {@link CustomVelocityExtension} class to be injected into Velocity context. */
    @Nullable String customVelocityExtensionClassName();

    /** Returns names of packages that can contain java methods callable from "javaMethodReference" expression evaluator. */
    Collection<String> javaMethodEvaluatorPackageNames();
}
