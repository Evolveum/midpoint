/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;

import java.util.Collection;

/** Simulates "config.xml" section for expressions. Used in tests to override the default configuration. */
public record TestingExpressionConfiguration(
        boolean isSafeExpressionsOnly, Collection<String> melExtensionLibraryClassNames)
        implements ExpressionsConfigurationSection {
}
