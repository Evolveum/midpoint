/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lskublik
 */

@PanelType(name = "arw-members")
@PanelInstance(identifier = "arw-members",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "MembersWizardPanel.title", icon = "fa fa-users"))
public class MembersWizardPanel extends AbstractWizardBasicPanel<FocusDetailsModels<RoleType>> {

    public static final String PANEL_TYPE = "arw-members";
    private static final String ID_TABLE = "table";

    public MembersWizardPanel(String id, FocusDetailsModels<RoleType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AbstractRoleMemberPanel table = new AbstractRoleMemberPanel(ID_TABLE, getAssignmentHolderDetailsModel(), getConfiguration()) {

            @Override
            protected void onInitialize() {
                super.onInitialize();
                getMemberTable().getTable().setShowAsCard(false);
                getMemberTable().getTable().add(AttributeAppender.replace("class", ""));
                getMemberTable().getTable().add(createRefreshBehaviour(getMemberTable().getObjectCollectionView()));
            }

            @Override
            protected List<InlineMenuItem> createRowActions() {
                List<InlineMenuItem> menu = new ArrayList<>();
                createUnassignMemberRowAction(menu);
                return menu;
            }

            @Override
            protected List<Component> createToolbarButtonList(String buttonId, List defaultToolbarList) {
                return List.of(
                        createAssignButton(buttonId),
                        createUnassignButton(buttonId),
                        createRefreshButton(buttonId),
                        createPlayPauseButton(buttonId));
            }

            protected String getStorageKeyTabSuffix() {
                return getConfiguration() == null ? PANEL_TYPE : super.getStorageKeyTabSuffix();
            }
            @Override
            protected CollectionPanelType getPanelType() {
                return CollectionPanelType.MEMBER_WIZARD;
            }

            @Override
            protected AjaxIconButton createAssignButton(String buttonId) {
                AjaxIconButton button = super.createAssignButton(buttonId);
                button.add(AttributeAppender.replace("class", "btn btn-primary"));
                button.showTitleAsLabel(true);
                return button;
            }

            @Override
            protected AjaxIconButton createUnassignButton(String buttonId) {
                AjaxIconButton button = super.createUnassignButton(buttonId);
                button.add(AttributeAppender.replace("class", "btn btn-outline-primary ml-2"));
                button.showTitleAsLabel(true);
                return button;
            }

            @Override
            protected void processTaskAfterOperation(Task task, AjaxRequestTarget target) {
                showMessageWithoutLinkForTask(task, target);
            }

            @Override
            protected WebMarkupContainer getFeedback() {
                return MembersWizardPanel.this.getFeedback();
            }

            @Override
            protected void unassignMembersPerformed(IModel rowModel, QName type, QueryScope scope, Collection relations, AjaxRequestTarget target) {
                super.unassignMembersPerformed(rowModel, type, scope, relations, target);
                target.add(getFeedback());
            }

            @Override
            protected void executeUnassign(AssignmentHolderType object, QName relation, AjaxRequestTarget target) {
                super.executeUnassign(object, relation, target);
                target.add(getFeedback());
            }

            @Override
            protected String getButtonTranslationPrefix() {
                return "MembersWizardPanel.button";
            }
        };
        table.setOutputMarkupId(true);

        add(table);
    }

    ContainerPanelConfigurationType getConfiguration() {
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("MembersWizardPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("MembersWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("MembersWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }
}
