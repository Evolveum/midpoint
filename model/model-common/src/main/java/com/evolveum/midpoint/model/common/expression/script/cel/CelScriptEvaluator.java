/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.script.cel.value.PolyStringCelValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import dev.cel.common.*;
import dev.cel.common.types.CelTypeProvider;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.CelValue;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.extensions.CelExtensions;
import dev.cel.parser.CelStandardMacro;
import dev.cel.parser.Operator;
import dev.cel.runtime.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Expression evaluator that is using Common Expression Language (CEL).
 */
public class CelScriptEvaluator extends AbstractScriptEvaluator {
// TODO: proper caching
//    public class CelScriptEvaluator extends AbstractCachingScriptEvaluator<CelRuntime, CelAbstractSyntaxTree> {


    private static final Trace LOGGER = TraceManager.getTrace(CelScriptEvaluator.class);

    public static final String LANGUAGE_NAME = "CEL";
    private static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    private CelTypeProvider typeProvider = null;

    private CelOptions celOptions = CelOptions.DEFAULT;

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

    // TODO: Temporary
    @Override
    public @Nullable Object evaluateInternal(
            @NotNull String codeString, @NotNull ScriptExpressionEvaluationContext context)
            throws Exception {

        CelAbstractSyntaxTree compiledScript = compileScript(codeString, context);

        InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
        return evaluateScript(compiledScript, context);
    }

//    @Override
    protected CelAbstractSyntaxTree compileScript(String codeString, ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, SecurityViolationException {
        try {
            CelValidationResult validationResult = getCompiler(context).compile(codeString, context.getContextDescription());
            // TODO: validationResult.hasError()
            return validationResult.getAst();
        } catch (Throwable e) { // TODO: CelValidationException
            throw new ExpressionEvaluationException(
                    "Unexpected error during compilation of script in %s: %s".formatted(
                            context.getContextDescription(), e.getMessage()),
                    serializationSafeThrowable(e));
        }
    }

    private CelCompiler getCompiler(ScriptExpressionEvaluationContext context) throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        // TODO: caching
        return createCompiler(context);
    }

    private CelCompiler createCompiler(ScriptExpressionEvaluationContext context) throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder();
        builder.setOptions(celOptions);
        builder.setStandardMacros(CelStandardMacro.HAS);
        builder.setTypeProvider(getTypeProvider());
        builder.addLibraries(
                CelExtensions.strings(),
                CelExtensions.bindings(),
                CelExtensions.math(celOptions),
                CelExtensions.encoders(celOptions),
                CelExtensions.sets(celOptions),
                CelExtensions.lists(),
                CelExtensions.regex(),
                CelExtensions.comprehensions(),
                CelExtensions.optional());
        PolyStringCelValue.addCompilerDeclarations(builder, context);
        // TODO: Further compiler config
        new CelFunctionLibraryMapper(context).compilerAddFunctionLibraryDeclarations(builder);
        for (var varEntry : prepareScriptVariablesTypedValueMap(context).entrySet()) {
            builder.addVar(varEntry.getKey(), CelTypeMapper.toCelType(varEntry.getValue()));
        }
        return builder.build();
    }

    private CelTypeProvider getTypeProvider() {
        if (typeProvider == null) {
            typeProvider = createTypeProvider();
        }
        return typeProvider;
    }

    private CelTypeProvider createTypeProvider() {
        MidPointTypeProvider midPointTypeProvider = new MidPointTypeProvider(getPrismContext());
        return midPointTypeProvider;
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


//    @Override
    protected Object evaluateScript(CelAbstractSyntaxTree compiledScript, ScriptExpressionEvaluationContext context) throws Exception {

        CelRuntime runtime = getRuntime(context);
        CelRuntime.Program program = runtime.createProgram(compiledScript);

        Map<String, ?> variables = prepareVariablesValueMap(context);
        Object resultObject = program.eval(variables);
        if (resultObject instanceof CelUnknownSet) {
            // This means error
            throw new ExpressionEvaluationException("CEL expression evaluation error: "+resultObject);
        }
        if (resultObject instanceof CelValue) {
            resultObject = CelTypeMapper.toJavaValue((CelValue) resultObject);
        }
        return resultObject;
    }

    private CelRuntime getRuntime(ScriptExpressionEvaluationContext context) {
        return createRuntime(context);
    }

    private CelRuntime createRuntime(ScriptExpressionEvaluationContext context) {
        // TODO: consider expression profiles?
        // TODO: caching
        CelRuntimeBuilder builder = CelRuntimeFactory.standardCelRuntimeBuilder();
        builder.setOptions(celOptions);
        builder.addLibraries(
                CelExtensions.strings(),
                CelExtensions.math(celOptions),
                CelExtensions.encoders(celOptions),
                CelExtensions.sets(celOptions),
                CelExtensions.lists(),
                CelExtensions.regex(),
                CelExtensions.comprehensions(),
                CelExtensions.optional());
        new CelFunctionLibraryMapper(context).runtimeAddFunctionLibraryDeclarations(builder);
        PolyStringCelValue.addRuntimeDeclarations(builder, context);
        return builder.build();
    }

    private Map<String, ?> prepareVariablesValueMap(ScriptExpressionEvaluationContext context) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        final Map<String, Object> scriptVariableMap = new HashMap<>();
        prepareScriptVariablesMap(context, scriptVariableMap, CelTypeMapper::convertVariableValue);
        return scriptVariableMap;
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
