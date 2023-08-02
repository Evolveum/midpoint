/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.helpers;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.cases.api.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class PerformerCommentsFormatterImpl implements PerformerCommentsFormatter {

    private static final Trace LOGGER = TraceManager.getTrace(PerformerCommentsFormatterImpl.class);

    @Nullable private final PerformerCommentsFormattingType formatting;
    @NotNull private final RepositoryService repositoryService;
    @NotNull private final CaseExpressionEvaluationHelper expressionEvaluationHelper;

    private final Map<String, ObjectType> performersCache = new HashMap<>();

    public PerformerCommentsFormatterImpl(
            @Nullable PerformerCommentsFormattingType formatting,
            @NotNull RepositoryService repositoryService,
            @NotNull CaseExpressionEvaluationHelper expressionEvaluationHelper) {
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

    private String formatComment(
            ObjectReferenceType performerRef, AbstractWorkItemOutputType output, AbstractWorkItemType workItem,
            WorkItemCompletionEventType event, Task task, OperationResult result) {
        if (formatting == null || formatting.getCondition() == null && formatting.getValue() == null) {
            return output.getComment();
        }
        ObjectType performer = getPerformer(performerRef, result);
        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_PERFORMER, performer, performer.asPrismObject().getDefinition());
        variables.put(ExpressionConstants.VAR_OUTPUT, output, AbstractWorkItemOutputType.class);
        variables.put(ExpressionConstants.VAR_WORK_ITEM, workItem, AbstractWorkItemType.class);
        variables.put(ExpressionConstants.VAR_EVENT, event, WorkItemCompletionEventType.class);
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
