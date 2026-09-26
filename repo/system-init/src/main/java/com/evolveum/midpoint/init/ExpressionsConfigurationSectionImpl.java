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
import org.jetbrains.annotations.Nullable;
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
        boolean safeExpressionsOnly,
        boolean safeVelocityExpressionsOnly,
        boolean legacyVelocityEngine,
        @Nullable String customVelocityExtensionClassName,
        Collection<String> javaMethodEvaluatorPackageNames) implements ExpressionsConfigurationSection {

    private static final String SAFE_EXPRESSIONS_ONLY_CONFIG_KEY = "safeExpressionsOnly";
    private static final String SAFE_VELOCITY_EXPRESSIONS_ONLY = "safeVelocityExpressionsOnly";
    private static final String LEGACY_VELOCITY_ENGINE = "legacyVelocityEngine";
    private static final String CUSTOM_VELOCITY_EXTENSION_CLASS_NAME = "customVelocityExtensionClassName";
    private static final String JAVA_METHOD_EVALUATOR_PACKAGE_NAME = "javaMethodEvaluatorPackageName";

    static ExpressionsConfigurationSectionImpl create(Configuration configuration) {
        var safeExpressionsOnly = configuration.getBoolean(SAFE_EXPRESSIONS_ONLY_CONFIG_KEY, false);
        var safeVelocityExpressionsOnly = configuration.getBoolean(SAFE_VELOCITY_EXPRESSIONS_ONLY, safeExpressionsOnly);
        var customVelocityExtensionClassName = configuration.getString(CUSTOM_VELOCITY_EXTENSION_CLASS_NAME, null);
        var legacyVelocityEngine = configuration.getBoolean(LEGACY_VELOCITY_ENGINE, false);

        var javaMethodEvaluatorPackageNames = List.copyOf(
                MiscUtil.emptyIfNull(
                        configuration.getList(String.class, JAVA_METHOD_EVALUATOR_PACKAGE_NAME)));

        if (safeExpressionsOnly && !safeVelocityExpressionsOnly) {
            throw new IllegalArgumentException(
                    "Configuration error: '%s' is true but '%s' is false. '%s' must be true if '%s' is true.".formatted(
                            SAFE_EXPRESSIONS_ONLY_CONFIG_KEY, SAFE_VELOCITY_EXPRESSIONS_ONLY,
                            SAFE_VELOCITY_EXPRESSIONS_ONLY, SAFE_EXPRESSIONS_ONLY_CONFIG_KEY));
        }

        if (safeVelocityExpressionsOnly && legacyVelocityEngine) {
            throw new IllegalArgumentException(
                    ("Configuration error: '%s' is true but '%s' is true as well. Safe Velocity expressions are not compatible "
                            + "with the legacy Velocity engine.").formatted(
                            SAFE_VELOCITY_EXPRESSIONS_ONLY, LEGACY_VELOCITY_ENGINE));
        }

        return new ExpressionsConfigurationSectionImpl(
                safeExpressionsOnly,
                safeVelocityExpressionsOnly,
                legacyVelocityEngine,
                customVelocityExtensionClassName,
                javaMethodEvaluatorPackageNames);
    }
}
