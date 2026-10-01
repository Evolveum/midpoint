/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

/** Simulates "config.xml" section for expressions. Used in tests to override the default configuration. */
@NullMarked
public record TestingExpressionConfiguration(
        boolean safeExpressionsOnly, boolean safeVelocityExpressionsOnly, Collection<String> javaMethodEvaluatorPackageNames)
        implements ExpressionsConfigurationSection {

    @Override
    public boolean legacyVelocityEngine() {
        return false;
    }

    @Override
    public @Nullable String customVelocityExtensionClassName() {
        return null;
    }
}
