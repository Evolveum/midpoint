/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.secretproviders;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

import java.util.List;

@PanelType(name = "secretsProvidersEnvironmentVariables")
@PanelInstance(
        identifier = "secretsProvidersEnvironmentVariables",
        applicableForType = SecretsProvidersType.class,
        display = @PanelDisplay(
                label = "SecretsProvidersType.environmentVariables",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        )
)
@Counter(provider = EnvironmentVariablesSecretProvidersCounter.class)
public class EnvironmentVariablesSecretProvidersContentPanel extends SecretProvidersContentPanel<EnvironmentVariablesSecretsProviderType> {

    public EnvironmentVariablesSecretProvidersContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, model, configurationType, EnvironmentVariablesSecretsProviderType.class, SecretsProvidersType.F_ENVIRONMENT_VARIABLES);
    }

    @Override
    protected void addCustomColumns(List<IColumn<PrismContainerValueWrapper<EnvironmentVariablesSecretsProviderType>, String>> columns) {
        columns.add(
                new PrismPropertyWrapperColumn<>(getContainerModel(), EnvironmentVariablesSecretsProviderType.F_USE_SYSTEM_PROPERTIES,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_ENVIRONMENT_VARIABLES_SECRET_PROVIDERS;
    }
}
