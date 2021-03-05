/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.actuator;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Check if database is up and available.
 * Indicator send query for countObjects(SystemConfigurationType.class)
 *
 * @author skublik
 */

@Component
public class RepoHealthIndicator implements HealthIndicator {

    private static final Trace LOGGER = TraceManager.getTrace(RepoHealthIndicator.class);

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public Health health() {
        if (!isAvailable()) {
            return Health.down().withDetail("database", "Database is not available.").build();
        }
        return Health.up().build();
    }

    private boolean isAvailable() {
        try {
            repositoryService.countObjects(SystemConfigurationType.class, null, null, new OperationResult("count_system_configuration"));
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Couldn't check database", e);
            return false;
        }
        return true;
    }
}
