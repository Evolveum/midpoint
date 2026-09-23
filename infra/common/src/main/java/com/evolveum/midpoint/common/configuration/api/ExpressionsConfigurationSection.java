/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import java.util.Collection;

/**
 * Provides typed access to the "expressions" section of the config.xml file.
 */
public interface ExpressionsConfigurationSection {

    /** @see MidpointConfiguration#isSafeExpressionsOnly() */
    boolean isSafeExpressionsOnly();

    /** Returns names of packages that can contain java methods callable from "javaMethodReference" expression evaluator. */
    Collection<String> javaMethodEvaluatorPackageNames();
}
