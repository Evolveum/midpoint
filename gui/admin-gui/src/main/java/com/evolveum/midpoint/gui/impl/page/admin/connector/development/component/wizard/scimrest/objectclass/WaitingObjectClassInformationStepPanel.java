/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingConnectorStepPanel;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

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

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-object-class")
@PanelInstance(identifier = "cdw-connector-waiting-object-class",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingObjectClass", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingObjectClassInformationStepPanel extends WaitingConnectorStepPanel implements WizardParentStep {

    private static final String PANEL_TYPE = "cdw-connector-waiting-object-class";

    public WaitingObjectClassInformationStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return getDetailsModel().getServiceLocator().getConnectorService().getDiscoverObjectClassInformationStatus(token, task, result);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitDiscoverObjectClasses(task, result);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingObjectClass");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingObjectClass.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingObjectClass.subText");
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_INFORMATION;
    }

    @Override
    public boolean isCompleted() {
        if (ConnectorDevelopmentWizardUtil.existPropertyValue(
                getDetailsModel().getObjectWrapper(),
                ItemPath.create(ConnectorDevelopmentType.F_APPLICATION,
                        ConnDevApplicationInfoType.F_DETECTED_SCHEMA,
                        ConnDevSchemaType.F_OBJECT_CLASS))) {
            return true;
        }

        return super.isCompleted();
    }
}
