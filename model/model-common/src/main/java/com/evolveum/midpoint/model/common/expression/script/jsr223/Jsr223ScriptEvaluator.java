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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.exception.*;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 * <p>
 * This evaluator does not really support expression profiles. It has just one
 * global almighty compiler (ScriptEngine).
 *
 * @author Radovan Semancik
 */
public class Jsr223ScriptEvaluator extends AbstractCachingScriptEvaluator<ScriptEngine, CompiledScript> {

    private final ScriptEngine scriptEngine;

    public Jsr223ScriptEvaluator(String engineName, PrismContext prismContext,
            Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        scriptEngine = scriptEngineManager.getEngineByName(engineName);
        if (scriptEngine == null) {
            throw new SystemException("The JSR-223 scripting engine for '" + engineName + "' was not found");
        }
    }

    @Override
    protected CompiledScript compileScript(String codeString, ScriptExpressionEvaluationContext context) throws Exception {
        return ((Compilable) scriptEngine).compile(codeString);
    }

    @Override
    protected Object evaluateScript(CompiledScript compiledScript, ScriptExpressionEvaluationContext context) throws Exception {

        Bindings bindings = convertToBindings(context);
        return compiledScript.eval(bindings);
    }

    private Bindings convertToBindings(ScriptExpressionEvaluationContext context)
            throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Bindings bindings = scriptEngine.createBindings();
        bindings.putAll(prepareScriptVariablesValueMap(context));
        return bindings;
    }

//    public <T> Object evaluateReportScript(String codeString, ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException,
//            ObjectNotFoundException, ExpressionSyntaxException, CommunicationException, ConfigurationException, SecurityViolationException {
//
//        Bindings bindings = convertToBindings(context);
//
////        String codeString = code;
//        if (codeString == null) {
//            throw new ExpressionEvaluationException("No script code in " + context.getContextDescription());
//        }
//
//        boolean allowEmptyValues = true;
////        if (expressionType.isAllowEmptyValues() != null) {
////            allowEmptyValues = expressionType.isAllowEmptyValues();
////        }
//
//        CompiledScript compiledScript = getCompiledScript(codeString, context);
//
//        Object evalRawResult;
//        try {
//            InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
//            evalRawResult = compiledScript.eval(bindings);
//        } catch (Throwable e) {
//            throw new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(), e);
//        }
//
//
//
//        return evalRawResult;
//    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageName()
     */
    @Override
    public String getLanguageName() {
        return scriptEngine.getFactory().getLanguageName();
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageUrl()
     */
    @Override
    public String getLanguageUrl() {
        return MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + getLanguageName();
    }

}
