/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
