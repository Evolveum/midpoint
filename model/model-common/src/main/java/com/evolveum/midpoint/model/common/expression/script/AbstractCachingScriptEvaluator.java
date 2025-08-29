/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import org.jetbrains.annotations.Nullable;

/**
 * Script evaluator that caches compiled scripts in {@link #scriptCache}.
 *
 * @param <I> script interpreter/compiler
 * @param <C> compiled code
 *
 * @author Radovan Semancik
 */
public abstract class AbstractCachingScriptEvaluator<I, C> extends AbstractScriptEvaluator {

    @NotNull private final ScriptCache<I, C> scriptCache;

    public AbstractCachingScriptEvaluator(
            PrismContext prismContext, Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);
        this.scriptCache = new ScriptCache<>();
    }

    protected @NotNull ScriptCache<I, C> getScriptCache() {
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
            throws ExpressionEvaluationException, ExpressionSyntaxException, SecurityViolationException {
        C cachedCompiledScript = scriptCache.getCode(context.getExpressionProfile(), codeString);
        if (cachedCompiledScript != null) {
            return cachedCompiledScript;
        }
        InternalMonitor.recordCount(InternalCounters.SCRIPT_COMPILE_COUNT);
        C compiledScript;
        try {
            compiledScript = compileScript(codeString, context);
        } catch (ExpressionEvaluationException | ExpressionSyntaxException | SecurityViolationException e) {
            throw e;
        } catch (Exception e) {
            throw new ExpressionEvaluationException(e.getMessage() + " while compiling " + context.getContextDescription(), e);
        }
        scriptCache.putCode(context.getExpressionProfile(), codeString, compiledScript);
        return compiledScript;
    }

    protected abstract C compileScript(String codeString, ScriptExpressionEvaluationContext context) throws Exception;

    protected abstract Object evaluateScript(C compiledScript, ScriptExpressionEvaluationContext context)
            throws Exception;
}
