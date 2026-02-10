/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.functions.LibraryFunctionExecutor;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.config.FunctionConfigItem;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelFunctionOverload;
import dev.cel.runtime.CelRuntimeBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class FunctionLibraryProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(FunctionLibraryProcessor.class);

    private static final String FUNCTION_ID_SUFFIX_NOARG = "-noarg";
    private static final String FUNCTION_ID_SUFFIX_UNARY = "-unary";

    public void addCompilerCustomLibraryDeclarations(CelCompilerBuilder builder, ScriptExpressionEvaluationContext context,
            FunctionLibraryBinding funcLibBinding) throws ConfigurationException {
        LOGGER.info("FFFFF: Adding compiler declarations {}", funcLibBinding);
        for (var function : funcLibBinding.getParsedLibrary().getFunctions()) {
            addCompilerCustomFunctionDeclaration(builder, context, funcLibBinding, function);
        }
    }

    private void addCompilerCustomFunctionDeclaration(CelCompilerBuilder builder, ScriptExpressionEvaluationContext context,
            FunctionLibraryBinding funcLibBinding, FunctionConfigItem function) throws ConfigurationException {

        List<ExpressionParameterType> parameterSpecs = function.getParameters();
        if (parameterSpecs.isEmpty()) {
            // No parameter overload declaration. This is invocation short-cut.
            builder.addFunctionDeclarations(
                    CelFunctionDecl.newFunctionDeclaration(
                            getFunctionFullName(funcLibBinding.getVariableName(), function.getName()),
                            CelOverloadDecl.newGlobalOverload(
                                    getFunctionId(funcLibBinding.getVariableName(), function.getName()) + FUNCTION_ID_SUFFIX_NOARG,
                                    CelTypeMapper.toCelType(function.getReturnTypeName())
                            )
                    )
            );
        } else if (parameterSpecs.size() == 1) {
            ExpressionParameterType parameterSpec = parameterSpecs.iterator().next();
            // Single-parameter overload declaration. This is invocation short-cut.
            builder.addFunctionDeclarations(
                    CelFunctionDecl.newFunctionDeclaration(
                            getFunctionFullName(funcLibBinding.getVariableName(), function.getName()),
                            CelOverloadDecl.newGlobalOverload(
                                    getFunctionId(funcLibBinding.getVariableName(), function.getName()) + FUNCTION_ID_SUFFIX_UNARY,
                                    CelTypeMapper.toCelType(function.getReturnTypeName()),
                                    ImmutableList.of(CelTypeMapper.toCelType(parameterSpec.getType()))
                            )
                    )
            );
        }

        // Specification of function overload that accepts map as a parameter.
        // This is a universal way to invoke any function using named parameters.
        builder.addFunctionDeclarations(
                CelFunctionDecl.newFunctionDeclaration(
                        getFunctionFullName(funcLibBinding.getVariableName(), function.getName()),
                        CelOverloadDecl.newGlobalOverload(
                                getFunctionId(funcLibBinding.getVariableName(), function.getName()),
                                CelTypeMapper.toCelType(function.getReturnTypeName()),
                                MapType.create(SimpleType.STRING, SimpleType.ANY)
                        )
                )
        );
    }

    public void addRuntimeCustomLibraryImplementations(CelRuntimeBuilder builder, ScriptExpressionEvaluationContext context,
            FunctionLibraryBinding funcLibBinding, FunctionLibrary parsedLibrary) throws ConfigurationException {
        LOGGER.info("FFFFF: Adding runtime implementation {}", funcLibBinding);
        Object implementation = funcLibBinding.getImplementation();
        if (implementation instanceof LibraryFunctionExecutor executor) {
            for (var function : parsedLibrary.getFunctions()) {
                addRuntimeCustomFunctionImplementation(builder, context, funcLibBinding, executor, function);
            }
        } else {
            throw new UnsupportedOperationException("TODO?");
        }
    }

    private void addRuntimeCustomFunctionImplementation(CelRuntimeBuilder builder, ScriptExpressionEvaluationContext context,
            FunctionLibraryBinding funcLibBinding, LibraryFunctionExecutor executor, FunctionConfigItem function) throws ConfigurationException {

        String functionName = function.getName();

        CelFunctionOverload implementation = args -> {
            try {
                return CelTypeMapper.toCelValue(executor.execute(functionName, toJavaArgMap(function, args)));
            } catch (ExpressionEvaluationException e) {
                throw new MelException(e.getMessage(), e);
            }
        };

        List<Class<?>> argTypes;
        List<ExpressionParameterType> parameterSpecs = function.getParameters();
        if (parameterSpecs.isEmpty()) {
            // No parameter overload declaration. This is invocation short-cut.
            builder.addFunctionBindings(
                    CelFunctionBinding.from(
                            getFunctionId(funcLibBinding.getVariableName(), functionName) + FUNCTION_ID_SUFFIX_NOARG,
                            ImmutableList.of(),
                            implementation)
            );
        } else if (parameterSpecs.size() == 1) {
            // Single-parameter overload declaration. This is invocation short-cut.
            ExpressionParameterType parameterSpec = parameterSpecs.iterator().next();
            builder.addFunctionBindings(
                    CelFunctionBinding.from(
                            getFunctionId(funcLibBinding.getVariableName(), functionName) + FUNCTION_ID_SUFFIX_UNARY,
                            ImmutableList.of(XsdTypeMapper.toJavaType(parameterSpec.getType())),
                            implementation)
            );
        }

        builder.addFunctionBindings(
                CelFunctionBinding.from(
                        getFunctionId(funcLibBinding.getVariableName(), functionName),
                        ImmutableList.of(Map.class),
                        implementation)
        );
    }

    private String getFunctionFullName(String prefix, String functionName) {
        return prefix + "." + functionName;
    }

    private String getFunctionId(String prefix, String functionName) {
        return "mp-custom-" + prefix + "-" + functionName;
    }

    private @NotNull Map<String, Object> toJavaArgMap(FunctionConfigItem function, Object[] args) {
        if (args == null || args.length == 0) {
            return ImmutableMap.of();
        }
        if (function.getParameters().size() == 1) {
            ExpressionParameterType paramSpec = function.getParameters().iterator().next();
            if (args.length == 1) {
                if (CelTypeMapper.isCellNull(args[0])) {
                    return ImmutableMap.of(paramSpec.getName(), null);
                }
                if (args[0] instanceof Map map) {
                    return CelTypeMapper.toJavaValueMap((Map)map);
                }
                return ImmutableMap.of(paramSpec.getName(), CelTypeMapper.toJavaValue(args[0]));
            } else {
                throw new UnsupportedOperationException("TODO?");
            }
        }
        if (args.length == 1) {
            if (args[0] instanceof Map<?,?> map) {
                return CelTypeMapper.toJavaValueMap((Map)map);
            } else {
                throw new UnsupportedOperationException("TODO?");
            }
        } else {
            throw new UnsupportedOperationException("TODO?");
        }
    }



}
