/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ImmutableSet;
import dev.cel.checker.CelCheckerBuilder;
import dev.cel.common.CelFunctionDecl;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;
import dev.cel.runtime.CelRuntimeBuilder;
import dev.cel.runtime.CelRuntimeLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Extensions for CEL compiler and runtime implementing behavior of "MEL" language.
 *
 * @author Radovan Semancik
 */
public abstract class AbstractMidPointCelExtensions
        implements CelCompilerLibrary, CelRuntimeLibrary, CelExtensionLibrary.FeatureSet {

    public static final String FUNC_CONTAINS_IGNORE_CASE_NAME = "containsIgnoreCase";
    public static final String FUNC_IS_EMPTY_NAME = "isEmpty";
    public static final String FUNC_IS_BLANK_NAME = "isBlank";
    public static final String FUNC_ENCRYPT_NAME = "encrypt";
    public static final String FUNC_DECRYPT_NAME = "decrypt";

    private static final Trace LOGGER = TraceManager.getTrace(AbstractMidPointCelExtensions.class);


    public class Function {

        private final CelFunctionDecl functionDecl;
        private final ImmutableSet<CelFunctionBinding> commonFunctionBindings;

        String getFunction() {
            return functionDecl.name();
        }

        Function(CelFunctionDecl functionDecl, CelFunctionBinding... commonFunctionBindings) {
            this.functionDecl = functionDecl;
            this.commonFunctionBindings = ImmutableSet.copyOf(commonFunctionBindings);
        }
    };
    private ImmutableSet<AbstractMidPointCelExtensions.Function> functions;

    public AbstractMidPointCelExtensions() {
    }

    protected void initialize() {
        functions = initializeFunctions();
    }

    protected abstract ImmutableSet<Function> initializeFunctions();

    @Override
    public ImmutableSet<CelFunctionDecl> functions() {
        return functions.stream().map(f -> f.functionDecl).collect(toImmutableSet());
    }

    @Override
    public void setCheckerOptions(CelCheckerBuilder checkerBuilder) {
        functions.forEach(function -> checkerBuilder.addFunctionDeclarations(function.functionDecl));
    }

    @Override
    public void setRuntimeOptions(CelRuntimeBuilder runtimeBuilder) {
        functions.forEach(function -> {
            runtimeBuilder.addFunctionBindings(function.commonFunctionBindings);
        });
    }

    @Nullable
    protected static Object toJava(@Nullable Object o) {
        return CelTypeMapper.toJavaValue(o);
    }

    protected static boolean isCellNull(@Nullable Object object) {
        return CelTypeMapper.isCellNull(object);
    }

    @NotNull
    protected RuntimeException createException(@NotNull Exception e) {
        // TODO: better error handling
        return new RuntimeException(e.getMessage(), e);
    }

    @NotNull
    protected RuntimeException createException(@NotNull String message) {
        // TODO: better error handling
        return new RuntimeException(message);
    }

    @NotNull
    protected RuntimeException createException(@NotNull String message, @NotNull Exception e) {
        // TODO: better error handling
        return new RuntimeException(message, e);
    }


}
