/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceUncategorizedPanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
public class PreviewResourceDataWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "resourceUncategorized";
    private static final String ID_TABLE = "table";

    public PreviewResourceDataWizardPanel(String id, ResourceDetailsModel model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        getTable().setShowAsCard(false);
    }

    private void initLayout() {
        ResourceUncategorizedPanel table = new ResourceUncategorizedPanel(ID_TABLE, getAssignmentHolderDetailsModel(), getConfiguration()) {

            @Override
            protected boolean isRepoSearch() {
                return false;
            }

            @Override
            protected boolean isResourceSearch() {
                return true;
            }

            @Override
            protected boolean isTaskButtonsContainerVisible() {
                return false;
            }

            @Override
            protected boolean isTopTableButtonsVisible() {
                return false;
            }

            @Override
            protected boolean isSourceChoiceVisible() {
                return false;
            }
        };
        table.setOutputMarkupId(true);

        add(table);
    }

    public BoxedTablePanel getTable() {
        ResourceUncategorizedPanel panel =
                (ResourceUncategorizedPanel) get(ID_TABLE);
        if (panel == null) {
            return null;
        }
        return panel.getTable();
    }

    ContainerPanelConfigurationType getConfiguration(){
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("PreviewResourceDataWizardPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("PreviewResourceDataWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("PreviewResourceDataWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-8";
    }
}
