/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.util.exception.*;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.OpaqueValue;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.parser.Operator;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeBuilder;
import dev.cel.runtime.CelRuntimeFactory;

import org.apache.commons.lang3.NotImplementedException;
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

    public static final String LANGUAGE_NAME = "CEL";
    private static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;
    private static final String FUNCTION_STRING_EQUALS_OPAQUE_ID = "string-equals-opaque";
    private static final String FUNCTION_OPAQUE_EQUALS_STRING_ID = "opaque-equals-string";
    private static final String FUNCTION_POLYSTRING_ORIG_NAME = "orig";
    private static final String FUNCTION_POLYSTRING_ORIG_ID = "polystring-orig";
    private static final String FUNCTION_POLYSTRING_NORM_NAME = "norm";
    private static final String FUNCTION_POLYSTRING_NORM_ID = "polystring-norm";

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
        builder.setStandardMacros(CelStandardMacro.HAS);
        compilerAddPolyStringDeclarations(builder, context);
        // TODO: Further compiler config
        new CelFunctionLibraryMapper(context).compilerAddFunctionLibraryDeclarations(builder);
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


//    @Override
    protected Object evaluateScript(CelAbstractSyntaxTree compiledScript, ScriptExpressionEvaluationContext context) throws Exception {

        CelRuntime runtime = getRuntime(context);
        CelRuntime.Program program = runtime.createProgram(compiledScript);

        Map<String, ?> variables = prepareVariablesValueMap(context);
        Object resultObject = program.eval(variables);
        return resultObject;
    }

    private CelRuntime getRuntime(ScriptExpressionEvaluationContext context) {
        return createRuntime(context);
    }

    private CelRuntime createRuntime(ScriptExpressionEvaluationContext context) {
        // TODO: consider expression profiles?
        // TODO: caching
        CelRuntimeBuilder builder = CelRuntimeFactory.standardCelRuntimeBuilder();
        new CelFunctionLibraryMapper(context).runtimeAddFunctionLibraryDeclarations(builder);
        runtimeAddPolyStringDeclarations(builder, context);
        return builder.build();
    }

    private Map<String, ?> prepareVariablesValueMap(ScriptExpressionEvaluationContext context) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        final Map<String, Object> scriptVariableMap = new HashMap<>();
        prepareScriptVariablesMap(context, scriptVariableMap, CelTypeMapper::convertVariableValue);
        return scriptVariableMap;
    }

    private void compilerAddPolyStringDeclarations(CelCompilerBuilder builder, ScriptExpressionEvaluationContext context) {
        builder.addFunctionDeclarations(
                CelFunctionDecl.newFunctionDeclaration(
                        Operator.EQUALS.getFunction(),
                        CelOverloadDecl.newGlobalOverload(
                                FUNCTION_STRING_EQUALS_OPAQUE_ID,
                                SimpleType.BOOL,
                                SimpleType.STRING,
                                CelTypeMapper.POLYSTRING_TYPE
                        )
                ),
                CelFunctionDecl.newFunctionDeclaration(
                        Operator.EQUALS.getFunction(),
                        CelOverloadDecl.newGlobalOverload(
                                FUNCTION_OPAQUE_EQUALS_STRING_ID,
                                SimpleType.BOOL,
                                CelTypeMapper.POLYSTRING_TYPE,
                                SimpleType.STRING
                        )
                ),
                CelFunctionDecl.newFunctionDeclaration(
                        FUNCTION_POLYSTRING_ORIG_NAME,
                        CelOverloadDecl.newMemberOverload(
                                FUNCTION_POLYSTRING_ORIG_ID,
                                SimpleType.STRING,
                                CelTypeMapper.POLYSTRING_TYPE
                        )
                ),
                CelFunctionDecl.newFunctionDeclaration(
                        FUNCTION_POLYSTRING_NORM_NAME,
                        CelOverloadDecl.newMemberOverload(
                                FUNCTION_POLYSTRING_NORM_ID,
                                SimpleType.STRING,
                                CelTypeMapper.POLYSTRING_TYPE
                        )
                )
        );
    }

    private void runtimeAddPolyStringDeclarations(CelRuntimeBuilder builder, ScriptExpressionEvaluationContext context) {
        builder.addFunctionBindings(
                CelFunctionBinding.from(
                        FUNCTION_STRING_EQUALS_OPAQUE_ID,
                        String.class, OpaqueValue.class,
                        CelTypeMapper::stringEqualsOpaque
                ),
                CelFunctionBinding.from(
                        FUNCTION_OPAQUE_EQUALS_STRING_ID,
                        OpaqueValue.class, String.class,
                        CelTypeMapper::opaqueEqualsString
                ),
                CelFunctionBinding.from(
                        FUNCTION_POLYSTRING_ORIG_ID,
                        OpaqueValue.class,
                        CelTypeMapper::funcPolystringOrig
                ),
                CelFunctionBinding.from(
                        FUNCTION_POLYSTRING_NORM_ID,
                        OpaqueValue.class,
                        CelTypeMapper::funcPolystringNorm
                )
        );
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
