/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import com.evolveum.midpoint.gui.api.component.wizard.WizardListener;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-resource-test")
@PanelInstance(identifier = "cdw-resource-test",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.resourceTest", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ResourceTestConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> implements WizardListener {

    private static final String PANEL_TYPE = "cdw-resource-test";

    private static final String ID_PANEL = "panel";

    private LoadableModel<String> resourceOidModel;
    private boolean nextButtonVisible = false;

    public ResourceTestConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initLayout();
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);
        wizard.addWizardListener(this);
    }

    private void createValuesModel() {
        resourceOidModel = new LoadableModel<>() {
            @Override
            protected String load() {
                try {
                    PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                            ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));
                    return resource.getValue().getRealValue().getOid();
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void initLayout() {
        getTextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getSubtextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        ResourceTestPanel waitingPanel = new ResourceTestPanel(ID_PANEL, resourceOidModel) {
            @Override
            protected void onFinishActionPerform(AjaxRequestTarget target) {
                nextButtonVisible = true;
                target.add(getButtonContainer());
            }

            @Override
            public Component getFeedbackPanel() {
                return getFeedback();
            }
        };
        waitingPanel.setOutputMarkupId(true);
        add(waitingPanel);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.resourceTest");
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleBehaviour(this::getNextButtonVisible);
    }

    private Boolean getNextButtonVisible() {
        return nextButtonVisible;
    }

    @Override
    protected boolean isSubmitEnable() {
        return false;
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        if (!ResourceTestConnectorStepPanel.this.equals(newStep)) {
            return;
        }

        ((ResourceTestPanel)get(ID_PANEL)).refresh();
    }

    @Override
    public boolean isCompleted() {

        try {
            ObjectDetailsModels<ResourceType> resourceModel = ConnectorDevelopmentWizardUtil.getTestingResourceModel(
                    getDetailsModel(), PANEL_TYPE);
            OperationalStateType stateBean = resourceModel.getObjectType().getOperationalState();
            return stateBean != null && stateBean.getLastAvailabilityStatus() != null && stateBean.getLastAvailabilityStatus() == AvailabilityStatusType.UP;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }
}
