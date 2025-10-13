/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
