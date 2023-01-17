/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class MembersWizardPanel extends AbstractWizardBasicPanel<FocusDetailsModels<RoleType>> {

    public static final String PANEL_TYPE = "roleWizard-members";
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
            protected List<QName> getSupportedRelations() {
                return getSupportedMembersTabRelations();
            }

            @Override
            protected List<InlineMenuItem> createRowActions() {
                List<InlineMenuItem> menu = new ArrayList<>();
                createUnassignMemberRowAction(menu);
                return menu;
            }

            @Override
            protected List<Component> createToolbarButtonList(String buttonId, List defaultToolbarList) {
                return List.of(createAssignButton(buttonId), createUnassignButton(buttonId));
            }

            protected String getStorageKeyTabSuffix() {
                return getConfiguration() == null ? PANEL_TYPE : super.getStorageKeyTabSuffix();
            }

            @Override
            protected boolean isVisibleAdvanceSearchItem() {
                return false;
            }

            @Override
            protected List<QName> getDefaultSupportedObjectTypes(boolean includeAbstractTypes) {
                return List.of(UserType.COMPLEX_TYPE);
            }

            @Override
            protected Class<UserType> getChoiceForAllTypes() {
                return UserType.class;
            }

            @Override
            protected SearchBoxConfigurationType getAdditionalPanelConfig() {
                SearchBoxConfigurationType searchConfig = super.getAdditionalPanelConfig();
                if (searchConfig == null) {
                    searchConfig = new SearchBoxConfigurationType();
                }

                if (searchConfig.getObjectTypeConfiguration() == null) {
                    ObjectTypeSearchItemConfigurationType objTypeConfig = new ObjectTypeSearchItemConfigurationType();
                    objTypeConfig.getSupportedTypes().addAll(getDefaultSupportedObjectTypes(false));
                    objTypeConfig.setDefaultValue(UserType.COMPLEX_TYPE);
                    objTypeConfig.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                    searchConfig.setObjectTypeConfiguration(objTypeConfig);
                }

                if (searchConfig.getRelationConfiguration() == null && getSupportedRelations().size() == 1) {
                    RelationSearchItemConfigurationType relationConfig = new RelationSearchItemConfigurationType();
                    relationConfig.setVisibility(UserInterfaceElementVisibilityType.HIDDEN);
                    relationConfig.getSupportedRelations().addAll(getSupportedRelations());
                    relationConfig.setDefaultValue(getSupportedRelations().get(0));
                    searchConfig.setRelationConfiguration(relationConfig);
                }
                return searchConfig;
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
}
