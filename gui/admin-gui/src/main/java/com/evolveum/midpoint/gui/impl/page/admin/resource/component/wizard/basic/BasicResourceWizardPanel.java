/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class BasicResourceWizardPanel extends AbstractWizardPanel<ResourceType, ResourceDetailsModel> {

    private Model<ResourceTemplate.TemplateType> templateType = Model.of();

    public BasicResourceWizardPanel(String id, WizardPanelHelper<ResourceType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        if (isStartWithChoiceTemplate()) {
            add(createChoiceFragment(createTemplateChoicePanel()));
        } else {
            add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModelBasic(createBasicSteps()))));
        }
    }

    @Override
    protected boolean initStartWithChoiceTemplate() {
        if (getHelper().getValueModel() != null
                && getHelper().getValueModel().getObject() != null) {
            PrismContainerValueWrapper<ResourceType> resource = getHelper().getValueModel().getObject();
            ItemPath path = ItemPath.create("connectorConfiguration", SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);
            try {
                PrismContainerWrapper<Containerable> configProperties = resource.findContainer(path);
                if (configProperties != null && !configProperties.getValues().isEmpty()
                        && !configProperties.getValue().getItems().isEmpty()) {
                    return false;
                }
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        }
        return super.initStartWithChoiceTemplate();
    }

    private Component createTemplateChoicePanel() {
        return new CreateResourceChoiceTemplatePanel(getIdOfChoicePanel(), getAssignmentHolderModel(), templateType) {
            @Override
            protected void onClickTile(AjaxRequestTarget target) {
                showChoiceFragment(target, createTemplatePanel());
            }
        };
    }

    protected CreateResourceTemplatePanel createTemplatePanel() {

        return new CreateResourceTemplatePanel(getIdOfChoicePanel(), templateType) {
            @Override
            protected void onTemplateSelectionPerformed(PrismObject<ResourceType> newObject, AjaxRequestTarget target) {
                reloadObjectDetailsModel(newObject);
                showWizardFragment(target, new WizardPanel(
                        getIdOfWizardPanel(), new WizardModelBasic(createBasicSteps())));
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                showChoiceFragment(target, createTemplateChoicePanel());
            }
        };
    }

    private void reloadObjectDetailsModel(PrismObject<ResourceType> newObject) {
        getAssignmentHolderModel().reset();
        getAssignmentHolderModel().reloadPrismObjectModel(newObject);
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicInformationResourceStepPanel(getAssignmentHolderModel()) {

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

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                getHelper().onExitPerformed(target);
            }

            @Override
            protected boolean isExitButtonVisible() {
                return !isStartWithChoiceTemplate();
            }

            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return new VisibleEnableBehaviour(BasicResourceWizardPanel.this::isStartWithChoiceTemplate);
            }
        });

        PrismObject<ConnectorType> connector = WebModelServiceUtils.loadObject(getAssignmentHolderModel().getObjectType().getConnectorRef(), getPageBase());

        CapabilityCollectionType capabilities
                = ProvisioningObjectsUtil.getNativeCapabilities(getAssignmentHolderModel().getObjectType(), getPageBase());

        if (connector != null && SchemaConstants.ICF_FRAMEWORK_URI.equals(connector.asObjectable().getFramework())) {

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
                steps.add(new ConfigurationStepPanel(getAssignmentHolderModel(), true) {
                    @Override
                    protected void onSubmitPerformed(AjaxRequestTarget target) {
                        target.add(getFeedback());
                        BasicResourceWizardPanel.this.onFinishBasicWizardPerformed(target);
                    }
                });
            }
        } else {
            steps.add(new ConfigurationStepPanel(getAssignmentHolderModel(), false) {
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

        return steps;
    }

    protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
    }
}
