/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.script.groovy.SandboxTypeCheckingExtension;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionPermissionProfile;
import com.evolveum.midpoint.schema.expression.ScriptLanguageExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.exception.*;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.expr.Type;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;

import org.apache.commons.lang3.NotImplementedException;
import org.glassfish.jaxb.core.v2.TODO;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Expression evaluator that is using Common Expression Language (CEL).
 */
public class CelScriptEvaluator extends AbstractCachingScriptEvaluator<CelRuntime, CelAbstractSyntaxTree> {

    public static final String LANGUAGE_NAME = "CEL";
    private static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    /** Called by Spring but also by lower-level tests */
    public CelScriptEvaluator(PrismContext prismContext, Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);

        // No initialization here. Compilers/interpreters are initialized on demand.
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public @NotNull String getLanguageUrl() {
        return LANGUAGE_URL;
    }

    @Override
    protected boolean doesSupportRestrictions() {
        return true;
    }

    @Override
    protected CelAbstractSyntaxTree compileScript(String codeString, ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, SecurityViolationException {
        try {
            CelValidationResult validationResult = getCompiler(context).compile(codeString, context.getContextDescription());
            // TODO: validationResult.hasError()
            return validationResult.getAst();
        } catch (Throwable e) {
            throw new ExpressionEvaluationException(
                    "Unexpected error during compilation of script in %s: %s".formatted(
                            context.getContextDescription(), e.getMessage()),
                    serializationSafeThrowable(e));
        }
    }

    private CelCompiler getCompiler(ScriptExpressionEvaluationContext context) throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder();
        builder.setStandardMacros(CelStandardMacro.HAS);
        // TODO: Further compiler config
        for (var varEntry : prepareScriptVariablesTypedValueMap(context).entrySet()) {
            builder.addVar(varEntry.getKey(), convertToCelType(varEntry.getValue()));
        }
        return builder.build();
    }

    @NotNull
    private CelType convertToCelType(TypedValue<?> typedValue) {
        ItemDefinition<?> def = typedValue.getDefinition();
        if (def == null) {
            Class<?> typeClass = typedValue.getTypeClass();
            if (typeClass == null) {
                throw new IllegalStateException("Typed value " + typedValue + " does not have neither definition nor class");
            }
            return SimpleType.DYN;
//            throw new NotImplementedException("Cannot convert class "+typeClass.getSimpleName()+" to CEL");
            // TODO: convert based on class
        } else {
            if (def instanceof PrismPropertyDefinition<?>) {
                PrismPropertyDefinition<?> propDef = (PrismPropertyDefinition<?>)def;
                return CelTypeMapper.toCelType(propDef.getTypeName());
            }
            throw new NotImplementedException("Cannot convert "+def+" to CEL");
        }
    }

//    private GroovyClassLoader getGroovyLoader(ScriptExpressionEvaluationContext context) throws SecurityViolationException {
//        GroovyClassLoader existingLoader = getScriptCache().getInterpreter(context.getExpressionProfile());
//        if (existingLoader != null) {
//            return existingLoader;
//        }
//        var newLoader = createGroovyLoader(context);
//        getScriptCache().putInterpreter(context.getExpressionProfile(), newLoader);
//        return newLoader;
//    }


    @Override
    protected Object evaluateScript(CelAbstractSyntaxTree compiledScript, ScriptExpressionEvaluationContext context) throws Exception {

        CelRuntime runtime = getRuntime(context);
        CelRuntime.Program program = runtime.createProgram(compiledScript);

        Map<String, ?> variables = prepareScriptVariablesValueMap(context);
        Object resultObject = program.eval(variables);
        return resultObject;
    }

    private CelRuntime getRuntime(ScriptExpressionEvaluationContext context) {
        // TODO: consider expression profiles?
        return CelRuntimeFactory.standardCelRuntimeBuilder().build();
    }

    private static Throwable serializationSafeThrowable(Throwable e) {
//        if (e instanceof GroovyRuntimeException) {
//            Throwable cause = serializationSafeThrowable(e.getCause());
//            // We reconstruct hierarchy with GroovyRuntimeExceptions only (some subclasses contains fields which
//            // are not serializable)
//            GroovyRuntimeException ret = new GroovyRuntimeException(e.getMessage(), cause);
//            ret.setStackTrace(e.getStackTrace());
//        }
        // Let's assume other non-groovy exceptions are safe to serialize
        return e;
    }
}
