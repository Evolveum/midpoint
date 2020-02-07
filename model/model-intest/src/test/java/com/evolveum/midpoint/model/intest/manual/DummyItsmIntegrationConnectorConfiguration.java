/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationProperty;

/**
 * @author semancik
 *
 */
public class DummyItsmIntegrationConnectorConfiguration {

    private String uselessString;
    private String[] uselessArray;

    @ConfigurationProperty
    public String getUselessString() {
        return uselessString;
    }

    public void setUselessString(String uselessString) {
        this.uselessString = uselessString;
    }

    @ConfigurationProperty
    public String[] getUselessArray() {
        return uselessArray;
    }

    public void setUselessArray(String[] uselessArray) {
        this.uselessArray = uselessArray;
    }
}
