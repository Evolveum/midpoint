/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script.groovy;


import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.syntax.SyntaxException;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import groovy.lang.Binding;
import groovy.lang.GString;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.CompileStatic;

/**
 * Expression evaluator that is using Groovy scripting engine.
 *
 * @author Radovan Semancik
 * "Sandboxing" based on type checking inspired by work of Cédric Champeau (http://melix.github.io/blog/2015/03/sandboxing.html)
 */
public class GroovyScriptEvaluator extends AbstractCachingScriptEvaluator<GroovyClassLoader, Class<?>> {

    public static final String LANGUAGE_NAME = "Groovy";
    public static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    static final String SANDBOX_ERROR_PREFIX = "[SANDBOX] ";

    /**
     * The name is not really used for anything serious. Maybe just for diagnostics.
     * But setting it to non-null to avoid confusing with the "null" profile.
     */
    public static final String BUILTIN_EXPRESSION_PROFILE_NAME = "_groovyBuiltIn";

    /**
     * Expression profile for built-in groovy functions that always needs to be allowed
     * or denied.
     */
    private static final ScriptExpressionProfile BUILTIN_SCRIPT_EXPRESSION_PROFILE = new ScriptExpressionProfile(BUILTIN_EXPRESSION_PROFILE_NAME);

    public GroovyScriptEvaluator(PrismContext prismContext, Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);

        // No initialization here. Compilers/interpreters are initialized on demand.
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageName()
     */
    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageUrl()
     */
    @Override
    public String getLanguageUrl() {
        return LANGUAGE_URL;
    }

    @Override
    protected void checkRestrictions(ScriptExpressionEvaluationContext context) throws SecurityViolationException {
        ScriptExpressionProfile scriptExpressionProfile = context.getScriptExpressionProfile();
        if (scriptExpressionProfile == null) {
            // no restrictions
            return;
        }
        if (!scriptExpressionProfile.hasRestrictions() && scriptExpressionProfile.getDecision() != AccessDecision.ALLOW) {
            throw new SecurityViolationException("Script intepreter for language "+getLanguageName()
            +" is not allowed in expression profile "+context.getExpressionProfile().getIdentifier()
            +"; script execution prohibited in "+context.getContextDescription());
        }
    }

    @Override
    protected Class<?> compileScript(String codeString, ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException, SecurityViolationException {
        try {
            return getGroovyLoader(context).parseClass(codeString, context.getContextDescription());
        } catch (MultipleCompilationErrorsException e) {
            String sandboxErrorMessage = getSandboxError(e);
            if (sandboxErrorMessage == null) {
                throw new ExpressionEvaluationException("Compilation error in " + context.getContextDescription() + ": " + e.getMessage(), e);
            } else {
                throw new SecurityViolationException("Denied access to functionality of script " + context.getContextDescription() + ": "+sandboxErrorMessage, e);
            }
        } catch (Throwable e) {
            throw new ExpressionEvaluationException("Unexpected error during compilation of script in " + context.getContextDescription() + ": " + e.getMessage(), e);
        }
    }


    private GroovyClassLoader getGroovyLoader(ScriptExpressionEvaluationContext context) throws SecurityViolationException {
        GroovyClassLoader groovyClassLoader = getScriptCache().getInterpreter(context.getExpressionProfile());
        if (groovyClassLoader != null) {
            return groovyClassLoader;
        }
        ScriptExpressionProfile scriptExpressionProfile = context.getScriptExpressionProfile();
        groovyClassLoader = createGroovyLoader(scriptExpressionProfile, context);
        getScriptCache().putInterpreter(context.getExpressionProfile(), groovyClassLoader);
        return groovyClassLoader;
    }

    private GroovyClassLoader createGroovyLoader(ScriptExpressionProfile expressionProfile, ScriptExpressionEvaluationContext context) throws SecurityViolationException {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
        configureCompiler(compilerConfiguration, expressionProfile, context);
        return new GroovyClassLoader(GroovyScriptEvaluator.class.getClassLoader(), compilerConfiguration);
    }

    private void configureCompiler(CompilerConfiguration compilerConfiguration, ScriptExpressionProfile scriptExpressionProfile, ScriptExpressionEvaluationContext context) throws SecurityViolationException {
        if (scriptExpressionProfile == null) {
            // No configuration is needed for "almighty" compiler.
            return;
        }
        if (!BooleanUtils.isTrue(scriptExpressionProfile.isTypeChecking())) {
            if (scriptExpressionProfile.hasRestrictions()) {
                throw new SecurityViolationException("Requested to apply restrictions to groovy script, but the script is not set to type checking mode, in "+context.getContextDescription());
            }
            return;
        }

        SecureASTCustomizer sAstCustomizer = new SecureASTCustomizer();
        compilerConfiguration.addCompilationCustomizers(sAstCustomizer);

        ASTTransformationCustomizer astTransCustomizer = new ASTTransformationCustomizer(
                Collections.singletonMap("extensions", Collections.singletonList(SandboxTypeCheckingExtension.class.getName())),
                CompileStatic.class);
        compilerConfiguration.addCompilationCustomizers(astTransCustomizer);
    }

    private String getSandboxError(MultipleCompilationErrorsException e) {
        List<?> errors = e.getErrorCollector().getErrors();
        if (errors == null) {
            return null;
        }
        for (Object error : errors) {
            if (!(error instanceof SyntaxErrorMessage)) {
                continue;
            }
            SyntaxException cause = ((SyntaxErrorMessage)error).getCause();

            if (cause == null) {
                continue;
            }

            String causeMessage = cause.getMessage();
            if (causeMessage == null) {
                continue;
            }
            int i = causeMessage.indexOf(SANDBOX_ERROR_PREFIX);
            if (i < 0) {
                continue;
            }
            return causeMessage.substring(i + SANDBOX_ERROR_PREFIX.length());
        }
        return null;
    }

    @Override
    protected Object evaluateScript(Class<?> compiledScriptClass, ScriptExpressionEvaluationContext context) throws Exception {

        if (!Script.class.isAssignableFrom(compiledScriptClass)) {
            throw new ExpressionEvaluationException("Expected groovy script class, but got "+compiledScriptClass);
        }

        Binding binding = new Binding(prepareScriptVariablesValueMap(context));

        Script scriptResultObject = InvokerHelper.createScript(compiledScriptClass, binding);

        Object resultObject = scriptResultObject.run();

        if (resultObject == null) {
            return null;
        }

        if (resultObject instanceof GString) {
            resultObject = ((GString)resultObject).toString();
        }

        return resultObject;
    }

    static AccessDecision decideGroovyBuiltin(String className, String methodName) {
        return BUILTIN_SCRIPT_EXPRESSION_PROFILE.decideClassAccess(className, methodName);
    }

    static {
        ExpressionPermissionProfile permissionProfile = new ExpressionPermissionProfile(BUILTIN_EXPRESSION_PROFILE_NAME);

        // Allow script initialization
        permissionProfile.addClassAccessRule(Script.class, "<init>", AccessDecision.ALLOW);
        permissionProfile.addClassAccessRule(InvokerHelper.class, "runScript", AccessDecision.ALLOW);

        // Deny access to reflection. Reflection can circumvent the sandbox protection.
        permissionProfile.addClassAccessRule(Class.class, null, AccessDecision.DENY);
        permissionProfile.addClassAccessRule(Method.class, null, AccessDecision.DENY);

        permissionProfile.setDecision(AccessDecision.DEFAULT);

        BUILTIN_SCRIPT_EXPRESSION_PROFILE.setPermissionProfile(permissionProfile);
        BUILTIN_SCRIPT_EXPRESSION_PROFILE.setDecision(AccessDecision.DEFAULT);
    }

}
