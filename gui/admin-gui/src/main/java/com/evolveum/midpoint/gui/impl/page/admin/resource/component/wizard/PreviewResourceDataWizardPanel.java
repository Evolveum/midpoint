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
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.function.Consumer;

/**
 * @author lskublik
 */
public class PreviewResourceDataWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "resourceUncategorized";
    private static final String ID_TABLE = "table";
    private static final String ID_FORM = "form";

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

        getTable().getTable().setShowAsCard(false);
    }

    private void initLayout() {

        MidpointForm form = new MidpointForm(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        ResourceUncategorizedPanel table = new ResourceUncategorizedPanel(
                ID_TABLE, getAssignmentHolderDetailsModel(), getConfiguration()) {
            @Override
            protected VisibleEnableBehaviour getTitleVisibleBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected boolean isShadowDetailsEnabled() {
                return false;
            }

            @Override
            protected boolean isEnabledInlineMenu() {
                return false;
            }

            @Override
            protected Consumer<Task> createProviderSearchTaskCustomizer() {
                return (Consumer<Task> & Serializable) (task) -> task.setExecutionMode(TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT);
            }
        };
        table.setOutputMarkupId(true);
        form.add(table);
    }

    public ShadowTablePanel getTable() {
        ResourceUncategorizedPanel panel =
                (ResourceUncategorizedPanel) get(createComponentPath(ID_FORM, ID_TABLE));
        if (panel == null) {
            return null;
        }
        return panel.getShadowTable();
    }

    private ContainerPanelConfigurationType getConfiguration(){
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
