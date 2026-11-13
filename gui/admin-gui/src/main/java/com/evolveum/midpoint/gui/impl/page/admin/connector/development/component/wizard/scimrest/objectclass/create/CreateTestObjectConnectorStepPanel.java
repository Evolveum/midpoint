/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.AbstractTestObjectOperationConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.TestObjectFlowState;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Wizard step for creating an object in the external system when testing the Create capability.
 */
@PanelType(name = "cdw-create-test-object")
@PanelInstance(identifier = "cdw-create-test-object",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.createTestObject", icon = "fa fa-wrench"),
        containerPath = "empty")
public class CreateTestObjectConnectorStepPanel extends AbstractTestObjectOperationConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-create-test-object";

    /**
     * Wrapper of the new object edited in the table, built lazily.
     */
    private final LoadableModel<ShadowWrapper> shadowWrapperModel = LoadableModel.create(this::createNewObjectWrapper, false);

    public CreateTestObjectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel, TestObjectFlowState flowState) {
        super(helper, valueModel, flowState);
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE);
    }

    @Override
    protected IModel<ShadowWrapper> getTestObjectWrapperModel() {
        return shadowWrapperModel;
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

    @Override
    protected String executeOperation(ObjectDelta<ShadowType> delta, Task task, OperationResult result)
            throws CommonException {
        ShadowType object = delta.getObjectToAdd().asObjectable();
        String oid = getDetailsModel().getConnectorDevelopmentOperation().addTestObject(object, task, result);
        getFlowState().getTestedObjectModel().setObject(shadowWrapperModel.getObject());
        getDetailsModel().reloadPrismObjectByOid();
        return oid;
    }

    @Override
    protected String getScriptPanelType() {
        return CreateScriptConnectorStepPanel.PANEL_TYPE;
    }

    @Override
    protected String getObjectNotRetrievedKey() {
        return "CreateTestObjectConnectorStepPanel.objectNotRetrieved";
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
}
