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
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.script.mel.extension.MidPointCelExtensionManager;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

import dev.cel.common.*;
import dev.cel.common.types.*;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.*;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Expression evaluator that is using MidPoint Expression Language (MEL).
 * MidPoint Expression Language (MEL) is based on Common Expression Language (CEL),
 * extended with midPoint-specific functionality.
 */
public class MelScriptEvaluator extends AbstractCachingScriptEvaluator<CelRuntime, CelAbstractSyntaxTree, CelScriptCacheKey> {


    private static final Trace LOGGER = TraceManager.getTrace(MelScriptEvaluator.class);

    public static final String LANGUAGE_NAME = "mel";
    public static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    private final BasicExpressionFunctions basicExpressionFunctions;
    private final MidpointFunctions midpointExpressionFunctions;
    private final CelOptions celOptions = CelOptions.current()
            .enableRegexPartialMatch(true)
            .enableOptionalSyntax(true)
            .enableShortCircuiting(true)
            .build();

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

    @Override
    protected boolean needsServiceVariables() { return false; }

    @Override
    protected boolean supportsDeprecatedVariables() { return false; }

    @Override
    protected CelAbstractSyntaxTree compileScript(String codeString, ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, SecurityViolationException {
        CelValidationResult validationResult;
        try {
            validationResult = createCompiler(context).compile(codeString, context.getContextDescription());
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

    @Override
    protected CelScriptCacheKey getScriptCachingKey(String codeString, ScriptExpressionEvaluationContext context)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {

        Map<String,CelType> celTypeMap = new HashMap<>();
        Map<String, TypedValue<?>> variables = prepareScriptVariablesTypedValueMap(context);
        for (var varEntry : variables.entrySet()) {
            celTypeMap.put(varEntry.getKey(), CelTypeMapper.toCelNullableType(varEntry.getValue()));
        }
        if (!variables.containsKey(ExpressionConstants.VAR_NOW)) {
            celTypeMap.put(ExpressionConstants.VAR_NOW, SimpleType.TIMESTAMP);
        }

        CelType resultType = determineResultType(context);

        return new CelScriptCacheKey(codeString, celTypeMap, resultType);
    }


    private CelCompiler createCompiler(ScriptExpressionEvaluationContext context) throws SecurityViolationException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectNotFoundException {
        CelCompilerBuilder builder = CelCompilerFactory.standardCelCompilerBuilder();
        builder.setOptions(celOptions);
        builder.setStandardMacros(CelStandardMacro.STANDARD_MACROS);
        builder.setTypeProvider(getTypeProvider());
        builder.addLibraries(midPointCelExtensionManager.getCompilerLibraries(context.getExpressionProfile()));
        addCompilerVariables(builder, context);
        addFunctionLibraryDeclarations(builder, context);
        builder.setResultType(determineResultType(context));
        return builder.build();
    }

    private void addCompilerVariables(CelCompilerBuilder builder, ScriptExpressionEvaluationContext context) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        Map<String, TypedValue<?>> variables = prepareScriptVariablesTypedValueMap(context);
        for (var varEntry : variables.entrySet()) {
            builder.addVar(varEntry.getKey(), CelTypeMapper.toCelNullableType(varEntry.getValue()));
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
        ItemDefinition<?> outputDefinition = context.getOutputDefinition();
        if (outputDefinition == null) {
            return SimpleType.ANY;
        }
        // Let's not be too smart here.
        // We could determine specific CEL type from outputDefinition, but we do not want to.
        // There are cases when the expression expects polystring, but the script produces string.
        // Setting polystring as a expected result type would cause an error in CEL compiler.
        // We do not want that.
        // We want the CEL script to produce whatever it produces, then we can use our heuristics to
        // convert it to correct type.
        // All we want to do is to indicate whether we are expecting scalar or list value.
        if (isSingleScalarResult(context)) {
            return SimpleType.ANY;
        } else {
            return ListType.create(SimpleType.ANY);
        }
    }

    private boolean isSingleScalarResult(ScriptExpressionEvaluationContext context) {
        if (context.getSuggestedReturnType() == null) {
            return context.getOutputDefinition().isSingleValue();
        } else {
            return context.getSuggestedReturnType() == ScriptExpressionReturnTypeType.SCALAR;
        }
    }

    @Override
    protected Object evaluateScript(CelAbstractSyntaxTree compiledScript, ScriptExpressionEvaluationContext context) throws Exception {

        CelRuntime runtime = getInterpreter(context);
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

    @Override
    protected CelRuntime createInterpreter(ScriptExpressionEvaluationContext context) throws ConfigurationException {
        // TODO: consider expression profiles?
        CelRuntimeBuilder builder = CelRuntimeFactory.standardCelRuntimeBuilder();
        builder.setOptions(celOptions);
        builder.addLibraries(midPointCelExtensionManager.getRuntimeLibraries(context.getExpressionProfile()));
        addFunctionLibraryImplementations(builder, context);
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
            scriptVariableMap.put(ExpressionConstants.VAR_NOW, CelTypeMapper.toInstant(basicExpressionFunctions.currentDateTime()));
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
