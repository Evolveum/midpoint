/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.boot.actuator;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author skublik
 */

@Component
public class MpHomeHealthIndicator implements HealthIndicator {

    private static final Trace LOGGER = TraceManager.getTrace(RepoHealthIndicator.class);

    @Override
    public Health health() {
        String errorMessage = checkDirectoryExistence();
        if (StringUtils.isNotEmpty(errorMessage)) {
            return Health.down().withDetail("mP_home", errorMessage).build();
        }
        return Health.up().build();
    }

    private String checkDirectoryExistence() {
        String mpHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
        if (StringUtils.isEmpty(mpHome)) {
            return "Value of property " + MidpointConfiguration.MIDPOINT_HOME_PROPERTY + " is empty.";
        }
        File d = new File(mpHome);
        if (!d.exists()) {
            String message = mpHome + " doesn't exist.";
            LOGGER.error(message);
            return message;
        }
        if (d.isFile()) {
            String message = mpHome + " is file and NOT a directory.";
            LOGGER.error(message);
            return message;
        }
        if (d.isDirectory()) {
            return null;
        }
        String message = mpHome + " isn't a directory.";
        LOGGER.error(message);
        return message;
    }
}
