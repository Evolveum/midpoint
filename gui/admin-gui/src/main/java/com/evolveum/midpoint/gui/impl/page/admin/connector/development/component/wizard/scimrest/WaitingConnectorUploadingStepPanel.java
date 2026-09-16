/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.util.List;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
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
 * Waiting step for the "upload connector" background activity. Shown as a single-step part item
 * (see {@code UploadConnectorDevPartItem}). Its own activity ({@code UploadConnectorActivityHandler})
 * packages a lib/-less bundle - a sibling of, but a separate activity from, the "export connector"
 * one - and shares its result type and download mechanism (via
 * {@link ConnectorDevelopmentDetailsModel#setPendingExportResult}) with
 * {@link WaitingConnectorExportingStepPanel}, since the two produce the same shape of result. A
 * future version of this action is expected to upload the bundle to another microservice instead of
 * downloading it.
 */
@PanelType(name = "cdw-connector-waiting-uploading-connector")
@PanelInstance(identifier = "cdw-connector-waiting-uploading-connector",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingUploadingConnector", icon = "fa-solid fa-gears"),
        containerPath = "empty")
public class WaitingConnectorUploadingStepPanel extends WaitingConnectorStepPanel implements WizardParentStep {

    private static final String PANEL_TYPE = "cdw-connector-waiting-uploading-connector";

    public WaitingConnectorUploadingStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_UPLOAD_CONNECTOR;
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws CommonException {
        return getDetailsModel().getServiceLocator().getConnectorService().getUploadConnectorStatus(token, task, result);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result, boolean regenerate) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitUploadConnector(task, result);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public List<WizardStep> createChildrenSteps() {
        return List.of(this);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingUploadingConnector");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingUploadingConnector.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingUploadingConnector.subText");
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        getDetailsModel().setPendingExportResult((ConnDevExportConnectorResultType) getResult());
        return super.onNextPerformed(target);
    }

    @Override
    protected Model<String> getIconModel() {
        return Model.of("fa-solid fa-gears");
    }

    @Override
    protected boolean objectClassRequired() {
        return false;
    }
}
