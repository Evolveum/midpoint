/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationProperty;

/**
 * @author semancik
 *
 */
public class ManualConnectorConfiguration {

    private String defaultAssignee;

    @ConfigurationProperty
    public String getDefaultAssignee() {
        return defaultAssignee;
    }

    public void setDefaultAssignee(String defaultAssignee) {
        this.defaultAssignee = defaultAssignee;
    }

}
