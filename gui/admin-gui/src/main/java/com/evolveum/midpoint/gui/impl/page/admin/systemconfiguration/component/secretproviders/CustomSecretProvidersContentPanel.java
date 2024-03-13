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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomSecretsProviderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecretsProvidersType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

import java.util.List;

@PanelType(name = "secretsProvidersCustom")
@PanelInstance(
        identifier = "secretsProvidersCustom",
        applicableForType = SecretsProvidersType.class,
        display = @PanelDisplay(
                label = "SecretsProvidersType.custom",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 50
        )
)
@Counter(provider = CustomSecretProvidersCounter.class)
public class CustomSecretProvidersContentPanel extends SecretProvidersContentPanel<CustomSecretsProviderType> {

    public CustomSecretProvidersContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, model, configurationType, CustomSecretsProviderType.class, SecretsProvidersType.F_CUSTOM);
    }

    @Override
    protected void addCustomColumns(List<IColumn<PrismContainerValueWrapper<CustomSecretsProviderType>, String>> columns) {
        columns.add(
                new PrismPropertyWrapperColumn<>(getContainerModel(), CustomSecretsProviderType.F_CLASS_NAME,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CUSTOM_SECRET_PROVIDERS;
    }
}
