/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevExportConnectorResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Waiting step for the "export connector" background activity. Shown as a single-step part item
 * (see {@code ExportConnectorDevPartItem}). On completion the step stashes the export result in
 * {@link ConnectorDevelopmentDetailsModel#setPendingExportResult}, so that the download can be
 * triggered by the "next steps" summary panel once it replaces this step in the same AJAX response
 * (a download behavior attached to this step would be invalidated by that replacement).
 */
@PanelType(name = "cdw-connector-waiting-exporting-connector")
@PanelInstance(identifier = "cdw-connector-waiting-exporting-connector",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingExportingConnector", icon = "fa fa-download"),
        containerPath = "empty")
public class WaitingConnectorExportingStepPanel extends WaitingConnectorStepPanel implements WizardParentStep {

    private static final String PANEL_TYPE = "cdw-connector-waiting-exporting-connector";

    public WaitingConnectorExportingStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_EXPORT_CONNECTOR;
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getDetailsModel().getServiceLocator().getConnectorService().getExportConnectorStatus(token, task, result);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result, boolean regenerate) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitExportConnector(task, result);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingExportingConnector");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingExportingConnector.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingExportingConnector.subText");
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        getDetailsModel().setPendingExportResult((ConnDevExportConnectorResultType) getResult());
        return super.onNextPerformed(target);
    }

    @Override
    protected Model<String> getIconModel() {
        return Model.of("fa fa-download");
    }

    @Override
    protected boolean objectClassRequired() {
        return false;
    }
}
