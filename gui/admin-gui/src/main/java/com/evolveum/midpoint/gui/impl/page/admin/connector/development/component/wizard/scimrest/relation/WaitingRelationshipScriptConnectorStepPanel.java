/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.Strings;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import java.util.Optional;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-relationship")
@PanelInstance(identifier = "cdw-connector-waiting-relationship",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingRelationshipScript", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingRelationshipScriptConnectorStepPanel extends WaitingScriptConnectorStepPanel {

    private static final Trace LOGGER = TraceManager.getTrace(WaitingRelationshipScriptConnectorStepPanel.class);

    private static final String PANEL_TYPE = "cdw-connector-waiting-relation";
    private final IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel;

    public WaitingRelationshipScriptConnectorStepPanel(
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
    protected String getNewTaskToken(Task task, OperationResult result) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitGenerateRelationScript(
                valueModel.getObject().getRealValue(), task, result);
    }

    @Override
    protected String getKeyForStoringToken() {
        return RelationshipScriptConnectorStepPanel.TASK_RELATIONSHIP_SCRIPTS_KEY;
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingRelationshipScript");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingRelationshipScript.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingRelationshipScript.subText");
    }

    @Override
    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-cogs");
    }

    @Override
    protected ConnectorDevelopmentArtifacts.KnownArtifactType getScripType() {
        return ConnectorDevelopmentArtifacts.KnownArtifactType.RELATIONSHIP_SCHEMA_DEFINITION;
    }

    @Override
    public boolean isCompleted() {
        try {
            PrismContainerWrapper<ConnDevRelationInfoType> relationshipWrapper = getDetailsModel().getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_RELATION));
            if (relationshipWrapper != null && !relationshipWrapper.getValues().isEmpty()) {
                Optional<PrismContainerValueWrapper<ConnDevRelationInfoType>> relationshipValue = relationshipWrapper.getValues().stream()
                        .filter(value ->
                                Strings.CS.equals(value.getRealValue().getName(), valueModel.getObject().getRealValue().getName()))
                        .findFirst();

                if (relationshipValue.isPresent()) {
                    return true;
                }

            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get relationship value.", e);
            String message = getDetailsModel().getPageAssignmentHolder().getString("WaitingRelationshipScriptConnectorStepPanel.couldntGetRelationshipValue");
            getDetailsModel().getPageAssignmentHolder().error(message);
        }

        return super.isCompleted();
    }
}
