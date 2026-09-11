/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

import org.apache.commons.configuration2.Configuration;

/**
 * Provides typed access to the "expressions" section of the config.xml file.
 *
 * Data are parsed here at startup because of performance reasons.
 *
 * @see MidpointConfiguration#isSafeExpressionsOnly()
 */
record ExpressionsConfigurationSectionImpl(boolean isSafeExpressionsOnly) implements ExpressionsConfigurationSection {

    private static final String SAFE_EXPRESSIONS_ONLY_CONFIG_KEY = "safeExpressionsOnly";

    ExpressionsConfigurationSectionImpl(Configuration configuration) {
        this(configuration != null && configuration.getBoolean(SAFE_EXPRESSIONS_ONLY_CONFIG_KEY, false));
    }
}
