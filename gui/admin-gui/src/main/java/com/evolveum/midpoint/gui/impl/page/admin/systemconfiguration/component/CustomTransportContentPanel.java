/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

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
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "customTransportPanel")
@PanelInstance(
        identifier = "customTransportPanel",
        applicableForType = MessageTransportConfigurationType.class,
        display = @PanelDisplay(
                label = "CustomTransportContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 50
        )
)
@Counter(provider = CustomTransportCounter.class)
public class CustomTransportContentPanel extends MultivalueContainerListPanelWithDetailsPanel<CustomTransportConfigurationType> {

    private IModel<PrismContainerWrapper<CustomTransportConfigurationType>> model;

    public CustomTransportContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, CustomTransportConfigurationType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION,
                MessageTransportConfigurationType.F_MAIL
        ));
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<CustomTransportConfigurationType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<CustomTransportConfigurationType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), CustomTransportConfigurationType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CustomTransportConfigurationType>> model) {
                        CustomTransportContentPanel.this.itemDetailsPerformed(target, model);
                    }
                }
                // todo more columns
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<CustomTransportConfigurationType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<CustomTransportConfigurationType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<CustomTransportConfigurationType>> item) {

        return new CustomTransportDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_MESSAGE_TEMPLATE_LOCALIZED_CONTENT_PANEL;    // todo fix
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
