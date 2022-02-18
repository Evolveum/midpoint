/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.AbstractAssignmentTypePanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizedMessageTemplateContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "messageTemplateLocalizedContent")
@PanelInstance(
        identifier = "messageTemplateLocalizedContent",
        applicableForType = MessageTemplateType.class,
        display = @PanelDisplay(
                label = "PageMessageTemplate.localizedContent",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        )
)
@Counter(provider = LocalizedContentCounter.class)
public class MessageTemplateLocalizedContentPanel extends MultivalueContainerListPanelWithDetailsPanel<LocalizedMessageTemplateContentType> {

    private IModel<PrismContainerWrapper<LocalizedMessageTemplateContentType>> model;

    public MessageTemplateLocalizedContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, LocalizedMessageTemplateContentType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), MessageTemplateType.F_LOCALIZED_CONTENT);
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_MESSAGE_TEMPLATE_LOCALIZED_CONTENT_PANEL;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), LocalizedMessageTemplateContentType.F_LANGUAGE,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>> model) {
                        MessageTemplateLocalizedContentPanel.this.itemDetailsPerformed(target, model);
                    }
                }
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<LocalizedMessageTemplateContentType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<LocalizedMessageTemplateContentType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<LocalizedMessageTemplateContentType>> item) {

        return new TemplateContentDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }
}
