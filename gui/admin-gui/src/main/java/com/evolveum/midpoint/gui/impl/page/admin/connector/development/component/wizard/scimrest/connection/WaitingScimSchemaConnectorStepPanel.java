/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * Waiting step panel that triggers SCIM schema refresh (object class discovery from shadows)
 * after resource test succeeds. Only visible for SCIM connectors.
 */
@PanelType(name = "cdw-connector-waiting-scim-schema")
@PanelInstance(identifier = "cdw-connector-waiting-scim-schema",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingScimSchema", icon = "fa fa-database"),
        containerPath = "empty")
public class WaitingScimSchemaConnectorStepPanel extends WaitingConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-scim-schema";

    public WaitingScimSchemaConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_REFRESH_SCIM_SCHEMA;
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getDetailsModel().getServiceLocator().getConnectorService().getRefreshScimSchemaStatus(token, task, result);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result, boolean regenerate) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitRefreshScimSchema(task, result);
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
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingScimSchema");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingScimSchema.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingScimSchema.subText");
    }

    @Override
    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-database");
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        if (!isScim()) {
            return Model.of(false);
        }
        return super.isStepVisible();
    }

    private boolean isScim() {
        try {
            var integrationType = getDetailsModel().getObjectWrapper().findProperty(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_INTEGRATION_TYPE));
            return integrationType != null
                    && ConnDevIntegrationType.SCIM.equals(integrationType.getValue().getRealValue());
        } catch (SchemaException e) {
            return false;
        }
    }
}
