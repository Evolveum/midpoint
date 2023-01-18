/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ResourceWizardPanel extends BasePanel {

    private static final String ID_WIZARD_PANEL = "wizardPanel";

    private final ResourceDetailsModel resourceModel;

    public ResourceWizardPanel(String id, ResourceDetailsModel model) {
        super(id);
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createBasicWizard());
    }

    private BasicResourceWizardPanel createBasicWizard() {
        BasicResourceWizardPanel basicWizard = new BasicResourceWizardPanel(ID_WIZARD_PANEL, getResourceModel()) {

            @Override
            protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
                ResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        };
        basicWizard.setOutputMarkupId(true);
        return basicWizard;
    }

    protected ResourceObjectTypeWizardPanel createObjectTypeWizard(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {

        WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper = new WizardPanelHelper<>(getResourceModel(), valueModel) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                showWizardPanel(createTablePanel(), target);
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return ResourceWizardPanel.this.onSaveResourcePerformed(target);
            }
        };
        ResourceObjectTypeWizardPanel wizard = new ResourceObjectTypeWizardPanel(ID_WIZARD_PANEL, helper);
        wizard.setOutputMarkupId(true);
        return wizard;
    }

    protected ResourceObjectTypeTableWizardPanel createTablePanel() {
        return new ResourceObjectTypeTableWizardPanel(ID_WIZARD_PANEL, getResourceModel()) {
            @Override
            protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
                showWizardPanel(createObjectTypeWizard(valueModel), target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        };
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSaveResourcePerformed(target);
        if (!result.isError()) {
            WebComponentUtil.createToastForCreateObject(target, ResourceType.COMPLEX_TYPE);
            exitToPreview(target);
        }
    }

    private PreviewResourceDataWizardPanel createPreviewResourceDataWizardPanel() {
        return new PreviewResourceDataWizardPanel(ID_WIZARD_PANEL, getResourceModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        };
    }

    private void exitToPreview(AjaxRequestTarget target) {
        ResourceWizardPreviewPanel preview = new ResourceWizardPreviewPanel(ID_WIZARD_PANEL, getResourceModel()) {
            @Override
            protected void onTileClickPerformed(ResourceWizardPreviewPanel.PreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case PREVIEW_DATA:
                        showWizardPanel(createPreviewResourceDataWizardPanel(), target);
                        break;
                    case CONFIGURE_OBJECT_TYPES:
                        showWizardPanel(createTablePanel(), target);
                        break;
                }
            }
        };
        preview.setOutputMarkupId(true);
        ResourceWizardPanel.this.replace(preview);
        target.add(preview);
    }

    private void showWizardPanel(Component wizard, AjaxRequestTarget target) {
        ResourceWizardPanel.this.addOrReplace(wizard);
        target.add(wizard);
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }
}
