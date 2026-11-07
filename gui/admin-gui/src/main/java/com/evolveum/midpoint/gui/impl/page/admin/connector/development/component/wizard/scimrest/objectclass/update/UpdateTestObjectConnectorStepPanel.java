/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractTestObjectOperationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectFlowState;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for modifying the tested object in the external system when testing the Update capability.
 */
@PanelType(name = "cdw-update-test-object")
@PanelInstance(identifier = "cdw-update-test-object",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.updateTestObject", icon = "fa fa-wrench"),
        containerPath = "empty")
public class UpdateTestObjectConnectorStepPanel extends AbstractTestObjectOperationConnectorStepPanel {

    private static final Trace LOGGER = TraceManager.getTrace(UpdateTestObjectConnectorStepPanel.class);

    private static final String OP_REVERT_ALL = UpdateTestObjectConnectorStepPanel.class.getName() + ".revertAll";

    private static final String PANEL_TYPE = "cdw-update-test-object";

    public UpdateTestObjectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> getFlowState().getTestedObjectModel().getObject() != null;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.updateTestObject");
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
    protected IModel<ShadowWrapper> getTestObjectWrapperModel() {
        return getFlowState().getTestedObjectModel();
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        customButtons.add(createRevertAllButton(customButtons.newChildId()));
    }

    @Override
    protected boolean isSubmitEnable() {
        return hasChanges();
    }

    @Override
    protected String executeOperation(ObjectDelta<ShadowType> delta, Task task, OperationResult result)
            throws CommonException {
        getDetailsModel().getConnectorDevelopmentOperation()
                .modifyTestObject(delta.getOid(), delta.getModifications(), task, result);
        return delta.getOid();
    }

    @Override
    protected String getScriptPanelType() {
        return UpdateScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    protected String getObjectNotRetrievedKey() {
        return "UpdateTestObjectConnectorStepPanel.objectNotRetrieved";
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.updateTestObject.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.updateTestObject.subText");
    }

    private AjaxButton createRevertAllButton(String id) {
        AjaxButton button = new AjaxButton(id, createStringResource("UpdateTestObjectConnectorStepPanel.revertAll")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                revertAllPerformed(target);
            }
        };
        button.add(new VisibleBehaviour(this::hasChanges));
        return button;
    }

    private void revertAllPerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_REVERT_ALL);
        OperationResult result = task.getResult();
        try {
            getFlowState().getTestedObjectModel().setObject(ConnectorDevelopmentWizardUtil.createShadowWrapperForModification(
                    getPageBase(), getFlowState().getTestedObjectModel().getObject().getObjectOld().clone(), task, result));
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't revert changes of the test object", e);
            result.recordFatalError(e);
            getPageBase().showResult(result);
            target.add(getFeedback());
            return;
        }
        target.add(getAttributesPanel(), getCustomButtonsContainer());
    }

    private boolean hasChanges() {
        ShadowWrapper wrapper = getFlowState().getTestedObjectModel().getObject();
        try {
            return wrapper != null && !wrapper.getObjectDelta().isEmpty();
        } catch (CommonException e) {
            throw new IllegalStateException("Couldn't compute changes of the test object", e);
        }
    }
}
