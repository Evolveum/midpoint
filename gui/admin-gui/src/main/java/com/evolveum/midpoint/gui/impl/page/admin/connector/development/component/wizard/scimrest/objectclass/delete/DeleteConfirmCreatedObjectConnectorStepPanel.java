/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.delete;

import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractConfirmCreatedObjectConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectFlowState;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step presenting the object created in the external system for testing the Delete capability.
 */
@PanelType(name = "cdw-delete-confirm-created-object")
@PanelInstance(identifier = "cdw-delete-confirm-created-object",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.deleteConfirmCreatedObject", icon = "fa fa-wrench"),
        containerPath = "empty")
public class DeleteConfirmCreatedObjectConnectorStepPanel extends AbstractConfirmCreatedObjectConnectorStepPanel {

    private static final String CLASS_DOT = DeleteConfirmCreatedObjectConnectorStepPanel.class.getName() + ".";
    private static final String OP_DELETE_OBJECT = CLASS_DOT + "deleteObject";

    private static final String PANEL_TYPE = "cdw-delete-confirm-created-object";

    public DeleteConfirmCreatedObjectConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.deleteConfirmCreatedObject");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.DELETE);
    }

    @Override
    protected String getScriptPanelType() {
        return DeleteScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("DeleteConfirmCreatedObjectConnectorStepPanel.proceed");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        deleteTestedObjectPerformed(target);
    }

    private void deleteTestedObjectPerformed(AjaxRequestTarget target) {
        ShadowType remainingObject = changeExternalSystem(OP_DELETE_OBJECT, getScriptPanelType(),
                "DeleteSelectTestObjectConnectorStepPanel.deleteFailed", target, this::deleteAndFind);
        if (isOperationFailed()) {
            return;
        }
        if (remainingObject != null) {
            getPageBase().error(getString("DeleteSelectTestObjectConnectorStepPanel.objectStillExists"));
            target.add(getFeedback());
            return;
        }
        getFlowState().objectDeleted(getTestedObject());
        nextWithoutConfirmationPerformed(target);
    }

    private ShadowType deleteAndFind(Task task) throws CommonException {
        ConnectorDevelopmentOperation operation = getDetailsModel().getConnectorDevelopmentOperation();
        operation.deleteTestObjects(Set.of(getTestedObject().getOid()), task, task.getResult());
        getDetailsModel().reloadPrismObjectByOid();
        return operation.findTestingResourceObject(getObjectClassName(),
                ConnectorDevelopmentWizardUtil.formatPrimaryIdentifiers(getTestedObject()), task, task.getResult());
    }
}
