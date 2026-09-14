/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;

import org.jetbrains.annotations.Nullable;

/**
 * Script executor that caches compiled scripts in {@link #scriptCache}.
 *
 * @param <I> script interpreter/compiler/runtime
 * @param <C> compiled code
 * @param <K> code caching key (e.g. source code)
 *
 * @author Radovan Semancik
 */
public abstract class AbstractCachingScriptExecutor<I, C, K> extends AbstractScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCachingScriptExecutor.class);

    @NotNull private final ScriptCache<I, C, K> scriptCache;

    public AbstractCachingScriptExecutor(
            PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            ExpressionsConfigurationSection configuration) {
        super(prismContext, protector, localizationService, configuration);
        this.scriptCache = new ScriptCache<>();
    }

    protected @NotNull ScriptCache<I, C, K> getScriptCache() {
        return scriptCache;
    }

    protected void clearScriptCache() {
        getScriptCache().clear();
    }

    @Override
    public @Nullable Object executeInternal(
            @NotNull String codeString, @NotNull ScriptExecutionContext context)
            throws Exception {

        C compiledScript = getCompiledScript(codeString, context);

        InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
        return executeScript(compiledScript, context);
    }

    private C getCompiledScript(String codeString, ScriptExecutionContext context)
            throws ExpressionEvaluationException, SecurityViolationException, SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
        K key = getScriptCachingKey(codeString, context);
        C cachedCompiledScript = scriptCache.getCode(context.getExpressionProfile(), key);
        if (cachedCompiledScript != null) {
            return cachedCompiledScript;
        }
        InternalMonitor.recordCount(InternalCounters.SCRIPT_COMPILE_COUNT);
        C compiledScript;
        try {
            compiledScript = compileScript(codeString, context);
        } catch (ExpressionEvaluationException | SecurityViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExpressionEvaluationException(e.getMessage() + " while compiling " + context.getContextDescription(), e);
        }
        scriptCache.putCode(context.getExpressionProfile(), key, compiledScript);
        return compiledScript;
    }

    protected I getInterpreter(ScriptExecutionContext context) throws SecurityViolationException, ConfigurationException {
        I existingInterpreter = getScriptCache().getInterpreter(context.getExpressionProfile());
        if (existingInterpreter != null) {
            return existingInterpreter;
        }
        var newInterpreter = createInterpreter(context);
        getScriptCache().putInterpreter(context.getExpressionProfile(), newInterpreter);
        return newInterpreter;
    }

    protected abstract I createInterpreter(ScriptExecutionContext context) throws SecurityViolationException, ConfigurationException;

    protected abstract K getScriptCachingKey(String codeString, ScriptExecutionContext context) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException;

    protected abstract C compileScript(String codeString, ScriptExecutionContext context) throws Exception;

    protected abstract Object executeScript(C compiledScript, ScriptExecutionContext context)
            throws Exception;
}
