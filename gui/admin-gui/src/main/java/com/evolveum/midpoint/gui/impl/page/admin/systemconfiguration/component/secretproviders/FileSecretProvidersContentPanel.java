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

@PanelType(name = "secretsProvidersFile")
@PanelInstance(
        identifier = "secretsProvidersFile",
        applicableForType = SecretsProvidersType.class,
        display = @PanelDisplay(
                label = "SecretsProvidersType.file",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
@Counter(provider = FileSecretProvidersCounter.class)
public class FileSecretProvidersContentPanel extends SecretProvidersContentPanel<FileSecretsProviderType> {

    public FileSecretProvidersContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, model, configurationType, FileSecretsProviderType.class, SecretsProvidersType.F_FILE);
    }

    @Override
    protected void addCustomColumns(List<IColumn<PrismContainerValueWrapper<FileSecretsProviderType>, String>> columns) {
        columns.add(
                new PrismPropertyWrapperColumn<>(getContainerModel(), FileSecretsProviderType.F_PARENT_DIRECTORY_PATH,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_FILE_SECRET_PROVIDERS;
    }
}
