/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;

/** Simulates "config.xml" section for expressions. Used in tests to override the default configuration. */
public class TestingExpressionConfiguration implements ExpressionsConfigurationSection {

    private final boolean safeExpressionsOnly;

    public TestingExpressionConfiguration(boolean safeExpressionsOnly) {
        this.safeExpressionsOnly = safeExpressionsOnly;
    }

    @Override
    public boolean isSafeExpressionsOnly() {
        return safeExpressionsOnly;
    }
}
