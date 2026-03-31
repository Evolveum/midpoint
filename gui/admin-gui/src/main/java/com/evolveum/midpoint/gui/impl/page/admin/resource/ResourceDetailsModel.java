/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.removeWholeTaskObject;
import static com.evolveum.midpoint.schema.SchemaConstantsGenerated.C_CORRELATION_SUGGESTION;
import static com.evolveum.midpoint.schema.SchemaConstantsGenerated.C_MAPPINGS_SUGGESTION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType.F_FOCUS_TYPE_SUGGESTION;

public class ResourceDetailsModel extends AssignmentHolderDetailsModel<ResourceType> {

    private static final String DOT_CLASS = ResourceDetailsModel.class.getName() + ".";
    private static final String OPERATION_FETCH_SCHEMA = DOT_CLASS + "fetchSchema";
    private static final Trace LOGGER = TraceManager.getTrace(ResourceDetailsModel.class);

    private static final @NotNull List<ItemName> OBJECT_TYPE_SUGGESTION_ACTIVITY_TYPES = List.of(
            F_FOCUS_TYPE_SUGGESTION,
            C_CORRELATION_SUGGESTION,
            C_MAPPINGS_SUGGESTION);

    private final LoadableModel<List<ObjectClassWrapper>> objectClassesModel;

    public ResourceDetailsModel(LoadableDetachableModel<PrismObject<ResourceType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        this.objectClassesModel = new LoadableModel<>() {
            @Override
            protected List<ObjectClassWrapper> load() {
                List<ObjectClassWrapper> list = new ArrayList<>();

                // First, we get the list of all object classes right from the connector (ignoring generation constraints).
                ResourceType resourceType = new ResourceType().name("test");
                PrismObject<ResourceType> currentResource;
                try {
                    currentResource = getObjectWrapper().getObjectApplyDelta();
                } catch (CommonException e) {
                    throw new RuntimeException(e);
                }
                WebPrismUtil.cleanupEmptyContainers(currentResource);
                resourceType.connectorRef(currentResource.asObjectable().getConnectorRef())
                        .connectorConfiguration(currentResource.asObjectable().getConnectorConfiguration());
                OperationResult result = new OperationResult("bla");
                @Nullable ResourceSchema schemaForConnector = getPageBase().getModelService().fetchSchema(resourceType.asPrismObject(), result);

                if (schemaForConnector == null) {
                    return list;
                }

                for (ResourceObjectClassDefinition definition : schemaForConnector.getObjectClassDefinitions()) {
                    list.add(new ObjectClassWrapper(definition));
                }

                Collections.sort(list);

                // Now we mark all classes that are actually used in schemaHandling as selected and disabled (so they
                // cannot be deselected).

                ResourceSchema currentSchema = getResourceSchema(getObjectWrapper(), getPageBase());

                if (currentSchema == null) {
                    return list;
                }

                currentSchema.getObjectTypeDefinitions().forEach(objectTypeDef -> {
                    list.forEach(objectClassWrapper -> {
                        if (QNameUtil.match(objectClassWrapper.getObjectClassName(), objectTypeDef.getObjectClassName())) {
                            objectClassWrapper.setEnabled(false);
                            objectClassWrapper.setSelected(true);
                        }
                    });
                });

                return list;
            }
        };
    }

    public static ResourceSchema getResourceSchema(PrismObjectWrapper<ResourceType> objectWrapper, PageAdminLTE pageBase) {
        ResourceSchema schema = null;
        OperationResult result = new OperationResult(OPERATION_FETCH_SCHEMA);
        try {
            PrismObject<ResourceType> resource = objectWrapper.getObjectApplyDelta();
            WebPrismUtil.cleanupEmptyContainers(resource);
            schema = pageBase.getModelService().fetchSchema(resource, result);
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError("Cannot load schema object classes, " + e.getMessage());
            result.computeStatus();
            pageBase.showResult(result);
        }
        return schema;
    }

    public CompleteResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
        @NotNull ResourceType resource = getObjectWrapperModel().getObject().getObjectOld().asObjectable().clone();
        WebPrismUtil.cleanupEmptyContainers(resource.asPrismContainer());
        return ResourceSchemaFactory.getCompleteSchema(resource, LayerType.PRESENTATION);
    }

    @Override
    public void reset() {
        super.reset();
        objectClassesModel.reset();
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        PrismObject<ResourceType> resource = getPrismObject();
        PrismReference connector = resource.findReference(ResourceType.F_CONNECTOR_REF);
        String connectorOid = connector != null ? connector.getOid() : null;
        GuiResourceDetailsPageType resourceDetailsConfiguration = getModelServiceLocator().getCompiledGuiProfile().findResourceDetailsConfiguration(connectorOid);
        return applyArchetypePolicy(resourceDetailsConfiguration);
    }

    public IModel<List<ObjectClassWrapper>> getObjectClassesModel() {
        return objectClassesModel;
    }

    public PageResource getPageResource() {
        return (PageResource) super.getPageBase();
    }

    @Override
    protected WrapperContext createWrapperContext(Task task, OperationResult result) {
        WrapperContext ctx = super.createWrapperContext(task, result);
        ctx.setConfigureMappingType(true);
        return ctx;
    }

    private ResourceSchema getResourceSchemaOrNothing() {
        try {
            return getRefinedSchema();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot load resource schema", e);
            return null;
        }

    }

    public ResourceObjectTypeDefinition getDefaultObjectType(ShadowKindType kind) {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();
        if (resourceSchema == null) {
            return null;
        }
        ResourceObjectDefinition defaultObjectType = resourceSchema.findDefaultDefinitionForKind(kind);
        if (defaultObjectType instanceof ResourceObjectTypeDefinition) {
            return (ResourceObjectTypeDefinition) defaultObjectType;
        }
        List<? extends ResourceObjectTypeDefinition> objectTypes = getResourceObjectTypesDefinitions(kind);
        if (objectTypes.isEmpty()) {
            return null;
        }
        return objectTypes.iterator().next();
    }

    public ResourceObjectTypeDefinition getObjectTypeDefinition(ShadowKindType kind, String intent) {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();
        if (resourceSchema == null) {
            return null;
        }

        if (StringUtils.isEmpty(intent)) {
            return getDefaultObjectType(kind);
        }

        ResourceObjectTypeDefinition defaultObjectType = resourceSchema.getObjectTypeDefinition(kind, intent);
        if (defaultObjectType == null) {
            return getDefaultObjectType(kind);
        }

        return defaultObjectType;
    }

    public List<? extends ResourceObjectTypeDefinition> getResourceObjectTypesDefinitions(ShadowKindType kind) {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();

        if (resourceSchema == null) {
            return null;
        }

        return resourceSchema.getObjectTypeDefinitions(kind);
    }

    public QName getDefaultObjectClass() {
        List<QName> objectClassDefinitions = getResourceObjectClassesDefinitions();
        if (objectClassDefinitions == null) {
            return null;
        }
        return objectClassDefinitions.iterator().next();
    }

    public List<QName> getResourceObjectClassesDefinitions() {
        ResourceSchema resourceSchema = getResourceSchemaOrNothing();

        if (resourceSchema == null) {
            return null;
        }
        return resourceSchema.getObjectClassDefinitions()
                .stream()
                .map(ResourceObjectDefinition::getObjectClassName)
                .collect(Collectors.toList());
    }

    public ResourceObjectDefinition findResourceObjectClassDefinition(QName objectClass) {
        ResourceSchema schema = getResourceSchemaOrNothing();
        if (schema == null) {
            return null;
        }
        return schema.findObjectClassDefinition(objectClass);
    }

    /**
     * Performs post-save cleanup for deleted object types.
     *
     * <p>When an object type is removed, any related suggestion tasks are deleted as well,
     * so that newly created object types do not reuse stale focus type, correlation,
     * or mapping suggestions.</p>
     */
    @Override
    public void performAfterSavePerformed(
            @Nullable Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas,
            @NotNull AjaxRequestTarget target,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (result.isError() || executedDeltas == null || executedDeltas.isEmpty()) {
            return;
        }

        String resourceOid = getObjectType() != null ? getObjectType().getOid() : null;
        if (resourceOid == null) {
            return;
        }

        PageBase pageBase = getPageBase();
        SmartIntegrationService smartIntegrationService = pageBase.getSmartIntegrationService();

        List<ResourceObjectTypeDefinitionType> deletedObjectTypes = findDeletedObjectTypes(executedDeltas);
        if (deletedObjectTypes.isEmpty()) {
            return;
        }

        for (ResourceObjectTypeDefinitionType deletedObjectType : deletedObjectTypes) {
            removeRelatedSuggestionTasks(
                    deletedObjectType,
                    resourceOid,
                    smartIntegrationService,
                    pageBase,
                    task,
                    result);
        }

        result.computeStatusIfUnknown();
    }

    private void removeRelatedSuggestionTasks(
            @NotNull ResourceObjectTypeDefinitionType deletedObjectType,
            @NotNull String resourceOid,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull PageBase pageBase,
            @NotNull Task task,
            @NotNull OperationResult result) {

        try {
            ResourceObjectTypeIdentification objectTypeIdentification =
                    ResourceObjectTypeIdentification.of(deletedObjectType);

            SearchResultList<PrismObject<TaskType>> relatedTasks =
                    smartIntegrationService.listObjectTypeRelatedSuggestionTasks(
                            objectTypeIdentification,
                            resourceOid,
                            OBJECT_TYPE_SUGGESTION_ACTIVITY_TYPES,
                            task,
                            result);

            for (PrismObject<TaskType> taskToDelete : relatedTasks) {
                removeWholeTaskObject(pageBase, task, result, taskToDelete.getOid());
            }
        } catch (Exception e) {
            LOGGER.warn(
                    "Couldn't delete suggestion tasks for object type '{}'/{}: {}",
                    deletedObjectType.getKind(),
                    deletedObjectType.getIntent(),
                    e.getMessage(),
                    e);
        }
    }

    /**
     * Extracts object types that were deleted in the provided executed deltas.
     */
    private @NotNull List<ResourceObjectTypeDefinitionType> findDeletedObjectTypes(
            @NotNull Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas) {

        List<ResourceObjectTypeDefinitionType> deleted = new ArrayList<>();

        for (ObjectDeltaOperation<? extends ObjectType> odo : executedDeltas) {
            if (odo == null || odo.getObjectDelta() == null) {
                continue;
            }

            ObjectDelta<? extends ObjectType> delta = odo.getObjectDelta();
            if (!ResourceType.class.equals(delta.getObjectTypeClass())) {
                continue;
            }

            List<PrismValue> deletedValues = delta.getDeletedValuesFor(
                    ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));

            if (deletedValues == null || deletedValues.isEmpty()) {
                continue;
            }

            for (PrismValue deletedValue : deletedValues) {
                if (!(deletedValue instanceof PrismContainerValue<?> containerValue)) {
                    continue;
                }

                Containerable containerable = containerValue.asContainerable();
                if (containerable instanceof ResourceObjectTypeDefinitionType objectType) {
                    deleted.add(objectType.clone());
                }
            }
        }

        return deleted;
    }
}
