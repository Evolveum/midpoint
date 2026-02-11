/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import java.util.List;

import com.evolveum.midpoint.model.common.expression.functions.LogExpressionFunctions;

import com.evolveum.midpoint.util.DebugUtil;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelFunctionDecl;
import dev.cel.common.CelOverloadDecl;
import dev.cel.common.types.SimpleType;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.runtime.CelFunctionBinding;

import com.evolveum.midpoint.model.common.expression.script.mel.CelTypeMapper;

/**
 * Extensions for CEL compiler and runtime implementing logging and diagnostic functions.
 *
 * @author Radovan Semancik
 */
public class CelLogExtensions extends AbstractMidPointCelExtensions {


    public CelLogExtensions() {
        initialize();
    }

    @Override
    protected ImmutableSet<Function> initializeFunctions() {
        return ImmutableSet.of(

            // debugDump(param)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "debugDump",
                            CelOverloadDecl.newGlobalOverload(
                                    "mp-debugDump",
                                    "Returns formatted, human-friendly, multi-line dump of a complex data structure.",
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mp-debugDump", Object.class,
                            this::debugDump)),

            // log.error(format, param)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "log.error",
                            CelOverloadDecl.newGlobalOverload(
                                    "mp-log-error",
                                    "Records formatted message in system logs at ERROR level.",
                                    SimpleType.ANY,
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mp-log-error", String.class, Object.class,
                            this::logError)),

            // log.warn(format, param)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "log.warn",
                            CelOverloadDecl.newGlobalOverload(
                                    "mp-log-warn",
                                    "Records formatted message in system logs at WARN level.",
                                    SimpleType.ANY,
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mp-log-warn", String.class, Object.class,
                            this::logWarn)),

            // log.info(format, param)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "log.info",
                            CelOverloadDecl.newGlobalOverload(
                                    "mp-log-info",
                                    "Records formatted message in system logs at INFO level.",
                                    SimpleType.ANY,
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mp-log-info", String.class, Object.class,
                            this::logInfo)),

            // log.debug(format, param)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "log.debug",
                            CelOverloadDecl.newGlobalOverload(
                                    "mp-log-debug",
                                    "Records formatted message in system logs at DEBUG level.",
                                    SimpleType.ANY,
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mp-log-debug", String.class, Object.class,
                            this::logDebug)),

            // log.trace(format, param)
            new Function(
                    CelFunctionDecl.newFunctionDeclaration(
                            "log.trace",
                            CelOverloadDecl.newGlobalOverload(
                                    "mp-log-trace",
                                    "Records formatted message in system logs at TRACE level.",
                                    SimpleType.ANY,
                                    SimpleType.STRING,
                                    SimpleType.ANY)),
                    CelFunctionBinding.from("mp-log-trace", String.class, Object.class,
                            this::logTrace))
        );
    }

    private String debugDump(Object param) {
        return DebugUtil.debugDump(CelTypeMapper.toJavaValue(param), 0);
    }

    private Object logError(String format, Object param) {
        LogExpressionFunctions.LOGGER.error(format, processParams(param));
        return param;
    }

    private Object logWarn(String format, Object param) {
        LogExpressionFunctions.LOGGER.warn(format, processParams(param));
        return param;
    }

    private Object logInfo(String format, Object param) {
        LogExpressionFunctions.LOGGER.info(format, processParams(param));
        return param;
    }

    private Object logDebug(String format, Object param) {
        LogExpressionFunctions.LOGGER.debug(format, processParams(param));
        return param;
    }

    private Object logTrace(String format, Object param) {
        LogExpressionFunctions.LOGGER.trace(format, processParams(param));
        return param;
    }

    private Object[] processParams(Object param) {
        if (CelTypeMapper.isCellNull(param)) {
            return new Object[]{};
        }
        if (param instanceof List<?> params) {
            return CelTypeMapper.toJavaValues(params.toArray());
        }
        return new Object[]{ CelTypeMapper.toJavaValue(param) };
    }

    private static final class Library implements CelExtensionLibrary<CelLogExtensions> {
        private final CelLogExtensions version0;

        private Library() {
            version0 = new CelLogExtensions();
        }

        @Override
        public String name() {
            return "mpObject";
        }

        @Override
        public ImmutableSet<CelLogExtensions> versions() {
            return ImmutableSet.of(version0);
        }
    }

    public static CelExtensionLibrary<CelLogExtensions> library() {
        return new Library();
    }

    @Override
    public int version() {
        return 0;
    }

}
