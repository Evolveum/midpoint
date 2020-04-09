/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationItem;

/**
 * @author semancik
 *
 */
public class DummyItsmIntegrationConnectorConfiguration {

    private String uselessString;
    private String[] uselessArray;

    @ConfigurationItem
    public String getUselessString() {
        return uselessString;
    }

    public void setUselessString(String uselessString) {
        this.uselessString = uselessString;
    }

    @ConfigurationItem
    public String[] getUselessArray() {
        return uselessArray;
    }

    public void setUselessArray(String[] uselessArray) {
        this.uselessArray = uselessArray;
    }
}
