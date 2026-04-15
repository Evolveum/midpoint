/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.create;

import java.io.IOException;

import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-create-script")
@PanelInstance(identifier = "cdw-create-script",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.createScript", icon = "fa fa-wrench"),
        containerPath = "empty")
public class CreateScriptConnectorStepPanel extends ScriptConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-create-script";

    static final String TASK_CREATE_SCRIPTS_KEY = "taskCreateScriptKey";
    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;

    public CreateScriptConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper, IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createScript");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createScript.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.createScript.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected void saveScript(ConnDevArtifactType object, Task task, OperationResult result) throws IOException, CommonException {
        getDetailsModel().getConnectorDevelopmentOperation().saveArtifact(object, task, result);
    }

    @Override
    protected ConnectorDevelopmentArtifacts.KnownArtifactType getScriptType() {
        return ConnectorDevelopmentArtifacts.KnownArtifactType.CREATE;
    }

    @Override
    protected String getObjectClassName() {
        return valueModel.getObject().getRealValue().getName();
    }
}
