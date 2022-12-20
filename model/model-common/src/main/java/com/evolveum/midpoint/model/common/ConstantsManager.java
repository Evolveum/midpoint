/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
