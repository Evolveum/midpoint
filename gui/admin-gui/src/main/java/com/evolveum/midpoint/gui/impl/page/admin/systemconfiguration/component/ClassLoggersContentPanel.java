/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "classLoggersContent")
@PanelInstance(
        identifier = "classLoggersContent",
        applicableForType = LoggingConfigurationType.class,
        display = @PanelDisplay(
                label = "ClassLoggersContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
@Counter(provider = ClassLoggersMenuLinkCounter.class)
public class ClassLoggersContentPanel extends MultivalueContainerListPanelWithDetailsPanel<ClassLoggerConfigurationType> {

    private IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> model;

    public ClassLoggersContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, ClassLoggerConfigurationType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_LOGGING,
                LoggingConfigurationType.F_CLASS_LOGGER
        ));
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), ClassLoggerConfigurationType.F_PACKAGE,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> model) {
                        ClassLoggersContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), ClassLoggerConfigurationType.F_LEVEL,
                        AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> model) {
                        ClassLoggersContentPanel.this.itemDetailsPerformed(target, model);
                    }
                }
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<ClassLoggerConfigurationType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<ClassLoggerConfigurationType>> item) {

        return new ClassLoggerDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_MESSAGE_TEMPLATE_LOCALIZED_CONTENT_PANEL;    // todo fix
    }
}
