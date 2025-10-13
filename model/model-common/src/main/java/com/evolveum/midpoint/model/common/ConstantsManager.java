/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common;

import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

/**
 * @author semancik
 */
@Component
public class ConstantsManager {

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    private Configuration constConfig;

    public ConstantsManager() {
    }

    @VisibleForTesting
    public ConstantsManager(Configuration config) {
        this.constConfig = config;
    }

    private Configuration getConstConfig() {
        if (constConfig == null) {
            constConfig = midpointConfiguration.getConfiguration(MidpointConfiguration.CONSTANTS_CONFIGURATION);
        }
        return constConfig;
    }

    public String getConstantValue(String constName) {
        return getConstConfig().getString(constName);
    }
}
