/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

/**
 * Provides typed access to the "expressions" section of the config.xml file.
 */
public interface ExpressionsConfigurationSection {

    /** @see MidpointConfiguration#isSafeExpressionsOnly() */
    boolean isSafeExpressionsOnly();
}
