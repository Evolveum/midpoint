/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.CelType;
import dev.cel.common.types.ListType;
import dev.cel.common.types.OpaqueType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.values.CelValue;
import dev.cel.common.values.OpaqueValue;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.parser.Operator;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelFunctionOverload;
import dev.cel.runtime.CelRuntimeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Maps midPoint (built-in) function libraries to CEL functions.
 *
 * @author Radovan Semancik
 */
public class CelFunctionLibraryMapper {

    private static final Trace LOGGER = TraceManager.getTrace(CelFunctionLibraryMapper.class);
    private static final String FUNC_ID_PREFIX = "midpoint-";

    private final ScriptExpressionEvaluationContext context;

    public CelFunctionLibraryMapper(ScriptExpressionEvaluationContext context) {
        this.context = context;
    }

    public void compilerAddFunctionLibraryDeclarations(CelCompilerBuilder builder) {
        for (FunctionLibraryBinding funcLib : emptyIfNull(context.getFunctionLibraryBindings())) {
            compilerAddFunctionLibraryDeclaration(builder, funcLib);
        }
    }


    private void compilerAddFunctionLibraryDeclaration(CelCompilerBuilder builder, FunctionLibraryBinding funcLib) {
        processFunctionLibraryDeclarations(funcLib, (library, method, funcName, funcId) -> {
            CelType returnType = CelTypeMapper.toCelType(method.getReturnType());
            List<CelType> paramTypes = new ArrayList<>();
            for (Type genericParameterType : method.getGenericParameterTypes()) {
                if (genericParameterType == Object.class) {
                    paramTypes.add(SimpleType.ANY);
                } else if (genericParameterType instanceof Class) {
                    paramTypes.add(CelTypeMapper.toCelType((Class<?>)genericParameterType));
                } else if (genericParameterType instanceof ParameterizedType) {
                    ParameterizedType parametrizedType = (ParameterizedType) genericParameterType;
                    LOGGER.info("PPPPPP: owner: {}, raw: {}",parametrizedType.getOwnerType(),parametrizedType.getRawType());
                    Type rawType = parametrizedType.getRawType();
                    if (rawType instanceof Class<?> rawClass) {
                        if (List.class.isAssignableFrom(rawClass)) {
                            Type elementType = ((ParameterizedType) genericParameterType).getActualTypeArguments()[0];
                            LOGGER.info("TTTTTTTT: element type {} ({}) -> {}", elementType, elementType.getClass(), elementType.getTypeName());
                            CelType elementCelType;
                            if (elementType == Object.class) {
                                elementCelType = SimpleType.ANY;
                            } else if (elementType instanceof Class) {
                                elementCelType = CelTypeMapper.toCelType((Class<?>) elementType);
                            } else {
                                throw new IllegalArgumentException("Unexpected element Java Type " + elementType + " (" + elementType.getClass() + ")");
                            }
                            paramTypes.add(ListType.create(elementCelType));
                        } else {
                            throw new IllegalArgumentException("Unexpected Java Type "+rawClass);
                        }
                    } else {
                        throw new IllegalArgumentException("Unexpected Java Type "+genericParameterType+" ("+genericParameterType.getClass()+")");
                    }
                }
            }

            LOGGER.info("FFFF: Declaring CEL function {}[{}]({}): {}", funcName, funcId, StringUtils.join(paramTypes, ", "), returnType);
            builder.addFunctionDeclarations(
                    CelFunctionDecl.newFunctionDeclaration(
                            funcName,
                            CelOverloadDecl.newGlobalOverload(
                                    funcId,
                                    returnType,
                                    paramTypes
                            )
                    )
            );
        });
    }

    private void processFunctionLibraryDeclarations(FunctionLibraryBinding funcLib, FunctionLibraryMethodProcessor processor) {
        String prefix = funcLib.getVariableName();
        Object implementation = funcLib.getImplementation();

        if (!(implementation instanceof BasicExpressionFunctions)) {
            LOGGER.info("FFFF: ignoring library {} -> {}", prefix, implementation.getClass().getName());
            return;
        }
        LOGGER.info("FFFF: processing library {} -> {}", prefix, implementation.getClass().getName());

        CelBasicExpressionFunctions basicLib = new CelBasicExpressionFunctions((BasicExpressionFunctions)implementation);

        for (Method method : basicLib.getClass().getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                LOGGER.info("FFFF: ignoring Object method {}", method.getName());
            }
            LOGGER.info("FFFF: processing method {}", method.getName());
            String funcName = prefix + "." + method.getName();
            String funcId = FUNC_ID_PREFIX + prefix + "-" + method.getName();

            processor.process(basicLib, method, funcName, funcId);
        }
        // TODO
    }

    public void runtimeAddFunctionLibraryDeclarations(CelRuntimeBuilder builder) {
        for (FunctionLibraryBinding funcLib : emptyIfNull(context.getFunctionLibraryBindings())) {
            runtimeAddFunctionLibraryDeclaration(builder, funcLib);
        }
    }

    private void runtimeAddFunctionLibraryDeclaration(CelRuntimeBuilder builder, FunctionLibraryBinding funcLib) {
        processFunctionLibraryDeclarations(funcLib, (library, method, funcName, funcId) -> {

            List<Class<?>> paramTypes = new ArrayList<>();
            for (Parameter parameter : method.getParameters()) {
                paramTypes.add(parameter.getType());
            }

            CelFunctionOverload impl = args -> {
                Object celReturnValue;
                try {
                    celReturnValue = method.invoke(library, CelTypeMapper.toJavaValues(args));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                return CelTypeMapper.toCelValue(celReturnValue);
            };

            LOGGER.info("FFFF: Binding CEL function {}[{}]({})", funcName, funcId, StringUtils.join(paramTypes, ", "));

            builder.addFunctionBindings(
                    CelFunctionBinding.from(
                            funcId,
                            paramTypes,
                            impl
                    )
            );
        });
    }
}
