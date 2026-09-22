/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.configuration2.Configuration;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;

/**
 * Provides typed access to the "expressions" section of the config.xml file.
 *
 * Data are parsed here at startup because of performance reasons.
 *
 * @see MidpointConfiguration#isSafeExpressionsOnly()
 */
@NullMarked
record ExpressionsConfigurationSectionImpl(
        boolean isSafeExpressionsOnly,
        Collection<String> melExtensionLibraryClassNames) implements ExpressionsConfigurationSection {

    private static final String SAFE_EXPRESSIONS_ONLY_CONFIG_KEY = "safeExpressionsOnly";
    private static final String MEL_EXTENSION_LIBRARY_CLASS_NAME_CONFIG_KEY = "melExtensionLibraryClassName";

    ExpressionsConfigurationSectionImpl(Configuration configuration) {
        this(
                configuration.getBoolean(SAFE_EXPRESSIONS_ONLY_CONFIG_KEY, false),
                List.copyOf(
                        MiscUtil.emptyIfNull(
                                configuration.getList(String.class, MEL_EXTENSION_LIBRARY_CLASS_NAME_CONFIG_KEY))));
    }
}
