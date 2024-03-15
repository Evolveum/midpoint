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
@PanelType(name = "objectCollectionViewsPanel")
@PanelInstance(
        identifier = "objectCollectionViewsPanel",
        applicableForType = AdminGuiConfigurationType.class,
        display = @PanelDisplay(
                label = "ObjectCollectionViewsContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 40
        )
)
@Counter(provider = ObjectCollectionViewsCounter.class)
public class ObjectCollectionViewsContentPanel extends MultivalueContainerListPanelWithDetailsPanel<GuiObjectListViewType> {

    private IModel<PrismContainerWrapper<GuiObjectListViewType>> model;

    public ObjectCollectionViewsContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, GuiObjectListViewType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS,
                GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW),
                (SerializableSupplier<PageBase>) () -> getPageBase());
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<GuiObjectListViewType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<GuiObjectListViewType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), GuiObjectListViewType.F_IDENTIFIER,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<GuiObjectListViewType>> model) {
                        ObjectCollectionViewsContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), GuiObjectListViewType.F_TYPE,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismPropertyWrapperColumn<>(getContainerModel(), GuiObjectListViewType.F_DISPLAY_ORDER,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase())
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<GuiObjectListViewType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<GuiObjectListViewType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<GuiObjectListViewType>> item) {

        return new ObjectCollectionViewsDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_OBJECT_COLLECTION_VIEWS;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
