/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.schema;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.MultiWaitingConnectorStepPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

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

import java.util.List;

/**
 * Waits for two parallel tasks discovering object class details: attributes and endpoints.
 *
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-object-class-details")
@PanelInstance(identifier = "cdw-connector-waiting-object-class-details",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingObjectClassDetails", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingObjectClassDetailsConnectorStepPanel extends MultiWaitingConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-object-class-details";
    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;

    public WaitingObjectClassDetailsConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    protected List<ItemName> getActivityTypes() {
        return List.of(
                WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ATTRIBUTES,
                WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ENDPOINTS);
    }

    @Override
    protected StatusInfo<?> obtainResult(ItemName activityType, String token, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        var service = getDetailsModel().getServiceLocator().getConnectorService();
        if (WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ENDPOINTS.equals(activityType)) {
            return service.getDiscoverObjectClassEndpointsStatus(token, task, result);
        }
        return service.getDiscoverObjectClassAttributesStatus(token, task, result);
    }

    @Override
    protected String triggerOperation(Task task, OperationResult result) {
        ConnectorDevelopmentOperation op = getDetailsModel().getConnectorDevelopmentOperation();
        if (op == null) {
            return null;
        }
        String objectClass = getObjectClassName();
        op.submitDiscoverObjectClassAttributes(objectClass, task, result);
        op.submitDiscoverObjectClassEndpoints(objectClass, task, result);
        return null;
    }

    @Override
    protected String restartActivity(ItemName activityType, Task task, OperationResult result) {
        ConnectorDevelopmentOperation op = getDetailsModel().getConnectorDevelopmentOperation();
        if (op == null) {
            return null;
        }
        String objectClass = getObjectClassName();
        if (WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ENDPOINTS.equals(activityType)) {
            return op.submitDiscoverObjectClassEndpoints(objectClass, task, result);
        }
        return op.submitDiscoverObjectClassAttributes(objectClass, task, result);
    }

    @Override
    protected IModel<String> getTaskTitleModel(ItemName activityType) {
        if (WorkDefinitionsType.F_DISCOVER_OBJECT_CLASS_ENDPOINTS.equals(activityType)) {
            return createStringResource("WaitingObjectClassDetailsConnectorStepPanel.discoverObjectClassEndpoints");
        }
        return createStringResource("WaitingObjectClassDetailsConnectorStepPanel.discoverObjectClassAttributes");
    }

    @Override
    protected boolean objectClassRequired() {
        return true;
    }

    @Override
    protected String getObjectClassName() {
        return valueModel.getObject().getRealValue().getName();
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingObjectClassDetails");
    }

    @Override
    protected IModel<String> getSubtitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingObjectClassDetails.subText");
    }

    @Override
    public boolean isCompleted() {

        PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue =
                ConnectorDevelopmentWizardUtil.getObjectClassValueWrapper(getDetailsModel(), getObjectClassName());
        if (ConnectorDevelopmentWizardUtil.existContainerValue(objectClassValue, ConnDevObjectClassInfoType.F_ATTRIBUTE)) {
            return true;
        }

        return super.isCompleted();
    }
}
