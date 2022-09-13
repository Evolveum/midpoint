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
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTransportConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "mailTransportPanel")
@PanelInstance(
        identifier = "mailTransportPanel",
        applicableForType = MessageTransportConfigurationType.class,
        display = @PanelDisplay(
                label = "MailTransportContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
@Counter(provider = MailTransportCounter.class)
public class MailTransportContentPanel extends GeneralTransportContentPanel<MailTransportConfigurationType> {

    public MailTransportContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, model, configurationType, MailTransportConfigurationType.class, MessageTransportConfigurationType.F_MAIL);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<MailTransportConfigurationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<MailTransportConfigurationType>, String>> columns = new ArrayList<>();
        columns.addAll(super.createDefaultColumns());
        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), MailTransportConfigurationType.F_SERVER,
                AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), MailTransportConfigurationType.F_DEFAULT_FROM,
                AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));

        return columns;
    }

    @Override
    protected MultivalueContainerDetailsPanel<MailTransportConfigurationType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<MailTransportConfigurationType>> item) {

        return new MailTransportDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_MAIL_TRANSPORT_CONTENT;
    }
}
