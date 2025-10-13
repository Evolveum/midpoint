/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationItem;

/**
 * @author semancik
 *
 */
public class ManualConnectorConfiguration {

    private String defaultAssignee;

    @ConfigurationItem
    public String getDefaultAssignee() {
        return defaultAssignee;
    }

    public void setDefaultAssignee(String defaultAssignee) {
        this.defaultAssignee = defaultAssignee;
    }

}
