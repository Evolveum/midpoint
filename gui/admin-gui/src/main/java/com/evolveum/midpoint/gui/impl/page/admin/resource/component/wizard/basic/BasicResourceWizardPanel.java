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
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class BasicResourceWizardPanel extends BasePanel {

    private static final String ID_FRAGMENTS = "fragments";
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
        Fragment fragment = new Fragment(ID_FRAGMENTS, ID_TEMPLATE_FRAGMENT, BasicResourceWizardPanel.this);
        fragment.setOutputMarkupId(true);
        add(fragment);
        CreateResourceTemplatePanel templatePanel = new CreateResourceTemplatePanel(ID_TEMPLATE) {

            @Override
            protected void onTemplateChosePerformed(PrismObject<ResourceType> newObject, AjaxRequestTarget target) {
                reloadObjectDetailsModel(newObject);
                Fragment fragment = createWizardFragment();
                fragment.setOutputMarkupId(true);
                BasicResourceWizardPanel.this.replace(fragment);
                target.add(fragment);
            }
        };
        fragment.add(templatePanel);
        return fragment;
    }

    private Fragment createWizardFragment() {
        return new DetailsFragment(ID_FRAGMENTS, ID_WIZARD_FRAGMENT, BasicResourceWizardPanel.this) {

            @Override
            protected void initFragmentLayout() {
                Form mainForm = new Form(ID_MAIN_FORM);
                add(mainForm);
                WizardPanel wizard = new WizardPanel(ID_WIZARD, new WizardModel(createSteps()));
                wizard.setOutputMarkupId(true);
                mainForm.add(wizard);
            }
        };
    }

    private void reloadObjectDetailsModel(PrismObject<ResourceType> newObject) {
        getResourceModel().reset();
        getResourceModel().reloadPrismObjectModel(newObject);
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    private List<WizardStep> createSteps() {
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
                BasicResourceWizardPanel.this.onFinishWizardPerformed(target);
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
                        BasicResourceWizardPanel.this.onFinishWizardPerformed(target);
                    }
                });
            } else {
                steps.add(new ConfigurationStepPanel(getResourceModel()) {
                    @Override
                    protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                        BasicResourceWizardPanel.this.onFinishWizardPerformed(target);
                    }
                });
            }

            if (capabilities.getSchema() != null) {
                steps.add(new SelectObjectClassesStepPanel(getResourceModel()) {
                    @Override
                    protected void onFinishWizardPerformed(AjaxRequestTarget target) {
                        BasicResourceWizardPanel.this.onFinishWizardPerformed(target);
                    }
                });
            }
        }

        return steps;
    }

    protected void onFinishWizardPerformed(AjaxRequestTarget target) {
    }
}
