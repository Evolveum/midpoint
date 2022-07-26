/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.ResourceWizardPreviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectTypes.ResourceObjectTypeTableWizardPanel;
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
public class BasicResourceWizardPanel extends BasePanel {

    private static final String ID_FRAGMENT = "fragment";
    private static final String ID_TEMPLATE_FRAGMENT = "templateFragment";
    private static final String ID_TEMPLATE = "template";
    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";

    private final ResourceDetailsModel resourceModel;

    public BasicResourceWizardPanel(String id, ResourceDetailsModel model) {
        super(id);
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createTemplateFragment());
    }

    protected Fragment createTemplateFragment() {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_TEMPLATE_FRAGMENT, BasicResourceWizardPanel.this);
        fragment.setOutputMarkupId(true);
        add(fragment);
        CreateResourceTemplatePanel templatePanel = new CreateResourceTemplatePanel(ID_TEMPLATE) {

            @Override
            protected void onTemplateSelectionPerformed(PrismObject<ResourceType> newObject, AjaxRequestTarget target) {
                reloadObjectDetailsModel(newObject);
                Fragment fragment = createBasicWizardFragment();
                fragment.setOutputMarkupId(true);
                BasicResourceWizardPanel.this.replace(fragment);
                target.add(fragment);
            }
        };
        fragment.add(templatePanel);
        return fragment;
    }

    private Fragment createBasicWizardFragment() {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_WIZARD_FRAGMENT, BasicResourceWizardPanel.this);
        Form mainForm = new Form(ID_MAIN_FORM);
        fragment.add(mainForm);
        WizardPanel wizard = new WizardPanel(ID_WIZARD, new WizardModel(createBasicSteps()));
        wizard.setOutputMarkupId(true);
        mainForm.add(wizard);
        return fragment;
    }

    private void reloadObjectDetailsModel(PrismObject<ResourceType> newObject) {
        getResourceModel().reset();
        getResourceModel().reloadPrismObjectModel(newObject);
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSettingStepPanel(getResourceModel()) {

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                Fragment fragment = createTemplateFragment();
                fragment.setOutputMarkupId(true);
                BasicResourceWizardPanel.this.replace(fragment);
                target.add(fragment);

                return false;
            }

            @Override
            protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        });

        PrismObject<ConnectorType> connector = WebModelServiceUtils.loadObject(getResourceModel().getObjectType().getConnectorRef(), getPageBase());

        if (connector != null && SchemaConstants.ICF_FRAMEWORK_URI.equals(connector.asObjectable().getFramework())) {
            CapabilityCollectionType capabilities
                    = WebComponentUtil.getNativeCapabilities(getResourceModel().getObjectType(), getPageBase());

            if (capabilities.getDiscoverConfiguration() != null) {
                steps.add(new PartialConfigurationStepPanel(getResourceModel()));
                steps.add(new DiscoveryStepPanel(getResourceModel()) {
                    @Override
                    protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                        BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
                    }
                });
            } else {
                steps.add(new ConfigurationStepPanel(getResourceModel()) {
                    @Override
                    protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                        BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
                    }
                });
            }

            if (capabilities.getSchema() != null) {
                steps.add(new SelectObjectClassesStepPanel(getResourceModel()) {
                    @Override
                    protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                        BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
                    }
                });
            }
        }

        return steps;
    }

    protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
    }
}
