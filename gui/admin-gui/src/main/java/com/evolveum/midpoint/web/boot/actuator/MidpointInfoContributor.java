/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.boot.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
@PropertySource(value = "classpath:midpoint-system.properties", encoding = "UTF-8")
public class MidpointInfoContributor implements InfoContributor {

    @Autowired
    private Environment env;

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("name", "Midpoint");
        builder.withDetail("version", env.getProperty("midpoint.system.version"));
        builder.withDetail("build", env.getProperty("midpoint.system.build"));
        builder.withDetail("branch", env.getProperty("midpoint.system.branch"));
        builder.withDetail("buildTimestamp", env.getProperty("midpoint.system.buildTimestamp"));
        builder.withDetail("scm", env.getProperty("midpoint.system.scm"));
        builder.withDetail("jira", env.getProperty("midpoint.system.jira"));
        builder.withDetail("license", env.getProperty("midpoint.system.license"));

    }

}
