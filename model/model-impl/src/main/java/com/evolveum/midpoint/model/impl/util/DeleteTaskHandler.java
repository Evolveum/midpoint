/*
 * Copyright (c) 2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@Component
public class DeleteTaskHandler implements TaskHandler {
	
	public static final String HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/delete/handler-3";
	
	@Autowired(required=true)
	protected TaskManager taskManager;
	
	@Autowired(required=true)
	protected ModelService modelService;
	
	@Autowired(required = true)
	protected PrismContext prismContext;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(DeleteTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}
	
	@Override
	public TaskRunResult run(Task task) {
		return runInternal(task);
	}
	
	public <O extends ObjectType> TaskRunResult runInternal(Task task) {
		LOGGER.trace("Delete task run starting ({})", task);
		long startTimestamp = System.currentTimeMillis();
		
		OperationResult opResult = new OperationResult("DeleteTask.run");
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		
		opResult.setSummarizeErrors(true);
		opResult.setSummarizePartialErrors(true);
		opResult.setSummarizeSuccesses(true);

		QueryType queryType;
		PrismProperty<QueryType> objectQueryPrismProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        if (objectQueryPrismProperty != null && objectQueryPrismProperty.getRealValue() != null) {
        	queryType = objectQueryPrismProperty.getRealValue();
        } else {
        	// For "foolproofness" reasons we really require a query. Even if it is "ALL" query.
        	LOGGER.error("No query parameter in {}", task);
            opResult.recordFatalError("No query parameter in " + task);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
        
        Class<O> objectType;
        PrismProperty<QName> objectTypePrismProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        if (objectTypePrismProperty != null && objectTypePrismProperty.getRealValue() != null) {
        	objectType = (Class<O>) ObjectTypes.getObjectTypeFromTypeQName(objectTypePrismProperty.getRealValue()).getClassDefinition();
        } else {
        	LOGGER.error("No object type parameter in {}", task);
            opResult.recordFatalError("No object type parameter in " + task);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
		
        ObjectQuery query;
        try {
        	 query = QueryJaxbConvertor.createObjectQuery(objectType, queryType, prismContext);
             if (LOGGER.isTraceEnabled()) {
                 LOGGER.trace("Using object query from the task: {}", query.debugDump());
             }
        } catch (SchemaException ex) {
        	LOGGER.error("Schema error while creating a search filter: {}", new Object[]{ex.getMessage(), ex});
            opResult.recordFatalError("Schema error while creating a search filter: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
        		
        boolean optionRaw = true;
        PrismProperty<Boolean> optionRawPrismProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_OPTION_RAW);
        if (optionRawPrismProperty != null && optionRawPrismProperty.getRealValue() != null && !optionRawPrismProperty.getRealValue()) {
        	optionRaw = false;
        }
        
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deleting {}, raw={} using query:\n{}", 
					new Object[]{objectType.getSimpleName(), optionRaw, query.debugDump()});
		}

		
		boolean countObjectsOnStart = true; // TODO
		long progress = 0;
		
		Integer maxSize = 100;
		ObjectPaging paging = ObjectPaging.createPaging(0, maxSize);
		query.setPaging(paging);
		
		Collection<SelectorOptions<GetOperationOptions>> searchOptions = null;
		ModelExecuteOptions execOptions = null;
		if (optionRaw) {
			searchOptions = SelectorOptions.createCollection(GetOperationOptions.createRaw());
			execOptions = ModelExecuteOptions.createRaw();
		}
		
		try {

            // counting objects can be within try-catch block, because the handling is similar to handling errors within searchIterative
            Long expectedTotal = null;
			if (countObjectsOnStart) {
				Integer expectedTotalInt = modelService.countObjects(objectType, query, searchOptions, task, opResult);
                LOGGER.trace("Expecting {} objects to be deleted", expectedTotal);
                if (expectedTotalInt != null) {
                    expectedTotal = (long) expectedTotalInt;        // conversion would fail on null
                }
            }

			runResult.setProgress(progress);
            task.setProgress(progress);
            if (expectedTotal != null) {
                task.setExpectedTotal(expectedTotal);
            }
            try {
                task.savePendingModifications(opResult);
            } catch (ObjectAlreadyExistsException e) {      // other exceptions are handled in the outer try block
                throw new IllegalStateException("Unexpected ObjectAlreadyExistsException when updating task progress/expectedTotal", e);
            }

            SearchResultList<PrismObject<O>> objects;
            while (true) {
            	
	            objects = modelService.searchObjects(objectType, query, searchOptions, task, opResult);
	            
	            if (objects.isEmpty()) {
	            	break;
	            }
	            
	            int skipped = 0;
	            for(PrismObject<O> object: objects) {
	            	
	            	if (!optionRaw && ShadowType.class.isAssignableFrom(objectType) 
	            			&& Boolean.TRUE == ((ShadowType)(object.asObjectable())).isProtectedObject()) {
	            		LOGGER.debug("Skipping delete of protected object {}", object);
	            		skipped++;
	            		continue;
	            	}
	            	
	            	ObjectDelta<?> delta = ObjectDelta.createDeleteDelta(objectType, object.getOid(), prismContext);
					modelService.executeChanges(MiscSchemaUtil.createCollection(delta), execOptions, task, opResult);
					progress++;
	            }
	            
	            opResult.summarize();
	            task.setProgress(progress);
	            if (LOGGER.isTraceEnabled()) {
	            	LOGGER.trace("Deleted {} objects, result:\n{}", progress, opResult.debugDump());
	            }
	            
	            if (objects.size() == skipped) {
	            	break;
	            }
	            
            }

        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("{}", new Object[]{e.getMessage(), e});
            opResult.recordFatalError("Object not found " + e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            runResult.setProgress(progress);
            return runResult;
		} catch (CommunicationException e) {
            LOGGER.error("{}", new Object[]{e.getMessage(), e});
            opResult.recordFatalError("Object not found " + e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            runResult.setProgress(progress);
            return runResult;
		}

        runResult.setProgress(progress);
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        opResult.summarize();
        opResult.recordSuccess();

        long wallTime = System.currentTimeMillis() - startTimestamp;

        String finishMessage = "Finished delete (" + task + "). ";
        String statistics = "Processed " + progress + " objects in " + wallTime/1000 + " seconds.";
        if (progress > 0) {
            statistics += " Wall clock time average: " + ((float) wallTime / (float) progress) + " milliseconds";
        }

        opResult.createSubresult(DeleteTaskHandler.class.getName() + ".statistics").recordStatus(OperationResultStatus.SUCCESS, statistics);

        LOGGER.info(finishMessage + statistics);
        
        LOGGER.trace("Run finished (task {}, run result {})", new Object[]{task, runResult});

        return runResult;
		
	}

    @Override
    public Long heartbeat(Task task) {
    	return task.getProgress();
    }
    
    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

	@Override
	public String getCategoryName(Task task) {
		return TaskCategory.UTIL;
	}

	@Override
	public List<String> getCategoryNames() {
		return null;
	}
	
}
