/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Responsible for invoking hooks (both Java and scripting ones).
 */
@Component
public class ClockworkHookHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkHookHelper.class);

    @Autowired(required = false) private HookRegistry hookRegistry;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ScriptExpressionFactory scriptExpressionFactory;
    @Autowired private SystemObjectCache systemObjectCache;

    /**
     * Invokes hooks, if there are any.
     *
     * @return - ERROR, if any hook reported error; otherwise returns
     * - BACKGROUND, if any hook reported switching to background; otherwise
     * - FOREGROUND (if all hooks reported finishing on foreground)
     */
    HookOperationMode invokeHooks(LensContext<?> context, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        // TODO: following two parts should be merged together in later versions

        // Execute configured scripting hooks
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        // systemConfiguration may be null in some tests
        if (systemConfiguration != null) {
            ModelHooksType modelHooks = systemConfiguration.asObjectable().getModelHooks();
            if (modelHooks != null) {
                HookListType changeHooks = modelHooks.getChange();
                if (changeHooks != null) {
                    for (HookType hookType : changeHooks.getHook()) {
                        String shortDesc;
                        if (hookType.getName() != null) {
                            shortDesc = "hook '" + hookType.getName() + "'";
                        } else {
                            shortDesc = "scripting hook in system configuration";
                        }
                        if (hookType.isEnabled() != null && !hookType.isEnabled()) {
                            // Disabled hook, skip
                            continue;
                        }
                        if (hookType.getState() != null) {
                            if (!context.getState().toModelStateType().equals(hookType.getState())) {
                                continue;
                            }
                        }
                        if (hookType.getFocusType() != null) {
                            if (context.getFocusContext() == null) {
                                continue;
                            }
                            QName hookFocusTypeQname = hookType.getFocusType();
                            ObjectTypes hookFocusType = ObjectTypes.getObjectTypeFromTypeQName(hookFocusTypeQname);
                            if (hookFocusType == null) {
                                throw new SchemaException("Unknown focus type QName " + hookFocusTypeQname + " in " + shortDesc);
                            }
                            Class<?> focusClass = context.getFocusClass();
                            Class<? extends ObjectType> hookFocusClass = hookFocusType.getClassDefinition();
                            if (!hookFocusClass.isAssignableFrom(focusClass)) {
                                continue;
                            }
                        }

                        ScriptExpressionEvaluatorType scriptExpressionEvaluatorType = hookType.getScript();
                        if (scriptExpressionEvaluatorType == null) {
                            continue;
                        }
                        try {
                            evaluateScriptingHook(context, scriptExpressionEvaluatorType, shortDesc, task, result);
                        } catch (ExpressionEvaluationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new ExpressionEvaluationException("Evaluation of " + shortDesc + " failed: " + e.getMessage(), e);
                        } catch (ObjectNotFoundException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw e.wrap("Evaluation of " + shortDesc + " failed");
                        } catch (SchemaException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new SchemaException("Evaluation of " + shortDesc + " failed: " + e.getMessage(), e);
                        } catch (CommunicationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new CommunicationException("Evaluation of " + shortDesc + " failed: " + e.getMessage(), e);
                        } catch (ConfigurationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new ConfigurationException("Evaluation of " + shortDesc + " failed: " + e.getMessage(), e);
                        } catch (SecurityViolationException e) {
                            LOGGER.error("Evaluation of {} failed: {}", shortDesc, e.getMessage(), e);
                            throw new SecurityViolationException("Evaluation of " + shortDesc + " failed: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }

        // Execute registered Java hooks
        HookOperationMode resultMode = HookOperationMode.FOREGROUND;
        if (hookRegistry != null) {
            for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
                HookOperationMode mode = hook.invoke(context, task, result);
                if (mode == HookOperationMode.ERROR) {
                    resultMode = HookOperationMode.ERROR;
                } else if (mode == HookOperationMode.BACKGROUND) {
                    if (resultMode != HookOperationMode.ERROR) {
                        resultMode = HookOperationMode.BACKGROUND;
                    }
                }
            }
        }
        return resultMode;
    }

    private void evaluateScriptingHook(LensContext<?> context,
            ScriptExpressionEvaluatorType scriptExpressionEvaluatorType, String shortDesc, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {

        LOGGER.trace("Evaluating {}", shortDesc);
        // TODO: it would be nice to cache this
        // null output definition: this script has no output
        ScriptExpression scriptExpression = scriptExpressionFactory.createScriptExpression(
                scriptExpressionEvaluatorType, null,
                context.getPrivilegedExpressionProfile(), expressionFactory, shortDesc, result);

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_PRISM_CONTEXT, prismContext, PrismContext.class);
        variables.put(ExpressionConstants.VAR_MODEL_CONTEXT, context, ModelContext.class);
        LensFocusContext<?> focusContext = context.getFocusContext();
        if (focusContext != null) {
            variables.put(ExpressionConstants.VAR_FOCUS, focusContext.getObjectAny(), focusContext.getObjectDefinition());
        } else {
            variables.put(ExpressionConstants.VAR_FOCUS, null, FocusType.class);
        }

        ModelImplUtils.evaluateScript(scriptExpression, context, variables, false, shortDesc, task, result);
        LOGGER.trace("Finished evaluation of {}", shortDesc);
    }

    <F extends ObjectType> void invokePreview(LensContext<F> context, Task task, OperationResult result) {
        if (hookRegistry != null) {
            for (ChangeHook hook : hookRegistry.getAllChangeHooks()) {
                hook.invokePreview(context, task, result);
            }
        }
    }
}
