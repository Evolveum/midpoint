/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-creating-connector")
@PanelInstance(identifier = "cdw-connector-waiting-creating-connector",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingConnectorCreatingConnectorStepPanel extends WaitingConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-creating-connector";

    public WaitingConnectorCreatingConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getDetailsModel().getServiceLocator().getConnectorService().getCreateConnectorStatus(token, task, result);
    }

    @Override
    protected String getTaskToken(Task task, OperationResult result) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitCreateConnector(task, result);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector.subText");
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        try {
            PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));
            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    resource.getValue().getNewObjectModel(getContainerConfiguration(PANEL_TYPE), getPageBase(), new OperationResult("getResourceModel"));
            ResourceDetailsModel resourceDetailsModel = (ResourceDetailsModel) objectDetailsModel;

            PrismReferenceWrapper<Referencable> connectorRef = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_CONNECTOR_REF));

            resourceDetailsModel.getObjectWrapper().findProperty(ResourceType.F_NAME).getValue().setRealValue(PolyString.fromOrig(
                    "Resource - " + getDetailsModel().getObjectWrapper().getObject().getName().getOrig()));

            resourceDetailsModel.getObjectWrapper().findReference(ResourceType.F_CONNECTOR_REF).getValue().setRealValue(
                    connectorRef.getValue().getRealValue().clone());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }
}
