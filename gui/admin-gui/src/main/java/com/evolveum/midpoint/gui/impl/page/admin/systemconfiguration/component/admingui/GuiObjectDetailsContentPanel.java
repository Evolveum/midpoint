/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.admingui;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
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
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "objectDetailsPanel")
@PanelInstance(
        identifier = "objectDetailsPanel",
        applicableForType = AdminGuiConfigurationType.class,
        display = @PanelDisplay(
                label = "GuiObjectDetailsContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 50
        )
)
@Counter(provider = GuiObjectDetailsCounter.class)
public class GuiObjectDetailsContentPanel extends MultivalueContainerListPanelWithDetailsPanel<GuiObjectDetailsPageType> {

    private IModel<PrismContainerWrapper<GuiObjectDetailsPageType>> model;

    public GuiObjectDetailsContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, GuiObjectDetailsPageType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                AdminGuiConfigurationType.F_OBJECT_DETAILS,
                GuiObjectDetailsSetType.F_OBJECT_DETAILS_PAGE),
                (SerializableSupplier<PageBase>) () -> getPageBase());
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<GuiObjectDetailsPageType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<GuiObjectDetailsPageType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), GuiObjectDetailsPageType.F_TYPE,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<GuiObjectDetailsPageType>> model) {
                        GuiObjectDetailsContentPanel.this.itemDetailsPerformed(target, model);
                    }
                }
//                new PrismPropertyWrapperColumn<>(getContainerModel(), GuiObjectDetailsPageType.F_ROLE_RELATION,
//                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase())
                // todo more columns
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<GuiObjectDetailsPageType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<GuiObjectDetailsPageType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<GuiObjectDetailsPageType>> item) {

        return new GuiObjectDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_GUI_OBJECT_DETAILS_CONTENT;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
