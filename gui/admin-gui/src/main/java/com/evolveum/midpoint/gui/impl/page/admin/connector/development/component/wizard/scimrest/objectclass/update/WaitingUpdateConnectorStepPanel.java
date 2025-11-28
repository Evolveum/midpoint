/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.update;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingObjectClassScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-update")
@PanelInstance(identifier = "cdw-connector-waiting-update",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingUpdate", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingUpdateConnectorStepPanel extends WaitingObjectClassScriptConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-update";

    public WaitingUpdateConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        super(helper, objectClassModel);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result) {
        var realValue = getObjectClassModel().getObject().getRealValue();

        return getDetailsModel().getConnectorDevelopmentOperation().submitGenerateUpdateScript(
                realValue.getName(), realValue.getEndpoint(), task, result);
    }

    @Override
    protected String getKeyForStoringToken() {
        return UpdateScriptConnectorStepPanel.TASK_UPDATE_SCRIPTS_KEY;
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingUpdate");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingUpdate.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingUpdate.subText");
    }

    @Override
    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-cogs");
    }

    @Override
    protected ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType() {
        return ConnectorDevelopmentArtifacts.KnownArtifactType.UPDATE;
    }
}
