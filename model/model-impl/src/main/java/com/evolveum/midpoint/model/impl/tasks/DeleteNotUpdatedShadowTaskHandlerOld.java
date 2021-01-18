/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import java.util.Collection;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
@TaskExecutionClass(AbstractSearchIterativeTaskExecution.class)
@PartExecutionClass(DeleteNotUpdatedShadowTaskHandlerOld.PartExecution.class)
@DefaultHandledObjectType(ShadowType.class)
public class DeleteNotUpdatedShadowTaskHandlerOld
        extends AbstractSearchIterativeModelTaskHandler
        <DeleteNotUpdatedShadowTaskHandlerOld, DeleteNotUpdatedShadowTaskHandlerOld.TaskExecution> {

    public static final String HANDLER_URI = ModelPublicConstants.DELETE_NOT_UPDATE_SHADOW_TASK_HANDLER_URI + ".OLD";

    private static final ItemName NOT_UPDATED_DURATION_PROPERTY_NAME = new ItemName(ModelConstants.NS_EXTENSION, "notUpdatedShadowsDuration");

    private static final Trace LOGGER = TraceManager.getTrace(DeleteNotUpdatedShadowTaskHandlerOld.class);

    public DeleteNotUpdatedShadowTaskHandlerOld() {
        super("DeleteNotUpdatedShadow", OperationConstants.DELETE_NOT_UPDATED_SHADOWS);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @HandledObjectType(ShadowType.class)
    @ResultHandlerClass(PartExecution.Handler.class)
    protected class PartExecution
            extends AbstractSearchIterativeModelTaskPartExecution
            <ShadowType,
                    DeleteNotUpdatedShadowTaskHandlerOld,
                    DeleteNotUpdatedShadowTaskHandlerOld.TaskExecution,
                    PartExecution,
                    PartExecution.Handler> {

        private PrismObject<ResourceType> resource;

        public PartExecution(DeleteNotUpdatedShadowTaskHandlerOld.TaskExecution taskExecution) {
            super(taskExecution);
        }

        @Override
        protected void initialize(OperationResult opResult) throws SchemaException, ConfigurationException,
                ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
            resource = getResource(localCoordinatorTask);
            checkResource(resource);
        }

        @Override
        protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
                OperationResult opResult) {
            return SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        }

        private PrismObject<ResourceType> getResource(Task task) throws CommunicationException, ObjectNotFoundException,
                SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
            String resourceOid = task.getObjectOid();
            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is missing in task extension");
            }
            return provisioningService.getObject(ResourceType.class, resourceOid, null, task, task.getResult());
        }

        private void checkResource(PrismObject<ResourceType> resource) {
            PrismProperty<AvailabilityStatusType> status = resource.findProperty(ItemPath.create(ResourceType.F_OPERATIONAL_STATE,
                    OperationalStateType.F_LAST_AVAILABILITY_STATUS));

            if (status == null || !AvailabilityStatusType.UP.equals(status.getRealValue())) {
                throw new IllegalArgumentException("Resource has to have value of last availability status on UP");
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

        @Override
        protected ObjectQuery createQuery(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
                SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
            Duration notUpdatedDuration = localCoordinatorTask.getExtensionPropertyRealValue(NOT_UPDATED_DURATION_PROPERTY_NAME);
            if (notUpdatedDuration == null) {
                throw new IllegalArgumentException("Duration for deleting not updated shadow is missing in task extension");
            }

            String resourceOid = localCoordinatorTask.getObjectOid();

            if (resourceOid == null) {
                throw new IllegalArgumentException("Resource OID is missing in task extension");
            }

            PrismObject<ResourceType> resource = getResource(localCoordinatorTask);
            ObjectClassComplexTypeDefinition objectclassDef;

            RefinedResourceSchema refinedSchema;
            try {
                refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
                objectclassDef = ModelImplUtils.determineObjectClass(refinedSchema, localCoordinatorTask);
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

        protected class Handler
                extends AbstractSearchIterativeResultHandler
                <ShadowType,
                        DeleteNotUpdatedShadowTaskHandlerOld,
                        DeleteNotUpdatedShadowTaskHandlerOld.TaskExecution,
                        PartExecution,
                        Handler> {

            public Handler(PartExecution taskExecution) {
                super(taskExecution);
            }

            @Override
            protected boolean handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result)
                    throws CommonException, PreconditionViolationException {
                deleteShadow(object, resource, workerTask, result);
                return true;
            }

            private void deleteShadow(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, Task workerTask, OperationResult result) {
                ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
                change.setObjectDelta(shadow.createDeleteDelta());
                change.setResource(resource);
                change.setOldShadow(shadow);
                change.setCurrentShadow(shadow);
                synchronizationService.notifyChange(change, workerTask, result);
            }
        }
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

    /** Just to make Java compiler happy. */
    protected static class TaskExecution
            extends AbstractSearchIterativeTaskExecution<DeleteNotUpdatedShadowTaskHandlerOld, DeleteNotUpdatedShadowTaskHandlerOld.TaskExecution> {

        public TaskExecution(DeleteNotUpdatedShadowTaskHandlerOld taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
