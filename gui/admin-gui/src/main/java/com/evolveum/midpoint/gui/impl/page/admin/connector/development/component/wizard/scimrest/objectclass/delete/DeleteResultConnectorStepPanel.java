/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.delete;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractCapabilityTestStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectFlowState;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * Wizard step confirming that the tested object no longer exists after testing the Delete capability.
 */
@PanelType(name = "cdw-delete-verification")
@PanelInstance(identifier = "cdw-delete-verification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.deleteVerification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class DeleteResultConnectorStepPanel extends AbstractCapabilityTestStepPanel {

    private static final String PANEL_TYPE = "cdw-delete-verification";

    public DeleteResultConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> getFlowState().getDeletedObjectModel().getObject() != null;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.deleteVerification");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getTextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getSubtextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getButtonContainer().add(AttributeModifier.replace("class",
                "d-flex flex-column-reverse align-items-center mt-4 gap-3 wizard-actions-strip col-12"));
        getCustomButtonsContainer().add(AttributeModifier.append("class", "d-flex justify-content-center col-sm-5"));
        getSubmit().add(AttributeModifier.replace("class", "btn btn-primary flex-fill"));
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        customButtons.add(createObjectStillExistsButton(customButtons.newChildId()));
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.DELETE);
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("DeleteResultConnectorStepPanel.confirmAndFinish");
    }

    @Override
    protected String getSubmitIcon() {
        return "";
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        onNextPerformed(target);
    }

    private AjaxButton createObjectStillExistsButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("DeleteResultConnectorStepPanel.objectStillExists")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToScriptPerformed(target);
            }
        };
        button.add(AttributeModifier.replace("class", "btn btn-outline-primary flex-fill"));
        return button;
    }

    private void navigateToScriptPerformed(AjaxRequestTarget target) {
        if (!(getWizard() instanceof WizardModelWithParentSteps wizardModel)) {
            return;
        }
        wizardModel.setActiveStepWithinActivePart(DeleteScriptConnectorStepPanel.PANEL_TYPE);
        wizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
    }
}
