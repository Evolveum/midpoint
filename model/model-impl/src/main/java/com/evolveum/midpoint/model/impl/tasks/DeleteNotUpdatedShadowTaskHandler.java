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
import com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext;
import com.evolveum.midpoint.model.impl.tasks.simple.Processing;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleIterativeTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class DeleteNotUpdatedShadowTaskHandler
        extends SimpleIterativeTaskHandler
        <ShadowType,
                DeleteNotUpdatedShadowTaskHandler.MyExecutionContext,
                DeleteNotUpdatedShadowTaskHandler.MyProcessing> {

    public static final String HANDLER_URI = ModelPublicConstants.DELETE_NOT_UPDATE_SHADOW_TASK_HANDLER_URI;

    private static final ItemName NOT_UPDATED_DURATION_PROPERTY_NAME = new ItemName(ModelConstants.NS_EXTENSION, "notUpdatedShadowsDuration");

    private static final Trace LOGGER = TraceManager.getTrace(DeleteNotUpdatedShadowTaskHandler.class);

    public DeleteNotUpdatedShadowTaskHandler() {
        super(LOGGER, "DeleteNotUpdatedShadow", OperationConstants.DELETE_NOT_UPDATED_SHADOWS);
        reportingOptions.setPreserveStatistics(false);
        reportingOptions.setSkipWritingOperationExecutionRecords(true); // because the shadows are deleted anyway
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected MyExecutionContext createExecutionContext() {
        return new MyExecutionContext();
    }

    @Override
    protected MyProcessing createProcessing(MyExecutionContext ctx) {
        return new MyProcessing(ctx);
    }

    public class MyExecutionContext extends ExecutionContext {

        private PrismObject<ResourceType> resource;

        @Override
        protected void initialize(OperationResult opResult) throws CommunicationException, ObjectNotFoundException,
                SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
            resource = getResource(getLocalCoordinationTask(), opResult);
            checkResource();
        }

        private PrismObject<ResourceType> getResource(Task task, OperationResult opResult) throws CommunicationException,
                ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
                ExpressionEvaluationException {
            String resourceOid = task.getObjectOid();
            if (resourceOid == null) {
                throw new IllegalArgumentException("No resource OID specified for the task");
            }
            return provisioningService.getObject(ResourceType.class, resourceOid, null, task, opResult);
        }

        private void checkResource() {
            PrismProperty<AvailabilityStatusType> status = resource.findProperty(ItemPath.create(ResourceType.F_OPERATIONAL_STATE,
                    OperationalStateType.F_LAST_AVAILABILITY_STATUS));

            if (status == null || !AvailabilityStatusType.UP.equals(status.getRealValue())) {
                throw new IllegalArgumentException("Resource has to have value of last availability status on UP");
            }
        }
    }

    public class MyProcessing extends Processing<ShadowType, MyExecutionContext> {

        private MyProcessing(MyExecutionContext ctx) {
            super(ctx);
        }

        @Override
        protected Class<? extends ShadowType> determineObjectType(Class<? extends ShadowType> configuredType) {
            return ShadowType.class;
        }

        @Override
        protected ObjectQuery createQuery(ObjectQuery configuredQuery) throws CommunicationException, ObjectNotFoundException,
                SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

            RunningTask task = ctx.getLocalCoordinationTask();
            Duration notUpdatedDuration = task.getExtensionPropertyRealValue(NOT_UPDATED_DURATION_PROPERTY_NAME);
            if (notUpdatedDuration == null) {
                throw new IllegalArgumentException("Duration for deleting not updated shadow is missing in task extension");
            }

            ObjectClassComplexTypeDefinition objectclassDef;

            RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(ctx.resource, LayerType.MODEL, prismContext);
            objectclassDef = ModelImplUtils.determineObjectClass(refinedSchema, task);

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
                    .and().item(ShadowType.F_RESOURCE_REF).ref(ObjectTypeUtil.createObjectRef(ctx.resource, prismContext).asReferenceValue())
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectclassDef.getTypeName())
                    .build();

            LOGGER.trace("Shadow query:\n{}", query.debugDumpLazily());

            return query;
        }

        @Override
        protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
                Collection<SelectorOptions<GetOperationOptions>> configuredOptions) {
            return schemaHelper.getOperationOptionsBuilder()
                    .noFetch()
                    .build();
        }

        @Override
        protected void handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            deleteShadow(object, workerTask, result);
        }

        private void deleteShadow(PrismObject<ShadowType> shadow, Task workerTask, OperationResult result) {
            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setObjectDelta(shadow.createDeleteDelta());
            change.setResource(ctx.resource);
            change.setShadowedResourceObject(shadow);
            synchronizationService.notifyChange(change, workerTask, result);
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
}
