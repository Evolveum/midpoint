/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import groovy.lang.GroovyClassLoader;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;

import org.jetbrains.annotations.Nullable;

/**
 * Script evaluator that caches compiled scripts in {@link #scriptCache}.
 *
 * @param <I> script interpreter/compiler/runtime
 * @param <C> compiled code
 * @param <K> code caching key (e.g. source code)
 *
 * @author Radovan Semancik
 */
public abstract class AbstractCachingScriptEvaluator<I, C, K> extends AbstractScriptEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCachingScriptEvaluator.class);

    @NotNull private final ScriptCache<I, C, K> scriptCache;

    public AbstractCachingScriptEvaluator(
            PrismContext prismContext, Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);
        this.scriptCache = new ScriptCache<>();
    }

    protected @NotNull ScriptCache<I, C, K> getScriptCache() {
        return scriptCache;
    }

    @Override
    public @Nullable Object evaluateInternal(
            @NotNull String codeString, @NotNull ScriptExpressionEvaluationContext context)
            throws Exception {

        C compiledScript = getCompiledScript(codeString, context);

        InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
        return evaluateScript(compiledScript, context);
    }

    private C getCompiledScript(String codeString, ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, SecurityViolationException, SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException {
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

    protected I getInterpreter(ScriptExpressionEvaluationContext context) throws SecurityViolationException, ConfigurationException {
        I existingInterpreter = getScriptCache().getInterpreter(context.getExpressionProfile());
        if (existingInterpreter != null) {
            return existingInterpreter;
        }
        var newInterpreter = createInterpreter(context);
        getScriptCache().putInterpreter(context.getExpressionProfile(), newInterpreter);
        return newInterpreter;
    }

    protected abstract I createInterpreter(ScriptExpressionEvaluationContext context) throws SecurityViolationException, ConfigurationException;

    protected abstract K getScriptCachingKey(String codeString, ScriptExpressionEvaluationContext context) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException;

    protected abstract C compileScript(String codeString, ScriptExpressionEvaluationContext context) throws Exception;

    protected abstract Object evaluateScript(C compiledScript, ScriptExpressionEvaluationContext context)
            throws Exception;
}
