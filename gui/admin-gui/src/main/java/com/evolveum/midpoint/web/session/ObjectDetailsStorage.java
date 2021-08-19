/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import java.io.Serializable;

public class ObjectDetailsStorage implements Serializable {

    private ContainerPanelConfigurationType defaultConfiguration;

    public void setDefaultConfiguration(ContainerPanelConfigurationType defaultConfiguration) {
        this.defaultConfiguration = defaultConfiguration;
    }

    public ContainerPanelConfigurationType getDefaultConfiguration() {
        return defaultConfiguration;
    }
}
