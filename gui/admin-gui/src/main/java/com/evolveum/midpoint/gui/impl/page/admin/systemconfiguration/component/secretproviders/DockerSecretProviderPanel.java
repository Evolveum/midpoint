/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.component.GenericSingleContainerPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DockerSecretsProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProvidersType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@PanelType(name = "secretsProvidersDocker")
@PanelInstance(
        identifier = "secretsProvidersDocker",
        applicableForType = SecretsProvidersType.class,
        display = @PanelDisplay(
                label = "SecretsProvidersType.docker",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        ),
        containerPath = "secretsProviders/docker",
        type = "SecretsProvidersType",
        expanded = true
)
public class DockerSecretProviderPanel extends GenericSingleContainerPanel<DockerSecretsProviderType, SystemConfigurationType> {
    public DockerSecretProviderPanel(String id, ObjectDetailsModels<SystemConfigurationType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected ItemMandatoryHandler createMandatoryHandler() {
        return (itemWrapper) -> false;
    }
}
