/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import java.io.IOException;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-relation-script")
@PanelInstance(identifier = "cdw-relation-script",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.relationScript", icon = "fa fa-wrench"),
        containerPath = "empty")
public class RelationScriptConnectorStepPanel extends ScriptConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-relation-script";

    static final String TASK_RELATION_SCRIPTS_KEY = "taskRelationScriptKey";
    private final IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel;

    public RelationScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationScript");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationScript.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationScript.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected String getTokenForTaskForObtainResult() {
        return TASK_RELATION_SCRIPTS_KEY;
    }

    @Override
    protected void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException {
        getDetailsModel().getConnectorDevelopmentOperation().saveRelationScript(object, task, result);
    }

    @Override
    protected ConnDevArtifactType getOriginalContainerValue() {
        try {
            //TODO missing script for relation
            PrismContainerWrapper<ConnDevArtifactType> container = getDetailsModel().getObjectWrapper().findContainer(ConnDevRelationInfoType.F_SUBJECT);
            if (container != null) {
                return container.getValue().getRealValue();
            }
        } catch (SchemaException e) {
            //todo
            return null;
        }
        return null;
    }
}
