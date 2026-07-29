/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common;

import ch.qos.logback.core.boolex.PropertyConditionBase;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

/**
 * <p>This class offers conditional processing of logback
 * configuration files using Java-only code.</p>
 *
 * <p>Previously, conditional processing relied on the Janino library
 * which offers dynamic, i.e. runtime, java code compilation and execution.
 * In version 1.5.20, this approach has been deprecated due to security
 * vulnerabilities associated with dynamic code compilation and execution.
 * </p>
 *
 * <p>PropertyConditionBase, the superclass, is available in logback-core 1.5.20 or later.</p>
 */
public class AlternativeLoggingPropertyCondition extends PropertyConditionBase {

    @Override
    public boolean evaluate() {
        return "true".equals(property(MidpointConfiguration.MIDPOINT_LOGGING_ALT_ENABLED_PROPERTY));
    }
}
