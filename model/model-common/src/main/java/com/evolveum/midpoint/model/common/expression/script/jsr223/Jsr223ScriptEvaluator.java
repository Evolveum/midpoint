/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script.jsr223;

import javax.script.*;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.script.groovy.GroovyScriptEvaluator;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Generic expression evaluator that is using javax.script (JSR-223) engine.
 *
 * This evaluator does not really support expression profiles. It has just one global almighty compiler ({@link ScriptEngine}).
 * Groovy is handled by {@link GroovyScriptEvaluator}.
 *
 * @author Radovan Semancik
 */
public class Jsr223ScriptEvaluator extends AbstractCachingScriptEvaluator<ScriptEngine, CompiledScript> {

    private static final Trace LOGGER = TraceManager.getTrace(Jsr223ScriptEvaluator.class);

    private final ScriptEngine scriptEngine;
    private final String engineName;

    public Jsr223ScriptEvaluator(String engineName, PrismContext prismContext,
            Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);

        this.engineName = engineName;
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        long initStartMs = System.currentTimeMillis();
        scriptEngine = scriptEngineManager.getEngineByName(engineName);
        if (scriptEngine == null) {
            LOGGER.warn("The JSR-223 scripting engine for '{}' was not found", engineName);
            return;
        }
        LOGGER.info("Script engine for '{}' initialized in {} ms.",
                engineName, System.currentTimeMillis() - initStartMs);
    }

    @Override
    protected CompiledScript compileScript(String codeString, ScriptExpressionEvaluationContext evaluationContext)
            throws Exception {
        return ((Compilable) scriptEngine).compile(codeString);
    }

    @Override
    protected Object evaluateScript(CompiledScript compiledScript, ScriptExpressionEvaluationContext context) throws Exception {
        Bindings bindings = convertToBindings(context);
        return compiledScript.eval(bindings);
    }

    private Bindings convertToBindings(ScriptExpressionEvaluationContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Bindings bindings = scriptEngine.createBindings();
        bindings.putAll(prepareScriptVariablesValueMap(context));
        return bindings;
    }

    @Override
    public String getLanguageName() {
        if (scriptEngine != null) {
            return scriptEngine.getFactory().getLanguageName();
        }
        return engineName;
    }

    @Override
    public @NotNull String getLanguageUrl() {
        return MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + getLanguageName();
    }

    @Override
    public boolean isInitialized() {
        return scriptEngine != null;
    }
}
