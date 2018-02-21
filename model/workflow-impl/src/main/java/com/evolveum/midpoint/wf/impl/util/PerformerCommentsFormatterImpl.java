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

package com.evolveum.midpoint.wf.impl.util;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WfExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class PerformerCommentsFormatterImpl implements PerformerCommentsFormatter {

	private static final Trace LOGGER = TraceManager.getTrace(PerformerCommentsFormatterImpl.class);

	@Nullable private final PerformerCommentsFormattingType formatting;
	@NotNull private final RepositoryService repositoryService;
	@NotNull private final WfExpressionEvaluationHelper expressionEvaluationHelper;

	private final Map<String, ObjectType> performersCache = new HashMap<>();

	public PerformerCommentsFormatterImpl(@Nullable PerformerCommentsFormattingType formatting,
			@NotNull RepositoryService repositoryService, @NotNull WfExpressionEvaluationHelper expressionEvaluationHelper) {
		this.formatting = formatting;
		this.repositoryService = repositoryService;
		this.expressionEvaluationHelper = expressionEvaluationHelper;
	}

	@Override
	public String formatComment(@NotNull AbstractWorkItemType workItem, Task task, OperationResult result) {
		return formatComment(workItem.getPerformerRef(), workItem.getOutput(), workItem, null, task, result);
	}

	@Override
	public String formatComment(@NotNull WorkItemCompletionEventType event, Task task, OperationResult result) {
		return formatComment(event.getInitiatorRef(), event.getOutput(), null, event, task, result);
	}

	private String formatComment(ObjectReferenceType performerRef, AbstractWorkItemOutputType output, AbstractWorkItemType workItem,
			WorkItemCompletionEventType event, Task task, OperationResult result) {
		if (formatting == null || formatting.getCondition() == null && formatting.getValue() == null) {
			return output.getComment();
		}
		ObjectType performer = getPerformer(performerRef, result);
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_PERFORMER, performer);
		variables.addVariableDefinition(ExpressionConstants.VAR_OUTPUT, output);
		variables.addVariableDefinition(ExpressionConstants.VAR_WORK_ITEM, workItem);
		variables.addVariableDefinition(ExpressionConstants.VAR_EVENT, event);
		if (formatting.getCondition() != null) {
			try {
				boolean condition = expressionEvaluationHelper.evaluateBooleanExpression(formatting.getCondition(), variables,
						"condition in performer comments formatter", task, result);
				if (!condition) {
					return null;
				}
			} catch (CommonException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate condition expression in comments formatter - continuing as if it were true; "
						+ "performer = {}, output = {}, workItem = {}, event = {}", e, performer, output, workItem, event);
			}
		}
		if (formatting.getValue() != null) {
			try {
				return expressionEvaluationHelper.evaluateStringExpression(formatting.getValue(), variables,
						"value in performer comments formatter", task, result);
			} catch (CommonException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate value expression in comments formatter - using plain comment value; "
						+ "performer = {}, output = {}, workItem = {}, event = {}", e, performer, output, workItem, event);
				return output.getComment();
			}
		} else {
			return output.getComment();
		}
	}

	@Nullable
	private ObjectType getPerformer(ObjectReferenceType performerRef, OperationResult result) {
		if (performerRef == null || performerRef.getOid() == null) {
			return null;
		}
		String performerOid = performerRef.getOid();
		if (performersCache.containsKey(performerOid)) {
			return performersCache.get(performerOid); // potentially null (if performer was previously searched for but not found)
		}
		UserType performer;
		try {
			performer = repositoryService.getObject(UserType.class, performerOid, null, result).asObjectable(); // TODO other types when needed
		} catch (ObjectNotFoundException | SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve performer {}", e, ObjectTypeUtil.toShortString(performerRef));
			performer = null;
		}
		performersCache.put(performerOid, performer);
		return performer;
	}
}
