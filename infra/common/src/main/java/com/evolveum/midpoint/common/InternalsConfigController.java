/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import jakarta.annotation.PostConstruct;

import org.apache.commons.configuration2.Configuration;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

/**
 * @author semancik
 *
 */
public class InternalsConfigController {

    private MidpointConfiguration midpointConfiguration;

    public MidpointConfiguration getMidpointConfiguration() {
        return midpointConfiguration;
    }

    public void setMidpointConfiguration(MidpointConfiguration midpointConfiguration) {
        this.midpointConfiguration = midpointConfiguration;
    }

    @PostConstruct
    public void init() {
        Configuration internalsConfig = midpointConfiguration.getConfiguration(MidpointConfiguration.INTERNALS_CONFIGURATION);
        InternalsConfig.set(internalsConfig);
    }
}
