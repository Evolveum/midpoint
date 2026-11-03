/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Base step for filling in a test object, running the tested operation with it and reading back the resulting object.
 */
public abstract class AbstractTestObjectOperationConnectorStepPanel extends AbstractCapabilityTestStepPanel {

    private static final String OP_EXECUTE_OPERATION = AbstractTestObjectOperationConnectorStepPanel.class.getName() + ".executeOperation";

    private static final String ID_ATTRIBUTES = "attributes";

    /**
     * Table of the test object attributes, refreshed when the attributes change.
     */
    private TestObjectAttributesPanel attributesPanel;

    /**
     * Creates the step for testing an operation with a test object.
     *
     * @param helper wizard panel helper
     * @param valueModel object class being developed
     * @param flowState state of the test object shared by the steps of the capability test
     */
    public AbstractTestObjectOperationConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    public boolean isCompleted() {
        return getFlowState().getRetrievedObjectModel().getObject() != null || super.isCompleted();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        attributesPanel = createAttributesPanel();
        add(attributesPanel);
    }

    /**
     * Returns the table of test object attributes.
     *
     * @return the attributes panel
     */
    protected final TestObjectAttributesPanel getAttributesPanel() {
        return attributesPanel;
    }

    @Override
    protected String getSubmitIcon() {
        return "fa fa-cogs";
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("AbstractTestObjectOperationConnectorStepPanel.executeScript");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        executeOperationPerformed(target);
    }

    /**
     * Returns the wrapper of the test object used for the operation.
     *
     * @return the test object wrapper model
     */
    protected abstract IModel<ShadowWrapper> getTestObjectWrapperModel();

    /**
     * Runs the tested operation with the given changes of the test object.
     *
     * @param delta changes of the test object
     * @param task the task
     * @param result the result
     * @return OID of the test object to retrieve afterwards
     */
    protected abstract String executeOperation(ObjectDelta<ShadowType> delta, Task task, OperationResult result)
            throws CommonException;

    /**
     * Returns the type of the script step opened by the fix action.
     *
     * @return the panel type of the script step
     */
    protected abstract String getScriptPanelType();

    /**
     * Returns the key of the message shown when the object cannot be read back after the operation.
     *
     * @return the localization key
     */
    protected abstract String getObjectNotRetrievedKey();

    private TestObjectAttributesPanel createAttributesPanel() {
        TestObjectAttributesPanel panel = new TestObjectAttributesPanel(ID_ATTRIBUTES, getTestObjectWrapperModel()) {

            @Override
            protected void onAttributeChanged(AjaxRequestTarget target) {
                target.add(getCustomButtonsContainer());
            }
        };
        panel.setOutputMarkupId(true);
        return panel;
    }

    private void executeOperationPerformed(AjaxRequestTarget target) {
        getFlowState().getRetrievedObjectModel().setObject(null);
        ShadowType retrievedObject = changeExternalSystem(OP_EXECUTE_OPERATION, getScriptPanelType(),
                "AbstractTestObjectOperationConnectorStepPanel.executionFailedText", target, this::executeAndRetrieve);
        if (isOperationFailed()) {
            return;
        }
        if (retrievedObject == null) {
            getPageBase().error(getString(getObjectNotRetrievedKey()));
            target.add(getFeedback());
            return;
        }

        getFlowState().getRetrievedObjectModel().setObject(retrievedObject);
        nextWithoutConfirmationPerformed(target);
    }

    private ShadowType executeAndRetrieve(Task task) throws CommonException {
        String oid = executeOperation(getTestObjectWrapperModel().getObject().getObjectDelta(), task, task.getResult());
        PrismObject<ShadowType> retrieved = WebModelServiceUtils.loadObject(
                ShadowType.class, oid, getPageBase(), task, task.getResult());
        return retrieved != null ? retrieved.asObjectable() : null;
    }
}
