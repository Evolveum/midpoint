/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceUncategorizedPanel;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.function.Consumer;

/**
 * @author lskublik
 */
public class PreviewResourceObjectTypeDataWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final String PANEL_TYPE = "resourceUncategorized";

    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectType;

    public PreviewResourceObjectTypeDataWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectType) {
        super(id, model);
        this.resourceObjectType = resourceObjectType;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        getTable().getTable().setShowAsCard(false);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
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
            protected QName getDefaultObjectClass() {
                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = getResourceObjectType().getObject();
                if (objectType != null) {
                    ResourceObjectTypeDefinitionType objectTypeBean = objectType.getRealValue();
                    if (objectTypeBean != null) {
                        if (objectTypeBean.getDelineation() != null && objectTypeBean.getDelineation().getObjectClass() != null) {
                            return objectTypeBean.getDelineation().getObjectClass();
                        }
                    }
                }
                return super.getDefaultObjectClass();
            }

            @Override
            protected boolean isEnabledInlineMenu() {
                return false;
            }

            @Override
            protected Consumer<Task> createProviderSearchTaskCustomizer() {
                return (Consumer<Task> & Serializable) (task) -> task.setExecutionMode(TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT);
            }

            @Override
            protected boolean isTaskButtonVisible() {
                return false;
            }
        };
        table.setOutputMarkupId(true);
        form.add(table);
    }

    private ContainerPanelConfigurationType getConfiguration(){
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                PANEL_TYPE);
    }


    protected IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getResourceObjectType() {
        return resourceObjectType;
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.title");
    }

        @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.subText");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("PreviewResourceObjectTypeDataWizardPanel.text");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-8";
    }

    public ShadowTablePanel getTable() {
        ResourceUncategorizedPanel panel =
                (ResourceUncategorizedPanel) get(createComponentPath(ID_FORM, ID_TABLE));
        if (panel == null) {
            return null;
        }
        return panel.getShadowTable();
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("PageBase.button.back");
    }
}
