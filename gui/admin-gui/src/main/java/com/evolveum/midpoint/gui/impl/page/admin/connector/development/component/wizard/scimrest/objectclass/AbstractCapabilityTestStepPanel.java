/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConfirmationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.CheckedFunction;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * Base step for testing a capability of the connector on the testing resource, with the common layout of such steps.
 */
public abstract class AbstractCapabilityTestStepPanel extends ScriptConfirmationPanel {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCapabilityTestStepPanel.class);

    private static final String OP_CREATE_NEW_OBJECT_WRAPPER =
            AbstractCapabilityTestStepPanel.class.getName() + ".createNewObjectWrapper";

    private static final String ID_OPERATION_FAILED = "operationFailed";

    /**
     * State of the test object shared by the steps of the capability test.
     */
    private final TestObjectFlowState flowState;

    /**
     * Model of the testing resource of the connector, built lazily on first use.
     */
    private final LoadableModel<ResourceDetailsModel> testingResourceModel = LoadableModel.create(this::createTestingResourceModel, false);

    /**
     * Message of the failed operation, shown in the failure alert; null when nothing failed.
     */
    private final IModel<String> failureModel = Model.of();

    /**
     * Alert of the failed operation, refreshed after every operation on the testing resource.
     */
    private OperationFailedPanel operationFailedPanel;

    /**
     * Creates the testing step.
     *
     * @param helper wizard panel helper
     * @param valueModel object class being developed
     * @param flowState state of the test object shared by the steps of the capability test
     */
    public AbstractCapabilityTestStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel);
        this.flowState = flowState;
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getBack().add(AttributeAppender.replace("class", "btn btn-link"));
        getSubmit().add(AttributeAppender.replace("class",
                () -> isSubmitEnable() ? "btn btn-primary" : "btn btn-primary disabled"));

        operationFailedPanel = createOperationFailedPanel();
        add(operationFailedPanel);
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }

    /**
     * Returns the state of the test object shared by the steps of the capability test.
     *
     * @return the shared state of the test object
     */
    protected final TestObjectFlowState getFlowState() {
        return flowState;
    }

    /**
     * Returns the model of the testing resource of the connector.
     *
     * @return the model of the testing resource
     */
    protected final ResourceDetailsModel getTestingResourceModel() {
        return testingResourceModel.getObject();
    }

    /**
     * Returns the message of the failed operation, shown in the failure alert.
     *
     * @return the failure message model
     */
    protected final IModel<String> getFailureModel() {
        return failureModel;
    }

    /**
     * Tells whether the last operation on the testing resource failed.
     *
     * @return true if it failed
     */
    protected final boolean isOperationFailed() {
        return failureModel.getObject() != null;
    }

    /**
     * Returns the name of the tested operation, as shown in the texts of the test.
     *
     * @return the operation name, for example UPDATE
     */
    protected final String getTestedOperationName() {
        return getScriptClassifications().get(0).name();
    }

    /**
     * Reads data from the external system using the developed connector and shows the connector results in the drawer.
     *
     * @param operationName name of the operation
     * @param scriptPanelType the script step opened by the fix action
     * @param failureKey key of the message shown when the operation fails
     * @param target the request target
     * @param operation the operation
     * @param <T> type of the read value
     * @return the read value; null when the operation failed
     */
    protected final <T> T readFromExternalSystem(String operationName, String scriptPanelType, String failureKey,
            AjaxRequestTarget target, CheckedFunction<Task, T> operation) {
        Task task = getPageBase().createSimpleTask(operationName);
        ConnectorDevelopmentWizardUtil.configureConnectorDevelopmentTask(
                task, WebModelServiceUtils.isEnableExperimentalFeature(getPageBase()));
        return runConnectorOperation(task, scriptPanelType, failureKey, target, operation);
    }

    /**
     * Changes data in the external system using the developed connector and shows the connector results in the drawer.
     *
     * @param operationName name of the operation
     * @param scriptPanelType the script step opened by the fix action
     * @param failureKey key of the message shown when the operation fails
     * @param target the request target
     * @param operation the operation
     * @param <T> type of the returned value
     * @return the returned value; null when the operation failed
     */
    protected final <T> T changeExternalSystem(String operationName, String scriptPanelType, String failureKey,
            AjaxRequestTarget target, CheckedFunction<Task, T> operation) {
        Task task = getPageBase().createSimpleTask(operationName);
        if (WebModelServiceUtils.isEnableExperimentalFeature(getPageBase())) {
            ConnectorDevelopmentWizardUtil.enableConnectorLogCapture(task);
        }
        return runConnectorOperation(task, scriptPanelType, failureKey, target, operation);
    }

    /**
     * Creates a wrapper for a new object of the object class, to be filled in by the user.
     *
     * @return the wrapper of the new object
     */
    protected final ShadowWrapper createNewObjectWrapper() {
        Task task = getPageBase().createSimpleTask(OP_CREATE_NEW_OBJECT_WRAPPER);
        try {
            ShadowWrapper wrapper = ConnectorDevelopmentWizardUtil.createShadowWrapperForAdd(
                    getPageBase(), getTestingResourceModel(), getObjectClassName(), task, task.getResult());
            if (!WebComponentUtil.isSuccessOrHandledError(task.getResult())) {
                getPageBase().showResult(task.getResult(), false);
            }
            return wrapper;
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't prepare the new object of " + getObjectClassName(), e);
        }
    }

    private OperationFailedPanel createOperationFailedPanel() {
        return new OperationFailedPanel(ID_OPERATION_FAILED, failureModel, this::getWizard);
    }

    private <T> T runConnectorOperation(Task task, String scriptPanelType, String failureKey,
            AjaxRequestTarget target, CheckedFunction<Task, T> operation) {
        failureModel.setObject(null);
        OperationResult result = task.getResult();
        T value = null;
        try {
            value = operation.apply(task);
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't run operation {} on the testing resource", e,
                    result.getOperation());
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }

        ConnectorDevelopmentWizardUtil.reportConnectorResults(this, scriptPanelType, result, target);
        target.add(operationFailedPanel);

        if (!result.isSuccess()) {
            failureModel.setObject(getString(failureKey));
            return null;
        }
        return value;
    }

    private ResourceDetailsModel createTestingResourceModel() {
        try {
            return (ResourceDetailsModel) ConnectorDevelopmentWizardUtil.getTestingResourceModel(getDetailsModel(), getStepId());
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't get the testing resource", e);
        }
    }
}
