/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.io.IOException;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-relationship-script")
@PanelInstance(identifier = "cdw-relationship-script",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.relationshipScript", icon = "fa fa-wrench"),
        containerPath = "empty")
public class RelationshipScriptConnectorStepPanel extends ScriptConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-relationship-script";

    static final String TASK_RELATIONSHIP_SCRIPTS_KEY = "taskRelationshipScriptKey";
    private final IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel;

    public RelationshipScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationshipScript");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationshipScript.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationshipScript.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException {
        getDetailsModel().getConnectorDevelopmentOperation().saveRelationScript(object, task, result);
    }

    @Override
    protected ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType() {
        return ConnectorDevelopmentArtifacts.KnownArtifactType.RELATIONSHIP_SCHEMA_DEFINITION;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        try {
            PrismPropertyWrapper<Boolean> confirmProperty = valueModel.getObject().findProperty(ConnDevRelationInfoType.F_CONFIRM);
            PrismPropertyValueWrapper<Boolean> confirmValue = confirmProperty.getValue();
            confirmValue.setRealValue(Boolean.TRUE);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return super.onNextPerformed(target);
    }

    @Override
    public boolean isCompleted() {
        try {
            PrismPropertyWrapper<Boolean> confirmProperty = valueModel.getObject().findProperty(ConnDevRelationInfoType.F_CONFIRM);
            PrismPropertyValueWrapper<Boolean> confirmValue = confirmProperty.getValue();
            return Boolean.TRUE.equals(confirmValue.getRealValue());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }
}
