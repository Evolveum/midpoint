/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.notification;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTransportConfigurationType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;

import java.util.ArrayList;
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
public class CustomTransportContentPanel extends GeneralTransportContentPanel<CustomTransportConfigurationType> {

    public CustomTransportContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, model, configurationType, CustomTransportConfigurationType.class, MessageTransportConfigurationType.F_CUSTOM_TRANSPORT);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<CustomTransportConfigurationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<CustomTransportConfigurationType>, String>> columns = new ArrayList<>();
        columns.addAll(super.createDefaultColumns());
        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), CustomTransportConfigurationType.F_TYPE,
                AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));

        return columns;
    }

    @Override
    protected MultivalueContainerDetailsPanel<CustomTransportConfigurationType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<CustomTransportConfigurationType>> item) {

        return new CustomTransportDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CUSTOM_TRANSPORT_CONTENT;
    }
}
