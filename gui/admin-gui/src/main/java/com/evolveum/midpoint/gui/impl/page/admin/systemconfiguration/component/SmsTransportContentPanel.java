/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsTransportConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "smsTransportPanel")
@PanelInstance(
        identifier = "smsTransportPanel",
        applicableForType = MessageTransportConfigurationType.class,
        display = @PanelDisplay(
                label = "SmsTransportContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        )
)
@Counter(provider = SmsTransportCounter.class)
public class SmsTransportContentPanel extends GeneralTransportContentPanel<SmsTransportConfigurationType> {

    public SmsTransportContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, model, configurationType, SmsTransportConfigurationType.class, MessageTransportConfigurationType.F_SMS);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<SmsTransportConfigurationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<SmsTransportConfigurationType>, String>> columns = new ArrayList<>();
        columns.addAll(super.createDefaultColumns());
        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), SmsTransportConfigurationType.F_GATEWAY,
                AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), SmsTransportConfigurationType.F_DEFAULT_FROM,
                AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));

        return columns;
    }

    @Override
    protected MultivalueContainerDetailsPanel<SmsTransportConfigurationType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<SmsTransportConfigurationType>> item) {

        return new SmsTransportDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_SMS_TRANSPORT_CONTENT;
    }
}
