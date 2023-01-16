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
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "globalPolicyRulePanel")
@PanelInstance(
        identifier = "globalPolicyRulePanel",
        applicableForType = GlobalPolicyRuleType.class,
        display = @PanelDisplay(
                label = "GlobalPolicyRuleContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
@Counter(provider = GlobalPolicyRuleCounter.class)
public class GlobalPolicyRuleContentPanel extends MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType> {

    private IModel<PrismContainerWrapper<GlobalPolicyRuleType>> model;

    public GlobalPolicyRuleContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, GlobalPolicyRuleType.class, configurationType);

        this.model = PrismContainerWrapperModel.fromContainerWrapper(model.getObjectWrapperModel(), ItemPath.create(
                // todo nasty hack to get one instance of panel work for different containers (same type, different objects)
                // should be handled via @PanelInstances, but there's no way to configure it properly
                // If containerPath & type in {@link @PanelInstance} is used then there are too many NPE and ContainerPanelConfigurationType misconfigurations
                model.getObjectType() instanceof SystemConfigurationType ? SystemConfigurationType.F_GLOBAL_POLICY_RULE :TagType.F_POLICY_RULE
        ));
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<GlobalPolicyRuleType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<GlobalPolicyRuleType>, String>> createDefaultColumns() {
        return Arrays.asList(
                new PrismPropertyWrapperColumn<>(getContainerModel(), GlobalPolicyRuleType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<GlobalPolicyRuleType>> model) {
                        GlobalPolicyRuleContentPanel.this.itemDetailsPerformed(target, model);
                    }
                },
                new PrismContainerWrapperColumn<>(getContainerModel(), GlobalPolicyRuleType.F_POLICY_CONSTRAINTS, getPageBase()),
                new PrismContainerWrapperColumn<>(getContainerModel(), GlobalPolicyRuleType.F_POLICY_ACTIONS, getPageBase()),
                new PrismPropertyWrapperColumn<>(getContainerModel(), GlobalPolicyRuleType.F_POLICY_SITUATION,
                        AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
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
    protected IModel<PrismContainerWrapper<GlobalPolicyRuleType>> getContainerModel() {
        return model;
    }

    @Override
    protected MultivalueContainerDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<GlobalPolicyRuleType>> item) {

        return new GlobalPolicyRuleDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_GLOBAL_POLICY_RULE_CONTENT;
    }
}
