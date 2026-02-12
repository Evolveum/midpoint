/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.mel;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.script.mel.extension.MidPointCelExtensionManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import dev.cel.common.*;
import dev.cel.common.types.CelType;
import dev.cel.common.types.CelTypeProvider;
import dev.cel.common.types.ListType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Expression evaluator that is using MidPoint Expression Language (MEL).
 * MidPoint Expression Language (MEL) is based on Common Expression Language (CEL),
 * extended with midPoint-specific functionality.
 */
public class MelScriptEvaluator extends AbstractScriptEvaluator {
// TODO: proper caching
//    public class MelScriptEvaluator extends AbstractCachingScriptEvaluator<CelRuntime, CelAbstractSyntaxTree> {


    private static final Trace LOGGER = TraceManager.getTrace(MelScriptEvaluator.class);

    public static final String LANGUAGE_NAME = "mel";
    private static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    private final BasicExpressionFunctions basicExpressionFunctions;
    private final MidpointFunctions midpointExpressionFunctions;
    private final CelOptions celOptions = CelOptions.current().
            enableRegexPartialMatch(true).build();

    private CelTypeProvider typeProvider = null;

    private final MidPointCelExtensionManager midPointCelExtensionManager;
    private final FunctionLibraryProcessor functionLibraryProcessor;

    /** Called by Spring but also by lower-level tests */
    public MelScriptEvaluator(PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            BasicExpressionFunctions basicExpressionFunctions,
            MidpointFunctions midpointExpressionFunctions) {
        super(prismContext, protector, localizationService);
        this.basicExpressionFunctions = basicExpressionFunctions;
        this.midpointExpressionFunctions = midpointExpressionFunctions;
        midPointCelExtensionManager = new MidPointCelExtensionManager(protector,
                basicExpressionFunctions, midpointExpressionFunctions, celOptions);
        functionLibraryProcessor = new FunctionLibraryProcessor();

        // No compiler/interpreter initialization here. Compilers/interpreters are initialized on demand.
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
        CelValidationResult validationResult;
        try {
            validationResult = getCompiler(context).compile(codeString, context.getContextDescription());
        } catch (Throwable e) {
            throw new ExpressionEvaluationException(
                    "Unexpected error during compilation of script in %s: %s".formatted(
                            context.getContextDescription(), e.getMessage()), e);
        }
        if (validationResult.hasError()) {
            throw new ExpressionEvaluationException(
                    "Unexpected error during validation of script in %s: %s".formatted(
                            context.getContextDescription(), validationResult.getErrorString()));
        }
        try {
            return validationResult.getAst();
        } catch (CelValidationException e) {
            throw new ExpressionEvaluationException(
                    "Unexpected error during validation of script in %s: %s".formatted(
                            context.getContextDescription(), e.getMessage()), e);
        }
    }

    private CelCompiler getCompiler(ScriptExpressionEvaluationContext context) throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        // TODO: caching
        return createCompiler(context);
    }

    private CelCompiler createCompiler(ScriptExpressionEvaluationContext context) throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder();
        builder.setOptions(celOptions);
        builder.setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        builder.setTypeProvider(getTypeProvider());
        builder.addLibraries(midPointCelExtensionManager.allCompilerLibraries());
        // TODO: Further compiler config
//        new CelFunctionLibraryMapper(context).compilerAddFunctionLibraryDeclarations(builder);
        addCompilerVariables(builder, context);
        addFunctionLibraryDeclarations(builder, context);
        builder.setResultType(determineResultType(context));
        return builder.build();
    }

    private void addCompilerVariables(CelCompilerBuilder builder, ScriptExpressionEvaluationContext context) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        Map<String, TypedValue<?>> variables = prepareScriptVariablesTypedValueMap(context);
        for (var varEntry : variables.entrySet()) {
            builder.addVar(varEntry.getKey(), CelTypeMapper.toCelType(varEntry.getValue()));
        }
        if (!variables.containsKey(ExpressionConstants.VAR_NOW)) {
            builder.addVar(ExpressionConstants.VAR_NOW, SimpleType.TIMESTAMP);
        }
    }

    private void addFunctionLibraryDeclarations(CelCompilerBuilder builder, ScriptExpressionEvaluationContext context) throws ConfigurationException {
        for (FunctionLibraryBinding funcLibBinding : emptyIfNull(context.getFunctionLibraryBindings())) {
            if (funcLibBinding.getParsedLibrary() == null) {
                continue;
            }
            functionLibraryProcessor.addCompilerCustomLibraryDeclarations(builder, context, funcLibBinding);
        }
    }

    private CelTypeProvider getTypeProvider() {
        if (typeProvider == null) {
            typeProvider = createTypeProvider();
        }
        return typeProvider;
    }

    private CelTypeProvider createTypeProvider() {
        return new CelTypeMapper(getPrismContext());
    }

    private CelType determineResultType(ScriptExpressionEvaluationContext context) {
        CelType returnType = CelTypeMapper.toCelType(context.getOutputDefinition());
        if (isSingleScalarResult(context)) {
            return returnType;
        } else {
            return ListType.create(returnType);
        }
    }

    private boolean isSingleScalarResult(ScriptExpressionEvaluationContext context) {
        if (context.getSuggestedReturnType() == null) {
            return context.getOutputDefinition().isSingleValue();
        } else {
            return context.getSuggestedReturnType() == ScriptExpressionReturnTypeType.SCALAR;
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


//    @Override
    protected Object evaluateScript(CelAbstractSyntaxTree compiledScript, ScriptExpressionEvaluationContext context) throws Exception {

        CelRuntime runtime = getRuntime(context);
        CelRuntime.Program program = runtime.createProgram(compiledScript);

        Map<String, ?> variables = prepareVariablesValueMap(context);
        Object resultObject;
        try {
            resultObject = program.eval(variables);
        } catch (CelException e) {
            throw processCelException(e);
        }
        if (resultObject instanceof CelUnknownSet) {
            // This means error
            throw new ExpressionEvaluationException("CEL expression evaluation error: "+resultObject);
        }
        return CelTypeMapper.toJavaValue(resultObject);
    }

    private CelRuntime getRuntime(ScriptExpressionEvaluationContext context) throws ConfigurationException {
        return createRuntime(context);
    }

    private CelRuntime createRuntime(ScriptExpressionEvaluationContext context) throws ConfigurationException {
        // TODO: consider expression profiles?
        // TODO: caching
        CelRuntimeBuilder builder = CelRuntimeFactory.standardCelRuntimeBuilder();
        builder.setOptions(celOptions);
        builder.addLibraries(midPointCelExtensionManager.allRuntimeLibraries());
        addFunctionLibraryImplementations(builder, context);
//        new CelFunctionLibraryMapper(context).runtimeAddFunctionLibraryDeclarations(builder);
        return builder.build();
    }

    private void addFunctionLibraryImplementations(CelRuntimeBuilder builder, ScriptExpressionEvaluationContext context) throws ConfigurationException {
        for (FunctionLibraryBinding funcLib : emptyIfNull(context.getFunctionLibraryBindings())) {
            FunctionLibrary parsedLibrary = funcLib.getParsedLibrary();
            if (parsedLibrary == null) {
                continue;
            }
            functionLibraryProcessor.addRuntimeCustomLibraryImplementations(builder, context, funcLib, parsedLibrary);
        }
    }

    private Map<String, ?> prepareVariablesValueMap(ScriptExpressionEvaluationContext context) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        final Map<String, Object> scriptVariableMap = new HashMap<>();
        prepareScriptVariablesMap(context, scriptVariableMap, CelTypeMapper::convertVariableValue);
        if (!scriptVariableMap.containsKey(ExpressionConstants.VAR_NOW)) {
            scriptVariableMap.put(ExpressionConstants.VAR_NOW, CelTypeMapper.toTimestamp(basicExpressionFunctions.currentDateTime()));
        }
        return scriptVariableMap;
    }

    private Exception processCelException(CelException e) {
        LOGGER.trace("Original CEL exception: {}", e.getMessage(), e);
        // We do NOT want to throw ExpressionEvaluationException here.
        // AbstractScriptEvaluator is catching unknown exceptions and properly formatting them.
        // However, it assumes that all ExpressionEvaluationExceptions are already formatted.
        MelException melCause = ExceptionUtil.findCause(e, MelException.class);
        return Objects.requireNonNullElse(melCause, e);
    }
}
