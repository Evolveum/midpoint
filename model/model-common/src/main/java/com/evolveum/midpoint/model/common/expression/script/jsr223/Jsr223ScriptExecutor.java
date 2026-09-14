/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.jsr223;

import javax.script.*;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptExecutor;
import com.evolveum.midpoint.model.common.expression.script.ScriptExecutionContext;
import com.evolveum.midpoint.model.common.expression.script.groovy.GroovyScriptExecutor;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Generic script executor that is using javax.script (JSR-223) engine.
 *
 * This executor does not really support expression profiles. It has just one global almighty compiler ({@link ScriptEngine}).
 * Groovy (which supports profiles) is handled by {@link GroovyScriptExecutor}.
 *
 * @author Radovan Semancik
 */
public class Jsr223ScriptExecutor extends AbstractCachingScriptExecutor<ScriptEngine, CompiledScript, String> {

    private static final Trace LOGGER = TraceManager.getTrace(Jsr223ScriptExecutor.class);

    private final ScriptEngine scriptEngine;
    private final String engineName;

    public Jsr223ScriptExecutor(
            String engineName,
            PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            ExpressionsConfigurationSection configuration) {
        super(prismContext, protector, localizationService, configuration);

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

    // Not really used, but required by interface contract
    @Override
    protected ScriptEngine createInterpreter(ScriptExecutionContext context) throws SecurityViolationException, ConfigurationException {
        return scriptEngine;
    }

    @Override
    protected String getScriptCachingKey(String codeString, ScriptExecutionContext context) {
        return codeString;
    }

    @Override
    protected CompiledScript compileScript(String codeString, ScriptExecutionContext evaluationContext)
            throws Exception {
        return ((Compilable) scriptEngine).compile(codeString);
    }

    @Override
    protected Object executeScript(CompiledScript compiledScript, ScriptExecutionContext context) throws Exception {
        Bindings bindings = convertToBindings(context);
        return compiledScript.eval(bindings);
    }

    private Bindings convertToBindings(ScriptExecutionContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, SubscriptionComplianceException {
        Bindings bindings = scriptEngine.createBindings();
        bindings.putAll(prepareUnifiedScriptVariablesValueMap(context));
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
