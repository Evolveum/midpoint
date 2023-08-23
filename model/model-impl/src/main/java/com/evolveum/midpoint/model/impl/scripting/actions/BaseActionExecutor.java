/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import static com.evolveum.midpoint.model.impl.scripting.VariablesUtil.cloneIfNecessary;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.statistics.Operation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.scripting.*;
import com.evolveum.midpoint.model.impl.scripting.helpers.ExpressionHelper;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Superclass for all action executors.
 */
public abstract class BaseActionExecutor implements ActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(BaseActionExecutor.class);

    @Autowired protected BulkActionsExecutor bulkActionsExecutor;
    @Autowired protected PrismContext prismContext;
    @Autowired protected OperationsHelper operationsHelper;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected ExpressionHelper expressionHelper;
    @Autowired protected ProvisioningService provisioningService;
    @Autowired protected ModelService modelService;
    @Autowired protected SecurityEnforcer securityEnforcer;
    @Autowired protected SecurityContextManager securityContextManager;
    @Autowired protected TaskService taskService;
    @Autowired @Qualifier("cacheRepositoryService") protected RepositoryService cacheRepositoryService;
    @Autowired protected ScriptingActionExecutorRegistry actionExecutorRegistry;
    @Autowired protected MidpointFunctions midpointFunctions;
    @Autowired protected RelationRegistry relationRegistry;
    @Autowired protected MatchingRuleRegistry matchingRuleRegistry;
    @Autowired protected SchemaService schemaService;

    /**
     * Returns the name used to invoke this action in a dynamic way, e.g. `execute-script`, `generate-value`, etc.
     *
     * TODO should we really call this "legacy"? Not all actions have their "modern" names.
     * */
    abstract @NotNull String getLegacyActionName();

    /**
     * Returns the name used to invoke this action in a static way, e.g. `execute`, `generateValue`, etc.
     *
     * Not all actions have such a name; e.g. `reencrypt` has not.
     */
    abstract @Nullable String getConfigurationElementName();

    private String optionsSuffix(ModelExecuteOptions options) {
        return options.notEmpty() ? " " + options : "";
    }

    String drySuffix(boolean dry) {
        return dry ? " (dry run)" : "";
    }

    String optionsSuffix(ModelExecuteOptions options, boolean dry) {
        return optionsSuffix(options) + drySuffix(dry);
    }

    protected String exceptionSuffix(Throwable t) {
        return t != null ? " (error: " + t.getClass().getSimpleName() + ": " + t.getMessage() + ")" : "";
    }

    Throwable processActionException(Throwable e, String actionName, PrismValue value, ExecutionContext context)
            throws ScriptExecutionException {
        if (context.isContinueOnAnyError()) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute action '{}' on {}: {}", e,
                    actionName, value, e.getMessage());
            return e;
        } else {
            throw new ScriptExecutionException(
                    "Couldn't execute action '" + actionName + "' on " + value + ": " + e.getMessage(),
                    e);
        }
    }

    @FunctionalInterface
    public interface ItemProcessor {
        void process(PrismValue value, PipelineItem item, OperationResult result) throws CommonException;
    }

    @FunctionalInterface
    public interface ConsoleFailureMessageWriter {
        void write(PrismValue value, @NotNull Throwable exception);
    }

    void iterateOverItems(PipelineData input, ExecutionContext context, OperationResult globalResult,
            ItemProcessor itemProcessor, ConsoleFailureMessageWriter writer)
            throws ScriptExecutionException {

        for (PipelineItem item : input.getData()) {
            PrismValue value = item.getValue();

            context.checkTaskStop();
            Operation op;
            if (value instanceof PrismObjectValue) {
                op = operationsHelper.recordStart(context, asObjectType(value));
            } else {
                op = null;
            }

            OperationResult result = operationsHelper.createActionResult(item, this, globalResult);
            try {
                itemProcessor.process(value, item, result);
                operationsHelper.recordEnd(context, op, null, result);
            } catch (Throwable ex) {
                result.recordFatalError(ex);
                operationsHelper.recordEnd(context, op, ex, result);
                Throwable exception = processActionException(ex, getLegacyActionName(), value, context);
                writer.write(value, exception);
            } finally {
                result.close();
            }
            operationsHelper.trimAndCloneResult(result, item.getResult());
        }
    }

    private ObjectType asObjectType(PrismValue value) {
        return (ObjectType) ((PrismObjectValue<?>) value).asObjectable();
    }

    String getDescription(PrismValue value) {
        if (value instanceof PrismObjectValue<?>) {
            return asObjectType(value).asPrismObject().toString();
        } else {
            return value.toHumanReadableString();
        }
    }

    /**
     * Creates variables for script evaluation based on some externally-supplied variables,
     * plus some generic ones (prism context, actor).
     */
    @NotNull VariablesMap createVariables(VariablesMap externalVariables) {
        VariablesMap variables = new VariablesMap();

        variables.put(ExpressionConstants.VAR_PRISM_CONTEXT, prismContext, PrismContext.class);
        ExpressionUtil.addActorVariableIfNeeded(variables, securityContextManager);

        externalVariables.forEach((k, v) -> variables.put(k, cloneIfNecessary(k, v)));
        variables.registerAliasesFrom(externalVariables);

        return variables;
    }

    @Override
    public void checkExecutionAllowed(ExecutionContext context) throws SecurityViolationException {

        var expressionProfile = context.getExpressionProfile();
        var scriptingProfile = expressionProfile.getScriptingProfile();

        String legacyName = getLegacyActionName();
        String modernName = getConfigurationElementName();
        var decision = scriptingProfile.decideActionAccess(legacyName, modernName);
        var names = modernName != null ?
                "'%s' ('%s')".formatted(legacyName, modernName) :
                "'%s'".formatted(legacyName);

        if (decision != AccessDecision.ALLOW) {
            throw new SecurityViolationException(
                    "Access to action %s %s (applied expression profile '%s', actions profile '%s')"
                            .formatted(
                                    names,
                                    decision == AccessDecision.DENY ? "denied" : "not allowed",
                                    expressionProfile.getIdentifier(),
                                    scriptingProfile.getIdentifier()));
        }
    }
}
