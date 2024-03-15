/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.admingui;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import com.evolveum.midpoint.web.component.util.SerializableSupplier;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurableUserDashboardType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "configurableUserDashboardPanel")
@PanelInstance(
        identifier = "configurableUserDashboardPanel",
        applicableForType = AdminGuiConfigurationType.class,
        display = @PanelDisplay(
                label = "ConfigurableUserDashboardContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 60
        )
)
@Counter(provider = ConfigurableUserDashboardCounter.class)
public class ConfigurableUserDashboardContentPanel extends MultivalueContainerListPanelWithDetailsPanel<ConfigurableUserDashboardType> {

    private IModel<PrismContainerWrapper<ConfigurableUserDashboardType>> model;

    public ConfigurableUserDashboardContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, ConfigurableUserDashboardType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                AdminGuiConfigurationType.F_CONFIGURABLE_USER_DASHBOARD),
                (SerializableSupplier<PageBase>) () -> getPageBase());
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<ConfigurableUserDashboardType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ConfigurableUserDashboardType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), ConfigurableUserDashboardType.F_IDENTIFIER,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ConfigurableUserDashboardType>> model) {
                        ConfigurableUserDashboardContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), ConfigurableUserDashboardType.F_DISPLAY_ORDER,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase())
//                new PrismPropertyWrapperColumn<>(getContainerModel(), ConfigurableUserDashboardType.F_DISPLAY,
//                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase())
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<ConfigurableUserDashboardType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<ConfigurableUserDashboardType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<ConfigurableUserDashboardType>> item) {

        return new ConfigurableUserDashboardDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CONFIGURABLE_USER_DASHBOARD_CONTENT;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
