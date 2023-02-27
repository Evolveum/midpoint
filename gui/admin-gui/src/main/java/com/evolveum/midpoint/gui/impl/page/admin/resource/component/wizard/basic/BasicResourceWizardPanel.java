/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
public class BasicResourceWizardPanel extends AbstractWizardPanel<ResourceType, ResourceDetailsModel> {

//    private static final String ID_FRAGMENT = "fragment";
//    private static final String ID_TEMPLATE_FRAGMENT = "templateFragment";
//    private static final String ID_TEMPLATE = "template";
//    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
//    private static final String ID_MAIN_FORM = "mainForm";
//    private static final String ID_WIZARD = "wizard";

//    private final ResourceDetailsModel resourceModel;

    public BasicResourceWizardPanel(String id, WizardPanelHelper<ResourceType, ResourceDetailsModel> helper) {
        super(id, helper);
//        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        add(createChoiceFragment(createTemplatePanel()));
    }

    protected CreateResourceTemplatePanel createTemplatePanel() {
        return new CreateResourceTemplatePanel(getIdOfChoicePanel()) {

            @Override
            protected void onTemplateSelectionPerformed(PrismObject<ResourceType> newObject, AjaxRequestTarget target) {
                reloadObjectDetailsModel(newObject);
                showWizardFragment(target, new WizardPanel(
                        getIdOfWizardPanel(), new WizardModel(createBasicSteps())));
            }
        };
    }

    private void reloadObjectDetailsModel(PrismObject<ResourceType> newObject) {
        getAssignmentHolderModel().reset();
        getAssignmentHolderModel().reloadPrismObjectModel(newObject);
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicInformationStepPanel(getAssignmentHolderModel()) {

            @Override
            public boolean onBackPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTemplatePanel());

                return false;
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        });

        PrismObject<ConnectorType> connector = WebModelServiceUtils.loadObject(getAssignmentHolderModel().getObjectType().getConnectorRef(), getPageBase());

        if (connector != null && SchemaConstants.ICF_FRAMEWORK_URI.equals(connector.asObjectable().getFramework())) {
            CapabilityCollectionType capabilities
                    = WebComponentUtil.getNativeCapabilities(getAssignmentHolderModel().getObjectType(), getPageBase());

            if (capabilities.getDiscoverConfiguration() != null) {
                steps.add(new PartialConfigurationStepPanel(getAssignmentHolderModel()));
                steps.add(new DiscoveryStepPanel(getAssignmentHolderModel()) {
                    @Override
                    protected void onSubmitPerformed(AjaxRequestTarget target) {
                        target.add(getFeedback());
                        BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
                    }
                });
            } else {
                steps.add(new ConfigurationStepPanel(getAssignmentHolderModel()) {
                    @Override
                    protected void onSubmitPerformed(AjaxRequestTarget target) {
                        target.add(getFeedback());
                        BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
                    }
                });
            }

            if (capabilities.getSchema() != null) {
                steps.add(new SelectObjectClassesStepPanel(getAssignmentHolderModel()) {
                    @Override
                    protected void onSubmitPerformed(AjaxRequestTarget target) {
                        target.add(getFeedback());
                        super.onSubmitPerformed(target);
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
