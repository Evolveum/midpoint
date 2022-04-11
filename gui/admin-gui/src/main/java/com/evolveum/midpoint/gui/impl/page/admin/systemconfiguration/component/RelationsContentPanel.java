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
@PanelType(name = "relationsPanel")
@PanelInstance(
        identifier = "relationsPanel",
        applicableForType = RoleManagementConfigurationType.class,
        display = @PanelDisplay(
                label = "RelationsContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        )
)
@Counter(provider = RelationsCounter.class)
public class RelationsContentPanel extends MultivalueContainerListPanelWithDetailsPanel<RelationDefinitionType> {

    private IModel<PrismContainerWrapper<RelationDefinitionType>> model;

    public RelationsContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, RelationDefinitionType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_ROLE_MANAGEMENT,
                RoleManagementConfigurationType.F_RELATIONS,
                RelationsDefinitionType.F_RELATION
        ));
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<RelationDefinitionType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<RelationDefinitionType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), RelationDefinitionType.F_REF,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<RelationDefinitionType>> model) {
                        RelationsContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), RelationDefinitionType.F_DESCRIPTION,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase())
        );
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<RelationDefinitionType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<RelationDefinitionType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<RelationDefinitionType>> item) {

        return new RelationDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_RELATIONS_CONTENT;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }
}
