/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
        InternalsConfig.set(
                midpointConfiguration.getConfiguration(MidpointConfiguration.INTERNALS_CONFIGURATION));
    }
}
