/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRelationInfoType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search.SearchAllScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-relation")
@PanelInstance(identifier = "cdw-connector-waiting-relation",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingRelationScript", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingRelationScriptConnectorStepPanel extends WaitingConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-relation";
    private final IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel;

    public WaitingRelationScriptConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getDetailsModel().getServiceLocator().getConnectorService().getGenerateArtifactStatus(token, task, result);
    }

    @Override
    protected String getTaskToken(Task task, OperationResult result) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitGenerateRelationScript(
                valueModel.getObject().getRealValue(), task, result);
    }

    @Override
    protected String getKeyForStoringToken() {
        return RelationScriptConnectorStepPanel.TASK_RELATION_SCRIPTS_KEY;
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingRelationScript");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingRelationScript.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingRelationScript.subText");
    }

    @Override
    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-cogs");
    }
}
