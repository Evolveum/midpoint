/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.menu;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import org.jetbrains.annotations.NotNull;
import java.io.Serializable;

public class ContainerPanelDto implements Serializable {

    private boolean isExpanded;
    private final ContainerPanelConfigurationType containerPanelConfig;

    public ContainerPanelDto(@NotNull ContainerPanelConfigurationType containerPanelConfig) {
        this.containerPanelConfig = containerPanelConfig;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    public ContainerPanelConfigurationType getContainerPanelConfig() {
        return containerPanelConfig;
    }
}
