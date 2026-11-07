/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConfirmationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectAttributesPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for creating a test object with the CREATE script.
 */
@PanelType(name = "cdw-create-test-object")
@PanelInstance(identifier = "cdw-create-test-object",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.createTestObject", icon = "fa fa-wrench"),
        containerPath = "empty")
public class CreateTestObjectConnectorStepPanel extends ScriptConfirmationPanel {

    private static final Trace LOGGER = TraceManager.getTrace(CreateTestObjectConnectorStepPanel.class);

    private static final String CLASS_DOT = CreateTestObjectConnectorStepPanel.class.getName() + ".";
    private static final String OP_CREATE_OBJECT = CLASS_DOT + "createObject";
    private static final String OP_CREATE_WRAPPER = CLASS_DOT + "createWrapper";
    private static final String OP_STORE_TEST_OBJECT = CLASS_DOT + "storeTestObject";

    private static final String PANEL_TYPE = "cdw-create-test-object";

    private static final String ID_ATTRIBUTES = "attributes";

    private final IModel<ShadowType> createdObjectModel;
    private final IModel<ShadowType> retrievedObjectModel;

    private final LoadableModel<ShadowWrapper> shadowWrapperModel = new LoadableModel<>(false) {

        @Override
        protected ShadowWrapper load() {
            return createShadowWrapper();
        }
    };

    public CreateTestObjectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel,
            IModel<ShadowType> createdObjectModel, IModel<ShadowType> retrievedObjectModel) {
        super(helper, valueModel);
        this.createdObjectModel = createdObjectModel;
        this.retrievedObjectModel = retrievedObjectModel;
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        add(createAttributesPanel());
    }

    private TestObjectAttributesPanel createAttributesPanel() {
        return new TestObjectAttributesPanel(ID_ATTRIBUTES, shadowWrapperModel);
    }

    private ShadowWrapper createShadowWrapper() {
        Task task = getPageBase().createSimpleTask(OP_CREATE_WRAPPER);
        try {
            ResourceDetailsModel resourceModel = (ResourceDetailsModel)
                    ConnectorDevelopmentWizardUtil.getTestingResourceModel(getDetailsModel(), PANEL_TYPE);
            ResourceObjectDefinition objectClassDefinition =
                    resourceModel.findResourceObjectClassDefinition(new QName(getObjectClassName()));
            if (objectClassDefinition == null) {
                throw new IllegalStateException("No definition of object class " + getObjectClassName()
                        + " in the testing resource schema");
            }
            PrismObject<ShadowType> shadow = new ShadowType()
                    .resourceRef(resourceModel.getObjectWrapper().getOid(), ResourceType.COMPLEX_TYPE)
                    .objectClass(objectClassDefinition.getObjectClassName())
                    .asPrismObject();
            PrismObjectWrapperFactory<ShadowType> factory = getPageBase().findObjectWrapperFactory(shadow.getDefinition());
            ShadowWrapper wrapper = (ShadowWrapper) factory.createObjectWrapper(
                    shadow, ItemStatus.ADDED, new WrapperContext(task, task.getResult()));
            if (!WebComponentUtil.isSuccessOrHandledError(task.getResult())) {
                getPageBase().showResult(task.getResult(), false);
            }
            return wrapper;
        } catch (CommonException e) {
            throw new IllegalStateException("Couldn't prepare the new object of " + getObjectClassName(), e);
        }
    }

    private String getObjectClassName() {
        return getValueModel().getObject().getRealValue().getName();
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        customButtons.add(createSkipCreationButton(customButtons.newChildId()));
    }

    private AjaxButton createSkipCreationButton(String id) {
        return new AjaxButton(id, createStringResource("CreateTestObjectConnectorStepPanel.skipCreation")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
    }

    private void createObjectPerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_CREATE_OBJECT);
        if (WebModelServiceUtils.isEnableExperimentalFeature(getPageBase())) {
            ConnectorDevelopmentWizardUtil.enableConnectorLogCapture(task);
        }
        OperationResult result = task.getResult();

        ShadowType createdObject = null;
        ShadowType retrievedObject = null;
        try {
            ObjectDelta<ShadowType> delta = shadowWrapperModel.getObject().getObjectDelta();
            createdObject = delta.getObjectToAdd().asObjectable();

            String oid = ObjectDeltaOperation.findAddDeltaOidRequired(
                    getPageBase().getModelService().executeChanges(List.of(delta), null, task, result), ShadowType.class);
            ConnectorDevelopmentWizardUtil.addTestObject(getDetailsModel(), oid, task, result.createSubresult(OP_STORE_TEST_OBJECT));

            PrismObject<ShadowType> retrieved = WebModelServiceUtils.loadObject(
                    ShadowType.class, oid, getPageBase(), task, result);
            retrievedObject = retrieved != null ? retrieved.asObjectable() : null;
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create object for CREATE operation test", e);
            result.recordFatalError(e);
        } finally {
            result.computeStatusIfUnknown();
        }

        ConnectorDevelopmentWizardUtil.reportConnectorResults(
                this, CreateScriptConnectorStepPanel.PANEL_TYPE, result, target);

        if (!result.isSuccess()) {
            getPageBase().showResult(result);
            target.add(getFeedback());
            return;
        }
        if (retrievedObject == null) {
            getPageBase().error(getString("CreateTestObjectConnectorStepPanel.objectNotRetrieved"));
            target.add(getFeedback());
            return;
        }

        createdObjectModel.setObject(createdObject);
        retrievedObjectModel.setObject(retrievedObject);
        nextWithoutConfirmationPerformed(target);
    }

    @Override
    public boolean isCompleted() {
        return retrievedObjectModel.getObject() != null || super.isCompleted();
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
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
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createTestObject");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createTestObject.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createTestObject.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("CreateTestObjectConnectorStepPanel.executeScript");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        createObjectPerformed(target);
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
