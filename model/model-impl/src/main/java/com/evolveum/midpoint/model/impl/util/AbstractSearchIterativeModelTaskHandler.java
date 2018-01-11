/*
 * Copyright (c) 2010-2018 Evolveum
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

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author semancik
 *
 */
public abstract class AbstractSearchIterativeModelTaskHandler<O extends ObjectType, H extends AbstractSearchIterativeResultHandler<O>> extends AbstractSearchIterativeTaskHandler<O,H> {

	// WARNING! This task handler is efficiently singleton!
	// It is a spring bean and it is supposed to handle all search task instances
	// Therefore it must not have task-specific fields. It can only contain fields specific to
	// all tasks of a specified type
	// If you need to store fields specific to task instance or task run the ResultHandler is a good place to do that.

	@Autowired
	protected ModelObjectResolver modelObjectResolver;

	@Autowired
	protected SecurityEnforcer securityEnforcer;

	@Autowired
	protected ExpressionFactory expressionFactory;

	@Autowired
	protected SystemObjectCache systemObjectCache;

	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeModelTaskHandler.class);

	protected AbstractSearchIterativeModelTaskHandler(String taskName, String taskOperationPrefix) {
		super(taskName, taskOperationPrefix);
	}

	@NotNull
	@Override
	public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
		return new StatisticsCollectionStrategy(!isPreserveStatistics(), isEnableIterationStatistics(),
				isEnableSynchronizationStatistics(), isEnableActionsExecutedStatistics());
	}
	
	@Override
	protected ObjectQuery preProcessQuery(ObjectQuery query, Task coordinatorTask, OperationResult opResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		// TODO consider which variables should go here (there's no focus, shadow, resource - only configuration)
		if (ExpressionUtil.hasExpressions(query.getFilter())) {
			PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(opResult);
			ExpressionVariables variables = Utils.getDefaultExpressionVariables(null, null, null,
					configuration != null ? configuration.asObjectable() : null);
			try {
				ExpressionEnvironment<?> env = new ExpressionEnvironment<>(coordinatorTask, opResult);
				ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
				query = ExpressionUtil.evaluateQueryExpressions(query, variables, expressionFactory,
						prismContext, "evaluate query expressions", coordinatorTask, opResult);
			} finally {
				ModelExpressionThreadLocalHolder.popExpressionEnvironment();
			}
		}
		
		return query;
	}
	
	@Override
	protected <O extends ObjectType> Integer countObjects(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> queryOptions, Task coordinatorTask, OperationResult opResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		return modelObjectResolver.countObjects(type, query, queryOptions, coordinatorTask, opResult);
	}
	
	@Override
	protected <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> searchOptions, ResultHandler<O> resultHandler, Object coordinatorTask, OperationResult opResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		modelObjectResolver.searchIterative((Class<O>) type, query, searchOptions, resultHandler, coordinatorTask, opResult);
	}
	
    protected <T extends ObjectType> T resolveObjectRef(Class<T> type, TaskRunResult runResult, Task task, OperationResult opResult) {
    	String typeName = type.getClass().getSimpleName();
    	String objectOid = task.getObjectOid();
        if (objectOid == null) {
            LOGGER.error("Import: No {} OID specified in the task", typeName);
            opResult.recordFatalError("No "+typeName+" OID specified in the task");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }

        T objectType;
        try {

        	objectType = modelObjectResolver.getObject(type, objectOid, null, task, opResult);

        } catch (ObjectNotFoundException ex) {
            LOGGER.error("Import: {} {} not found: {}", typeName, objectOid, ex.getMessage(), ex);
            // This is bad. The resource does not exist. Permanent problem.
            opResult.recordFatalError(typeName+" not found " + objectOid, ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        } catch (SchemaException ex) {
            LOGGER.error("Import: Error dealing with schema: {}", ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            return null;
        } catch (RuntimeException ex) {
            LOGGER.error("Import: Internal Error: {}", ex.getMessage(), ex);
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        } catch (CommunicationException ex) {
        	LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            return null;
		} catch (ConfigurationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		} catch (SecurityViolationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		} catch (ExpressionEvaluationException ex) {
			LOGGER.error("Import: Error getting {} {}: {}", typeName, objectOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting "+typeName+" " + objectOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
		}

        if (objectType == null) {
            LOGGER.error("Import: No "+typeName+" specified");
            opResult.recordFatalError("No "+typeName+" specified");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }

        return objectType;
    }

    protected ModelExecuteOptions getExecuteOptionsFromTask(Task task) {
		PrismProperty<ModelExecuteOptionsType> property = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
		return property != null ? ModelExecuteOptions.fromModelExecutionOptionsType(property.getRealValue()) : null;
	}
}
