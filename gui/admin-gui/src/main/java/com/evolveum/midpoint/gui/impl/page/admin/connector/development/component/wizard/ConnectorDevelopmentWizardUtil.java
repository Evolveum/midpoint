/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ConnectorDevelopmentWizardUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorDevelopmentWizardUtil.class);

    public static PrismObject<TaskType> getTask(
            ItemName activityType,
            String objectClassName,
            ConnectorDevelopmentArtifacts.KnownArtifactType scriptType,
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

        if (scriptType != null) {
            tasks.removeIf(task ->
                    task.asObjectable().getActivity() == null
                            || task.asObjectable().getActivity().getWork() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact().getArtifact() == null
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact().getArtifact().getOperation() != scriptType.operation
                            || task.asObjectable().getActivity().getWork().getGenerateConnectorArtifact().getArtifact().getIntent() != scriptType.scriptIntent
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

    public static boolean existTask(
            ItemName activityType,
            String objectClassName,
            ConnectorDevelopmentArtifacts.KnownArtifactType knownArtifactType,
            String connectorDevelopmentOid,
            PageAdminLTE page) {
        try {
            return getTask(activityType, objectClassName, knownArtifactType, connectorDevelopmentOid, page) != null;
        } catch (CommonException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTaskToken(
            ItemName activityType,
            String objectClassName,
            ConnectorDevelopmentArtifacts.KnownArtifactType scriptType,
            String connectorDevelopmentOid,
            PageAdminLTE page) throws CommonException {
        PrismObject<TaskType> taskBean = getTask(activityType, objectClassName, scriptType, connectorDevelopmentOid, page);

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

    private static <C extends PrismContainerWrapper<?>> Object getPropertyValue(C container, ItemPath path) {
        if (container == null) {
            return false;
        }

        try {
            PrismPropertyWrapper<?> propertyWrapper = container.findProperty(path);
            if (propertyWrapper == null || propertyWrapper.getValues().isEmpty()) {
                return false;
            }
            if (propertyWrapper.getValues().get(0).getNewValue() != null) {
                return propertyWrapper.getValues().get(0).getNewValue().getRealValue();
            }

            return null;

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public static <C extends PrismContainerWrapper<?>> boolean existReferenceValue(C container, ItemPath path) {
        if (container == null) {
            return false;
        }

        try {
            PrismReferenceWrapper<Referencable> referenceWrapper = container.findReference(path);
            if (referenceWrapper == null || referenceWrapper.getValues().isEmpty()) {
                return false;
            }
            return referenceWrapper.getValues().get(0).getNewValue() != null
                    && referenceWrapper.getValues().get(0).getNewValue().getRealValue() != null
                    && StringUtils.isNotEmpty(referenceWrapper.getValues().get(0).getNewValue().getRealValue().getOid());

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

    public static <C extends PrismContainerWrapper<?>> Object getTestingResourcePropertyValue(
            ConnectorDevelopmentDetailsModel detailsModel, String panelType, ItemName propertyName) {
        try {
            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    ConnectorDevelopmentWizardUtil.getTestingResourceModel(detailsModel, panelType);

            return ConnectorDevelopmentWizardUtil.getPropertyValue(
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
        if (artifact == null) {
            return false;
        }

        ConnectorDevelopmentArtifacts.KnownArtifactType classification = ConnectorDevelopmentArtifacts.classify(artifact);
        if (classification == null) {
            return false;
        }

        return existScript(detailsModel, classification, artifact.getObjectClass());
    }

    public static boolean existScript(
            ConnectorDevelopmentDetailsModel detailsModel,
            ConnectorDevelopmentArtifacts.KnownArtifactType classification,
            String objectClass) {
        if (classification == null) {
            return false;
        }

        if (objectClass != null) {
            PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = getObjectClassValueWrapper(detailsModel, objectClass);
            if (objectClassValue == null) {
                return false;
            }
            return existContainerValue(objectClassValue, classification.itemName);
        } else {
            return existContainerValue(detailsModel.getObjectWrapper(), ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, classification.itemName));
        }
    }

    public static PrismContainerValueWrapper<ConnDevArtifactType> getScript(
            ConnectorDevelopmentDetailsModel detailsModel,
            ConnectorDevelopmentArtifacts.KnownArtifactType classification,
            String objectClass) {
        try {
            if (classification == null) {
                return null;
            }

            if (objectClass != null) {

                PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = getObjectClassValueWrapper(detailsModel, objectClass);
                if (objectClassValue == null) {
                    return null;
                }

                PrismContainerWrapper<ConnDevArtifactType> parentWrapper = objectClassValue.findContainer(classification.itemName);
                if (parentWrapper == null || parentWrapper.getValues().isEmpty()) {
                    return null;
                }
                return parentWrapper.getValue();

            } else {
                PrismContainerWrapper<ConnDevArtifactType> parentWrapper = detailsModel.getObjectWrapper().findContainer(
                        ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, classification.itemName));
                if (parentWrapper == null || parentWrapper.getValues().isEmpty()) {
                    return null;
                }
                return parentWrapper.getValue();
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

    public static PrismContainerValueWrapper<ConnDevObjectClassInfoType> getObjectClassValueWrapper(ConnectorDevelopmentDetailsModel detailsModel, String objectClassName) {
        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassesWrapper = detailsModel.getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_OBJECT_CLASS));
            if (objectClassesWrapper != null && !objectClassesWrapper.getValues().isEmpty()) {
                Optional<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassValue = objectClassesWrapper.getValues().stream()
                        .filter(value ->
                                Strings.CS.equals(value.getRealValue().getName(), objectClassName))
                        .findFirst();

                return objectClassValue.orElse(null);
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get object class value.", e);
            String message = detailsModel.getPageAssignmentHolder().getString("ConnectorDevelopmentWizardUtil.couldntGetObjectClassValue");
            detailsModel.getPageAssignmentHolder().error(message);
        }

        return null;
    }

    public static boolean isBasicSettingsComplete(PrismObjectWrapper<ConnectorDevelopmentType> objectWrapper) {
        return ConnectorDevelopmentWizardUtil.existReferenceValue(
                objectWrapper,
                ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_CONNECTOR_REF));
    }

    public static boolean isConnectionComplete(ConnectorDevelopmentDetailsModel detailsModel) {
        return isConnectionComplete(detailsModel, null);
    }

    public static boolean isConnectionComplete(ConnectorDevelopmentDetailsModel detailsModel, String panelType) {
        try {
            ObjectDetailsModels<ResourceType> resourceModel = ConnectorDevelopmentWizardUtil.getTestingResourceModel(
                    detailsModel, panelType);
            OperationalStateType stateBean = resourceModel.getObjectType().getOperationalState();
            return stateBean != null && stateBean.getLastAvailabilityStatus() != null && stateBean.getLastAvailabilityStatus() == AvailabilityStatusType.UP;
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get testing resource.", e);
            String message = detailsModel.getPageAssignmentHolder().getString("ConnectorDevelopmentWizardUtil.couldntGetTestingResource");
            detailsModel.getPageAssignmentHolder().error(message);
        }
        return false;
    }

    public static boolean isInitObjectClassSchemaOperationComplete(ConnectorDevelopmentDetailsModel detailsModel) {
        return isInitObjectClassOperationComplete(detailsModel, ConnDevObjectClassInfoType.F_NATIVE_SCHEMA_SCRIPT);
    }

    public static boolean isInitObjectClassSearchAllOperationComplete(ConnectorDevelopmentDetailsModel detailsModel) {
        return isInitObjectClassOperationComplete(detailsModel, ConnDevObjectClassInfoType.F_SEARCH_ALL_OPERATION);
    }

    private static boolean isInitObjectClassOperationComplete(ConnectorDevelopmentDetailsModel detailsModel, ItemName path) {
        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassContainer = detailsModel.getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_OBJECT_CLASS));
            if (objectClassContainer == null || objectClassContainer.getValues().isEmpty()) {
                return false;
            }

            if (objectClassContainer.getValues().size() >= 2) {
                return true;
            }

            PrismContainerValueWrapper<ConnDevObjectClassInfoType> objectClassValue = objectClassContainer.getValues().get(0);
            PrismContainerWrapper<ConnDevArtifactType> scriptContainer = objectClassValue.findContainer(path);
            if (scriptContainer == null) {
                return false;
            }

            PrismContainerValueWrapper<ConnDevArtifactType> searchAllValue = scriptContainer.getValue();
            PrismPropertyWrapper<String> filename = searchAllValue.findProperty(ConnDevArtifactType.F_FILENAME);
            if (filename == null || filename.getValue() == null || filename.getValue().getRealValue() == null) {
                return false;
            }

            PrismPropertyWrapper<Boolean> confirm = searchAllValue.findProperty(ConnDevArtifactType.F_CONFIRM);
            return confirm != null && confirm.getValue() != null && Boolean.TRUE.equals(confirm.getValue().getRealValue());

        } catch (SchemaException e) {
            LOGGER.error("Couldn't determine whether the schema operation is complete.", e);
            String message = detailsModel.getPageAssignmentHolder().getString("ConnectorDevelopmentWizardUtil.couldntDetermineOpComplete");
            detailsModel.getPageAssignmentHolder().error(message);
        }
        return false;
    }

    public static boolean isOperationStarted(
            ConnectorDevelopmentDetailsModel detailsModel,
            ConnectorDevelopmentArtifacts.KnownArtifactType classification,
            String objectClassName) {

        return existTask(
                WorkDefinitionsType.F_GENERATE_CONNECTOR_ARTIFACT,
                objectClassName,
                classification,
                detailsModel.getConnectorDevelopmentOperation().getObject().getOid(),
                detailsModel.getPageAssignmentHolder())
                || existScript(detailsModel, classification, objectClassName);

    }
}
