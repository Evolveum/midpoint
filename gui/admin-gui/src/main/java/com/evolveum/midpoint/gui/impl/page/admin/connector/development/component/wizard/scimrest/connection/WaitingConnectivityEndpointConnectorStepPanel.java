/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevTestingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.apache.wicket.model.IModel;

/**
 * Waiting step that triggers LLM discovery of suggested connectivity test endpoints.
 * Runs after credentials are saved so the backend can use them.
 */
@PanelType(name = "cdw-connector-waiting-connectivity-endpoint")
@PanelInstance(identifier = "cdw-connector-waiting-connectivity-endpoint",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingConnectivityEndpoint", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingConnectivityEndpointConnectorStepPanel extends WaitingConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-connectivity-endpoint";

    public WaitingConnectivityEndpointConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return getDetailsModel().getServiceLocator().getConnectorService()
                .getDiscoverConnectivityEndpointStatus(token, task, result);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result, boolean regenerate) {
        return getDetailsModel().getConnectorDevelopmentOperation()
                .submitDiscoverConnectivityEndpoint(task, result);
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_DISCOVER_CONNECTIVITY_ENDPOINT;
    }

    @Override
    protected boolean objectClassRequired() {
        return false;
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingConnectivityEndpoint");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingConnectivityEndpoint.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingConnectivityEndpoint.subText");
    }

    @Override
    public boolean isCompleted() {
        return ConnectorDevelopmentWizardUtil.existContainerValue(
                getDetailsModel().getObjectWrapper(),
                ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_SUGGESTED_ENDPOINT));
    }
}
