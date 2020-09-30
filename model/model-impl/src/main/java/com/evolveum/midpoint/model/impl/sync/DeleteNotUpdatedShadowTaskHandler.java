/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import java.util.Collection;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.task.api.*;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 */
@Component
public class DeleteNotUpdatedShadowTaskHandler extends AbstractSearchIterativeModelTaskHandler<ShadowType, AbstractSearchIterativeResultHandler<ShadowType>> {

    public static final String HANDLER_URI = ModelPublicConstants.DELETE_NOT_UPDATE_SHADOW_TASK_HANDLER_URI;
    private static final ItemName NOT_UPDATED_DURATION_PROPERTY_NAME = new ItemName(ModelConstants.NS_EXTENSION, "notUpdatedShadowsDuration");

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private SynchronizationService synchronizationService;
    @Autowired private Clock clock;
    @Autowired private ProvisioningService provisioningService;

    private static final Trace LOGGER = TraceManager.getTrace(DeleteNotUpdatedShadowTaskHandler.class);

    public DeleteNotUpdatedShadowTaskHandler() {
        super("DeleteNotUpdatedShadow", OperationConstants.DELETE_NOT_UPDATED_SHADOWS);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    protected Class<? extends ObjectType> getType(Task task) {
        return getTypeFromTask(task, ShadowType.class);
    }

    @Override
    protected AbstractSearchIterativeResultHandler<ShadowType> createHandler(TaskPartitionDefinitionType partition,
            TaskRunResult runResult, final RunningTask coordinatorTask, OperationResult opResult) {

        PrismObject<ResourceType> resource = getResource(coordinatorTask);
        checkResource(resource);

        AbstractSearchIterativeResultHandler<ShadowType> handler = new AbstractSearchIterativeResultHandler<ShadowType>(
                coordinatorTask, DeleteNotUpdatedShadowTaskHandler.class.getName(), "delete not updated shadow",
                "delete not updated shadow task", partition, taskManager) {
            @Override
            protected boolean handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result) {
                deleteShadow(object, resource, workerTask, result);
                return true;
            }
        };
        handler.setStopOnError(false);
        return handler;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
            AbstractSearchIterativeResultHandler<ShadowType> resultHandler, TaskRunResult runResult,
            Task coordinatorTask, OperationResult opResult) {
        return SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
    }

    @Override
    protected ObjectQuery createQuery(AbstractSearchIterativeResultHandler<ShadowType> handler, TaskRunResult runResult,
            Task task, OperationResult opResult) {
        Duration notUpdatedDuration = task.getExtensionPropertyRealValue(NOT_UPDATED_DURATION_PROPERTY_NAME);
        if (notUpdatedDuration == null) {
            throw new IllegalArgumentException("Duration for deleting not updated shadow is missing in task extension");
        }

        String resourceOid = task.getObjectOid();

        if (resourceOid == null) {
            throw new IllegalArgumentException("Resource OID is missing in task extension");
        }

        PrismObject<ResourceType> resource = getResource(task);
        ObjectClassComplexTypeDefinition objectclassDef;

        RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
            objectclassDef = ModelImplUtils.determineObjectClass(refinedSchema, task);
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            throw processErrorAndCreateException("Couldn't determine object class: Error dealing with schema", ex, opResult);
        }

        LOGGER.trace("Resource {}", resource);

        if (notUpdatedDuration.getSign() == 1) {
            notUpdatedDuration.negate();
        }
        Date deletingDate =  new Date(clock.currentTimeMillis());
        notUpdatedDuration.addTo(deletingDate);

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .block()
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(XmlTypeConverter.createXMLGregorianCalendar(deletingDate))
                    .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                .endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(ObjectTypeUtil.createObjectRef(resource, prismContext).asReferenceValue())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(objectclassDef.getTypeName())
                .build();

        LOGGER.trace("Shadow query:\n{}", query.debugDumpLazily());

        return query;
    }

    /**
     * Note that on any error we simply throw SystemException from this method. Not the best approach but the callers' methods
     * signatures do not allow for any better (for now).
     */
    private PrismObject<ResourceType> getResource(Task task) {
        String resourceOid = task.getObjectOid();
        if (resourceOid == null) {
            throw new IllegalArgumentException("Resource OID is missing in task extension");
        }

        try {
            return provisioningService.getObject(ResourceType.class, resourceOid, null, task, task.getResult());
        } catch (ObjectNotFoundException ex) {
            // This is bad. The resource does not exist. Permanent problem.
            throw processErrorAndCreateException("Resource does not exist, OID: " + resourceOid, ex, task.getResult());
        } catch (CommunicationException ex) {
            // Error, but not critical. Just try later.
            throw processErrorAndCreateException("Communication error", ex, task.getResult());
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            throw processErrorAndCreateException("Error dealing with schema", ex, task.getResult());
        } catch (RuntimeException ex) {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense
            // to retry.
            throw processErrorAndCreateException("Internal Error", ex, task.getResult());
        } catch (ConfigurationException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            throw processErrorAndCreateException("Configuration error", ex, task.getResult());
        } catch (SecurityViolationException ex) {
            throw processErrorAndCreateException("Security violation", ex, task.getResult());
        } catch (ExpressionEvaluationException ex) {
            throw processErrorAndCreateException("Expression error", ex, task.getResult());
        }
    }

    private void checkResource(PrismObject<ResourceType> resource) {
        PrismProperty<AvailabilityStatusType> status = resource.findProperty(ItemPath.create(ResourceType.F_OPERATIONAL_STATE,
                OperationalStateType.F_LAST_AVAILABILITY_STATUS));

        if (status == null || !AvailabilityStatusType.UP.equals(status.getRealValue())) {
            throw new IllegalArgumentException("Resource have to have value of last availability status on UP");
        }
    }

    private RuntimeException processErrorAndCreateException(String errorDesc, Throwable ex, OperationResult opResult) {
        String message;
        if (ex == null) {
            message = errorDesc;
        } else {
            message = errorDesc+": "+ex.getMessage();
        }
        LOGGER.error("Delete not updated shadow task handler: {}-{}", message, ex, ex);
        opResult.recordFatalError(message, ex);
        throw new SystemException(message, ex);
    }

    private void deleteShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, Task task, OperationResult result) {
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setObjectDelta(shadow.createDeleteDelta());
        change.setResource(resource);
        change.setOldShadow(shadow);
        change.setCurrentShadow(shadow);
        synchronizationService.notifyChange(change, task, result);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getDefaultChannel() {
        return Channel.CLEANUP.getUri();
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
