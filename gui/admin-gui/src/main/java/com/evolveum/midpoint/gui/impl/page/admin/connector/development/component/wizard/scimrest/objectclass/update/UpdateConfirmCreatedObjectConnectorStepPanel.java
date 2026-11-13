/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractConfirmCreatedObjectConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectFlowState;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * Wizard step presenting the object created in the external system for testing the Update capability.
 */
@PanelType(name = "cdw-update-confirm-created-object")
@PanelInstance(identifier = "cdw-update-confirm-created-object",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.updateConfirmCreatedObject", icon = "fa fa-wrench"),
        containerPath = "empty")
public class UpdateConfirmCreatedObjectConnectorStepPanel extends AbstractConfirmCreatedObjectConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-update-confirm-created-object";

    /**
     * Creates the step presenting the created object.
     *
     * @param helper wizard panel helper
     * @param valueModel object class being developed
     * @param flowState state of the test object shared by the steps of the capability test
     */
    public UpdateConfirmCreatedObjectConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.updateConfirmCreatedObject");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.UPDATE);
    }

    @Override
    protected String getScriptPanelType() {
        return UpdateScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("UpdateConfirmCreatedObjectConnectorStepPanel.proceed");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        nextWithoutConfirmationPerformed(target);
    }
}
