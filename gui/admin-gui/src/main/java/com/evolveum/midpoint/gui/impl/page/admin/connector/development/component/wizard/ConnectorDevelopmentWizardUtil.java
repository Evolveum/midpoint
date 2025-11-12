/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConnectorDevelopmentWizardUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentWizardUtil.class);

    public static PrismObject<TaskType> getTask(
            ItemName activityType,
            String objectClassName,
            ConnDevScriptIntentType scriptIntent,
            String connectorDevelopmentOid,
            PageAdminLTE page) throws CommonException {
        ObjectQuery query = PrismContext.get()
                .queryFor(TaskType.class)
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_ACTIVITY_TYPE))
                .eq(activityType)
                .and()
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_OBJECTS,
                        BasicObjectSetType.F_OBJECT_REF
                ))
                .ref(connectorDevelopmentOid)
                .build();

        @NotNull ObjectPaging paging = PrismContext.get().queryFactory().createPaging(
                ItemPath.create(TaskType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP),
                OrderDirection.DESCENDING);
        query.setPaging(paging);
        Task operationTask = page.createSimpleTask("load_task");

        SearchResultList<PrismObject<TaskType>> tasks;
        tasks = page.getModelService()
                .searchObjects(TaskType.class, query, null, operationTask, operationTask.getResult());

        if (objectClassName != null) {
            tasks.removeIf(task ->
                    task.asObjectable().getActivity() == null
                            || task.asObjectable().getActivity().getWork() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact().getArtifact() == null
                            || !Strings.CS.equals(
                            task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact().getArtifact().getObjectClass(),
                            objectClassName)
            );
        }

        if (scriptIntent != null) {
            tasks.removeIf(task ->
                    task.asObjectable().getActivity() == null
                            || task.asObjectable().getActivity().getWork() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact().getArtifact() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact().getArtifact().getIntent() != scriptIntent
            );
        }

        if (tasks.isEmpty()) {
            return null;
        }

        return tasks.get(0);
    }

    public static String getTaskToken(
            ItemName activityType,
            String connectorDevelopmentOid,
            PageAdminLTE page) throws CommonException {
        return getTaskToken(activityType, null, null, connectorDevelopmentOid, page);
    }

    public static String getTaskToken(
            ItemName activityType,
            String objectClassName,
            ConnDevScriptIntentType scriptIntent,
            String connectorDevelopmentOid,
            PageAdminLTE page) throws CommonException {
        PrismObject<TaskType> taskBean = getTask(activityType, objectClassName, scriptIntent, connectorDevelopmentOid, page);

        if (taskBean == null) {
            return null;
        }

        return taskBean.getOid();
    }

    public static <C extends PrismContainerWrapper<?>> boolean existContainerValue(C container, ItemPath path) {
        if (container == null) {
            return false;
        }

        try {
            PrismContainerWrapper<?> parentWrapper = container.findContainer(path);
            if (parentWrapper == null || parentWrapper.getValues().isEmpty()) {
                return false;
            }
            PrismContainerValue<?> cloneValue = parentWrapper.getValues().get(0).getNewValue().clone();
            WebPrismUtil.cleanupEmptyContainerValue(cloneValue);
            return !cloneValue.isEmpty();

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static <C extends PrismContainerValueWrapper<?>> boolean existContainerValue(C containerValue, ItemPath path) {
        if (containerValue == null) {
            return false;
        }

        try {
            PrismContainerWrapper<?> parentWrapper = containerValue.findContainer(path);
            if (parentWrapper == null || parentWrapper.getValues().isEmpty()) {
                return false;
            }
            PrismContainerValue<?> cloneValue = parentWrapper.getValues().get(0).getNewValue().clone();
            WebPrismUtil.cleanupEmptyContainerValue(cloneValue);
            return !cloneValue.isEmpty();

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static <C extends PrismContainerWrapper<?>> boolean existPropertyValue(C container, ItemPath path) {
        if (container == null) {
            return false;
        }

        try {
            PrismPropertyWrapper<?> propertyWrapper = container.findProperty(path);
            if (propertyWrapper == null || propertyWrapper.getValues().isEmpty()) {
                return false;
            }
            return propertyWrapper.getValues().get(0).getNewValue() != null
                    && propertyWrapper.getValues().get(0).getNewValue().getRealValue() != null;

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static <C extends PrismContainerWrapper<?>> boolean existTestingResourcePropertyValue(
            ConnectorDevelopmentDetailsModel detailsModel, String panelType, ItemName propertyName) {
        try {
            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    ConnectorDevelopmentWizardUtil.getTestingResourceModel(detailsModel, panelType);

            return ConnectorDevelopmentWizardUtil.existPropertyValue(
                    objectDetailsModel.getObjectWrapper(),
                    ItemPath.create(
                            "connectorConfiguration",
                            SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME,
                            ItemName.from("", propertyName.getLocalPart())));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectDetailsModels<ResourceType> getTestingResourceModel(
            ConnectorDevelopmentDetailsModel detailsModel, String panelType) throws SchemaException {
        PrismReferenceWrapper<Referencable> resource = detailsModel.getObjectWrapper().findReference(
                ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));

        ContainerPanelConfigurationType config = WebComponentUtil.getContainerConfiguration(
                detailsModel.getObjectDetailsPageConfiguration().getObject(), panelType);

        return resource.getValue().getNewObjectModel(
                config, detailsModel.getPageAssignmentHolder(), new OperationResult("getResourceModel"));
    }

    public static boolean existScript(ConnectorDevelopmentDetailsModel detailsModel, ConnDevArtifactType artifact) {
        try {
            var classification = ConnectorDevelopmentArtifacts.classify(artifact);
            if (classification == null) {
                return false;
            }

            if (artifact.getObjectClass() != null) {
                PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassWrapper = detailsModel.getObjectWrapper().findContainer(
                        ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_OBJECT_CLASS));
                if (objectClassWrapper == null || objectClassWrapper.getValues().isEmpty()) {
                    return false;
                }

                PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = objectClassWrapper.getValues().stream()
                        .filter(value -> value.getRealValue().getName().equals(artifact.getObjectClass()))
                        .findFirst()
                        .orElse(null);
                if (objectClassValue == null) {
                    return false;
                }

                return existContainerValue(objectClassValue, ItemPath.create(classification.itemName));
            } else {
                return existContainerValue(detailsModel.getObjectWrapper(), ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, classification.itemName));
            }
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isScriptConfirmed(
            ConnectorDevelopmentDetailsModel detailsModel,
            ConnectorDevelopmentArtifacts.KnownArtifactType classification,
            @Nullable String objectClass) {

        try {
            PrismPropertyWrapper<Boolean> confirmProperty;

            if (objectClass != null) {
                PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassWrapper = detailsModel.getObjectWrapper().findContainer(
                        ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_OBJECT_CLASS));
                if (objectClassWrapper == null || objectClassWrapper.getValues().isEmpty()) {
                    return false;
                }

                PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = objectClassWrapper.getValues().stream()
                        .filter(value -> value.getRealValue().getName().equals(objectClass))
                        .findFirst()
                        .orElse(null);
                if (objectClassValue == null) {
                    return false;
                }

                confirmProperty = objectClassValue.findProperty(ItemPath.create(classification.itemName, ConnDevArtifactType.F_CONFIRM));
            } else {
                confirmProperty = detailsModel.getObjectWrapper().findProperty(ItemPath.create(
                        ConnectorDevelopmentType.F_CONNECTOR, classification.itemName, ConnDevArtifactType.F_CONFIRM));
            }
            return confirmProperty != null && confirmProperty.getValue() != null && Boolean.TRUE.equals(confirmProperty.getValue().getRealValue());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getNameOfNewObjectClass(ConnectorDevelopmentDetailsModel detailsModel) {
        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassContainer = detailsModel.getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_OBJECT_CLASS));
            if (objectClassContainer == null || objectClassContainer.getValues().isEmpty()) {
                return null;
            }

            PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = objectClassContainer.getValues().stream()
                    .filter(value -> value.getStatus() == ValueStatus.ADDED)
                    .findFirst()
                    .orElse(null);

            if (objectClassValue != null) {
                return objectClassValue.getRealValue().getName();
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get object class name.", e);
            String message = detailsModel.getPageAssignmentHolder().getString("ConnectorDevelopmentWizardUtil.couldntGetObjectClassName");
            detailsModel.getPageAssignmentHolder().error(message);
        }
        return null;
    }

}
