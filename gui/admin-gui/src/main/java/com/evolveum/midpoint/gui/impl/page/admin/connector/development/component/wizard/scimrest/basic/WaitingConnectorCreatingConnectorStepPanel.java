/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.List;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.WaitingConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-creating-connector")
@PanelInstance(identifier = "cdw-connector-waiting-creating-connector",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingConnectorCreatingConnectorStepPanel extends WaitingConnectorStepPanel {

    private static final String PANEL_TYPE = "cdw-connector-waiting-creating-connector";

    public WaitingConnectorCreatingConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected ItemName getActivityType() {
        return WorkDefinitionsType.F_CREATE_CONNECTOR;
    }

    @Override
    protected StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws CommonException {
        return getDetailsModel().getServiceLocator().getConnectorService().getCreateConnectorStatus(token, task, result);
    }

    @Override
    protected String getNewTaskToken(Task task, OperationResult result, boolean regenerate) {
        return getDetailsModel().getConnectorDevelopmentOperation().submitCreateConnector(task, result);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorWaitingCreatingConnector.subText");
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        PrismReferenceWrapper<Referencable> resource;
        ResourceDetailsModel resourceDetailsModel;
        try {
            resource = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));
            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    resource.getValue().getNewObjectModel(getContainerConfiguration(PANEL_TYPE), getPageBase(), new OperationResult("getResourceModel"));
            resourceDetailsModel = (ResourceDetailsModel) objectDetailsModel;

            ConnDevCreateConnectorResultType connectorRefResult = (ConnDevCreateConnectorResultType) getResult();
            resourceDetailsModel.getObjectWrapper().findProperty(ResourceType.F_NAME).getValue().setRealValue(PolyString.fromOrig(
                    "Resource - " + getDetailsModel().getObjectWrapper().getObject().getName().getOrig()));

            resourceDetailsModel.getObjectWrapper().findReference(ResourceType.F_CONNECTOR_REF).getValue().setRealValue(
                    connectorRefResult.getConnectorRef().clone());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            linkTestingResourceIfMissing(resource, resourceDetailsModel);
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }

    /**
     * The testing resource is created as a precondition delta of the {@code testing.testingResource}
     * reference (see {@code PrismReferenceValueWrapperImpl.getPreconditionDeltas()}) - but that
     * reference's own new oid is only set on the in-memory wrapper *after* the parent
     * (connector-development) object's own save delta has already been computed, so the persisted
     * connector-development object never actually records which resource it points at, and every
     * later step that resolves {@code testing.testingResource} builds yet another fresh, empty,
     * never-saved resource instead of loading the real one. Persist the real oid as an explicit
     * follow-up delta once it's known.
     */
    private void linkTestingResourceIfMissing(PrismReferenceWrapper<Referencable> resource, ResourceDetailsModel resourceDetailsModel) {
        String createdResourceOid = resourceDetailsModel.getObjectWrapper().getObject().getOid();
        if (StringUtils.isEmpty(createdResourceOid)) {
            return;
        }
        try {
            Referencable currentValue = resource.getValue().getRealValue();
            if (currentValue != null && createdResourceOid.equals(currentValue.getOid())) {
                return;
            }
            ObjectDelta<ConnectorDevelopmentType> delta = PrismContext.get().deltaFor(ConnectorDevelopmentType.class)
                    .item(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE)
                    .replace(new ObjectReferenceType().oid(createdResourceOid).type(ResourceType.COMPLEX_TYPE))
                    .asObjectDelta(getDetailsModel().getObjectWrapper().getOid());
            getPageBase().getModelService().executeChanges(List.of(delta), null,
                    getPageBase().createSimpleTask("linkTestingResource"), new OperationResult("linkTestingResource"));
        } catch (CommonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-cogs");
    }

    @Override
    public boolean isCompleted() {
        if (ConnectorDevelopmentWizardUtil.isBasicSettingsComplete(getDetailsModel().getObjectWrapper())) {
            return true;
        }

        return super.isCompleted();
    }

    @Override
    protected boolean objectClassRequired() {
        return false;
    }
}
