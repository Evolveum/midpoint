/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectTypes.ResourceObjectTypeTableWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectTypes.ResourceObjectTypeWizardPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ResourceWizardPanel extends BasePanel {

    //    private static final String ID_FRAGMENT = "fragment";
//    private static final String ID_PREVIEW_FRAGMENT = "previewFragment";
//    private static final String ID_PREVIEW = "preview";
//    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
//    private static final String ID_MAIN_FORM = "mainForm";
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
                ResourceWizardPreviewPanel preview = createWizardPreviewPanel();
                ResourceWizardPanel.this.replace(preview);
                target.add(preview);
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
            ResourceWizardPreviewPanel preview = createWizardPreviewPanel();
            ResourceWizardPanel.this.replace(preview);
            target.add(preview);
        }
    }

    private ResourceWizardPreviewPanel createWizardPreviewPanel() {
        ResourceWizardPreviewPanel preview = new ResourceWizardPreviewPanel(ID_WIZARD_PANEL, getResourceModel()) {
            @Override
            protected void onResourceTileClick(ResourceWizardPreviewPanel.PreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case CONFIGURE_OBJECT_TYPES:
                        ResourceObjectTypeWizardPanel objectTypeWizard = createObjectTypeWizard();
                        ResourceWizardPanel.this.replace(objectTypeWizard);
                        target.add(objectTypeWizard);
                        break;
                }
            }
        };
        preview.setOutputMarkupId(true);
        return preview;
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }
}
