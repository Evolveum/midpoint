/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.policies;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
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
@PanelType(name = "objectPolicyPanel")
@PanelInstance(
        identifier = "objectPolicyPanel",
        applicableForType = ObjectPolicyConfigurationType.class,
        display = @PanelDisplay(
                label = "ObjectPolicyContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        )
)
@Counter(provider = ObjectPolicyCounter.class)
public class ObjectPolicyContentPanel extends MultivalueContainerListPanelWithDetailsPanel<ObjectPolicyConfigurationType> {

    private IModel<PrismContainerWrapper<ObjectPolicyConfigurationType>> model;

    public ObjectPolicyContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, ObjectPolicyConfigurationType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION
        ));
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<ObjectPolicyConfigurationType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ObjectPolicyConfigurationType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), ObjectPolicyConfigurationType.F_TYPE,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ObjectPolicyConfigurationType>> model) {
                        ObjectPolicyContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismPropertyWrapperColumn<>(getContainerModel(), ObjectPolicyConfigurationType.F_SUBTYPE,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismReferenceWrapperColumn<>(getContainerModel(), ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismReferenceWrapperColumn<>(getContainerModel(), ItemPath.create(ObjectPolicyConfigurationType.F_APPLICABLE_POLICIES, ApplicablePoliciesType.F_POLICY_GROUP_REF),
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()),
                new PrismContainerWrapperColumn<>(getContainerModel(), ItemPath.create(ObjectPolicyConfigurationType.F_LIFECYCLE_STATE_MODEL, LifecycleStateModelType.F_STATE), getPageBase()) {

                    @Override
                    public String getCssClass() {
                        return "mp-w-2";
                    }

                }
        );
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return getDefaultMenuActions();
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected IModel<PrismContainerWrapper<ObjectPolicyConfigurationType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<ObjectPolicyConfigurationType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<ObjectPolicyConfigurationType>> item) {

        return new ObjectPolicyDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_OBJECT_POLICY_CONTENT;
    }
}
