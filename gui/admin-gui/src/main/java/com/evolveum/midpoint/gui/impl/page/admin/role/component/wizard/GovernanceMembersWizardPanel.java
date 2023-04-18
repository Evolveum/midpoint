/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.GovernanceCardsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceUncategorizedPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */

@PanelType(name = "arw-governance")
@PanelInstance(identifier = "arw-governance",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "GovernanceMembersWizardPanel.title", icon = "fa fa-users"))
public class GovernanceMembersWizardPanel extends AbstractWizardBasicPanel<FocusDetailsModels<RoleType>> {

    public static final String PANEL_TYPE = "arw-governance";
    private static final String ID_TABLE = "table";

    public GovernanceMembersWizardPanel(String id, FocusDetailsModels<RoleType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        GovernanceCardsPanel table = new GovernanceCardsPanel(ID_TABLE, getAssignmentHolderDetailsModel(), getConfiguration()){
            protected String getStorageKeyTabSuffix() {
                return getConfiguration()  == null ? PANEL_TYPE : super.getStorageKeyTabSuffix();
            }

            @Override
            protected Class<? extends FocusType> getSearchableType() {
                return UserType.class;
            }

            @Override
            protected Behavior createCardDetailsButtonBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected String getCssForCardUnassignButton(String defaultCss) {
                return "btn btn-link mt-3 m-auto";
            }

            @Override
            protected List<InlineMenuItem> createCardHeaderMenuActions() {
                return List.of();
            }

            protected List<InlineMenuItem> createToolbarMenuActions() {
                List<InlineMenuItem> actions = new ArrayList<>();
                createUnselectAllAction(actions);
                return actions;
            }

            protected String getTileCssClasses() {
                return "col-xs-5i col-sm-5i col-md-5i col-lg-4 col-xl-3 col-xxl-3 px-4 mb-3";
            }

            @Override
            protected WebMarkupContainer getFeedback() {
                return GovernanceMembersWizardPanel.this.getFeedback();
            }

            protected CompiledObjectCollectionView getObjectCollectionView() {
                ContainerPanelConfigurationType config = getPanelConfiguration();
                if (config == null) {
                    return null;
                }
                GuiObjectListViewType listViewType = config.getListView();
                if (listViewType == null) {
                    listViewType = config.beginListView();
                }

                if (listViewType.getSearchBoxConfiguration() == null) {
                    listViewType.beginSearchBoxConfiguration();
                }

                if (listViewType.getSearchBoxConfiguration().getObjectTypeConfiguration() == null) {
                    listViewType.getSearchBoxConfiguration().beginObjectTypeConfiguration();
                }

                if (listViewType.getSearchBoxConfiguration().getObjectTypeConfiguration().getSupportedTypes().isEmpty()) {
                    listViewType
                            .getSearchBoxConfiguration()
                            .getObjectTypeConfiguration()
                            .getSupportedTypes()
                            .add(UserType.COMPLEX_TYPE);
                }

                return WebComponentUtil.getCompiledObjectCollectionView(listViewType, config, getPageBase());
            }

            @Override
            protected String getButtonTranslationPrefix() {
                return "MembersWizardPanel.button";
            }
        };
        table.setOutputMarkupId(true);

        add(table);
    }

    ContainerPanelConfigurationType getConfiguration(){
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("GovernanceMembersWizardPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("GovernanceMembersWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("GovernanceMembersWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }
}
