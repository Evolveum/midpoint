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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.task.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
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
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

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
    @Autowired private ModelService model;
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
    protected AbstractSearchIterativeResultHandler<ShadowType> createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, final RunningTask coordinatorTask,
            OperationResult opResult) {

        AbstractSearchIterativeResultHandler<ShadowType> handler = new AbstractSearchIterativeResultHandler<ShadowType>(
                coordinatorTask, DeleteNotUpdatedShadowTaskHandler.class.getName(), "delete not updated shadow", "delete not updated shadow task", partition, taskManager) {

            @Override
            protected boolean handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result) throws CommonException, PreconditionViolationException {
                deleteShadow(object, getOptions(coordinatorTask), workerTask, result);
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
            Task task, OperationResult opResult) throws SchemaException {
        Duration notUpdatedDuration = task.getExtensionPropertyRealValue(NOT_UPDATED_DURATION_PROPERTY_NAME);
        if(notUpdatedDuration == null) {
            throw new IllegalArgumentException("Duration for deleting not updated shadow is missing in task extension");
        }

        String resourceOid = task.getObjectOid();

        if (resourceOid == null) {
            throw new IllegalArgumentException("Resource OID is missing in task extension");
        }

        PrismObject<ResourceType> resource = getResource(task);
        ObjectClassComplexTypeDefinition objectclassDef = null;

        RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
            objectclassDef = ModelImplUtils.determineObjectClass(refinedSchema, task);
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorPartial("Error dealing with schema", ex, opResult);
        }

        LOGGER.trace("Resource {}", resource);

        if(notUpdatedDuration.getSign() == 1) {
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

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Shadow query:\n{}", query.debugDump());
        }

        return query;
    }

    private PrismObject<ResourceType> getResource(Task task) {

        String resourceOid = task.getObjectOid();
        if (resourceOid == null) {
            throw new IllegalArgumentException("Resource OID is missing in task extension");
        }

        PrismObject<ResourceType> resource = null;
        ObjectClassComplexTypeDefinition objectclassDef = null;
        try {
            resource = provisioningService.getObject(ResourceType.class, resourceOid, null, task, task.getResult());

            RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
            objectclassDef = ModelImplUtils.determineObjectClass(refinedSchema, task);
        } catch (ObjectNotFoundException ex) {
            // This is bad. The resource does not exist. Permanent problem.
            processErrorPartial("Resource does not exist, OID: " + resourceOid, ex, task.getResult());
        } catch (CommunicationException ex) {
            // Error, but not critical. Just try later.
            processErrorPartial("Communication error", ex, task.getResult());
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorPartial("Error dealing with schema", ex, task.getResult());
        } catch (RuntimeException ex) {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense
            // to retry.
            processErrorPartial("Internal Error", ex, task.getResult());
        } catch (ConfigurationException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            processErrorPartial("Configuration error", ex, task.getResult());
        } catch (SecurityViolationException ex) {
            processErrorPartial("Security violation", ex, task.getResult());
        } catch (ExpressionEvaluationException ex) {
            processErrorPartial("Expression error", ex, task.getResult());
        }

        if(resource == null) {
            throw new IllegalArgumentException("Resource is null");
        }

        PrismProperty<AvailabilityStatusType> status = resource.findProperty(ItemPath.create(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS));

        if(status == null || !AvailabilityStatusType.UP.equals(status.getRealValue())) {
            throw new IllegalArgumentException("Resource have to have value of last availability status on UP");
        }

        return resource;
    }

    private void processErrorPartial(String errorDesc, Exception ex, OperationResult opResult) {
        String message;
        if (ex == null) {
            message = errorDesc;
        } else {
            message = errorDesc+": "+ex.getMessage();
        }
        LOGGER.error("Reconciliation: {}-{}", new Object[]{message, ex});
        opResult.recordFatalError(message, ex);
    }


    private ModelExecuteOptions getOptions(Task coordinatorTask) throws SchemaException {
        ModelExecuteOptions modelExecuteOptions = ModelImplUtils.getModelExecuteOptions(coordinatorTask);
        if (modelExecuteOptions == null) {
            modelExecuteOptions =  ModelExecuteOptions.createReconcile();
        }
        LOGGER.trace("ModelExecuteOptions: {}", modelExecuteOptions);
        return modelExecuteOptions;
    }

    private void deleteShadow(PrismObject<ShadowType> shadow, ModelExecuteOptions options, Task task, OperationResult result) throws SchemaException,
        ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
        ConfigurationException, PolicyViolationException, SecurityViolationException, PreconditionViolationException {
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setObjectDelta(shadow.createDeleteDelta());
        change.setResource(getResource(task));
        change.setOldShadow(shadow);
        change.setCurrentShadow(shadow);
        synchronizationService.notifyChange(change, task, result);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    protected String getDefaultChannel() {
        return SchemaConstants.CHANGE_CHANNEL_DEL_NOT_UPDATED_SHADOWS_URI;
    }
}
