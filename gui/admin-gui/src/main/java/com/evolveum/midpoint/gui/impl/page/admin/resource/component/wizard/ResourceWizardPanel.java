/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.apache.wicket.ajax.AjaxRequestTarget;

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

    protected ResourceObjectTypeWizardPanel createObjectTypeWizard() {
        ResourceObjectTypeWizardPanel wizard = new ResourceObjectTypeWizardPanel(ID_WIZARD_PANEL, getResourceModel()) {
            @Override
            protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                return ResourceWizardPanel.this.onSaveResourcePerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                exitToPreview(target);
            }
        };
        wizard.setOutputMarkupId(true);
        return wizard;
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSaveResourcePerformed(target);
        if (!result.isError()) {
            exitToPreview(target);
        }
    }

    private PreviewResourceDataWizardPanel createPreviewResourceDataWizardPanel() {
        return new PreviewResourceDataWizardPanel(ID_WIZARD_PANEL, getResourceModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                exitToPreview(target);
            }
        };
    }

    private void exitToPreview(AjaxRequestTarget target) {
        ResourceWizardPreviewPanel preview = new ResourceWizardPreviewPanel(ID_WIZARD_PANEL, getResourceModel()) {
            @Override
            protected void onResourceTileClick(ResourceWizardPreviewPanel.PreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case PREVIEW_DATA:
                        PreviewResourceDataWizardPanel uncategorizedPanel = createPreviewResourceDataWizardPanel();
                        ResourceWizardPanel.this.addOrReplace(uncategorizedPanel);
                        target.add(uncategorizedPanel);
                        break;
                    case CONFIGURE_OBJECT_TYPES:
                        ResourceObjectTypeWizardPanel objectTypeWizard = createObjectTypeWizard();
                        ResourceWizardPanel.this.addOrReplace(objectTypeWizard);
                        target.add(objectTypeWizard);
                        break;
                }
            }
        };
        preview.setOutputMarkupId(true);
        ResourceWizardPanel.this.replace(preview);
        target.add(preview);
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }
}
