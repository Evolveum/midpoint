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
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractSelectTestObjectConnectorStepPanel;
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
 * Wizard step for choosing the object to delete when testing the Delete capability.
 */
@PanelType(name = "cdw-delete-select-test-object")
@PanelInstance(identifier = "cdw-delete-select-test-object",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.deleteSelectTestObject", icon = "fa fa-wrench"),
        containerPath = "empty")
public class DeleteSelectTestObjectConnectorStepPanel extends AbstractSelectTestObjectConnectorStepPanel {

    private static final String CLASS_DOT = DeleteSelectTestObjectConnectorStepPanel.class.getName() + ".";
    private static final String OP_DELETE_OBJECT = CLASS_DOT + "deleteObject";

    private static final String PANEL_TYPE = "cdw-delete-select-test-object";

    public DeleteSelectTestObjectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.deleteSelectTestObject");
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
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.deleteSelectTestObject.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.deleteSelectTestObject.subText");
    }

    @Override
    protected String getSubmitIcon() {
        return isCreatingNewObject() ? super.getSubmitIcon() : "fa fa-cogs";
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        IModel<String> createLabel = super.getSubmitLabelModel();
        return () -> isCreatingNewObject()
                ? createLabel.getObject()
                : getString("DeleteSelectTestObjectConnectorStepPanel.executeScript");
    }

    @Override
    protected void onObjectSelected(AjaxRequestTarget target) {
        if (isCreatingNewObject()) {
            super.onObjectSelected(target);
            return;
        }
        deleteTestedObjectPerformed(target);
    }

    private void deleteTestedObjectPerformed(AjaxRequestTarget target) {
        ShadowType remainingObject = changeExternalSystem(OP_DELETE_OBJECT, DeleteScriptConnectorStepPanel.PANEL_TYPE,
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

    private ShadowType getTestedObject() {
        return getFlowState().getTestedObjectModel().getObject().getObjectOld().asObjectable();
    }
}
