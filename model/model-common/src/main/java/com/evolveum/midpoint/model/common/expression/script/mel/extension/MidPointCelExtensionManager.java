/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;

import com.evolveum.midpoint.prism.crypto.Protector;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.collect.ImmutableList;
import dev.cel.common.CelOptions;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensions;
import dev.cel.runtime.CelRuntimeLibrary;

public class MidPointCelExtensionManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointCelExtensionManager.class);

    private final Protector protector;
    private final BasicExpressionFunctions basicExpressionFunctions;
    private final CelOptions celOptions;

    private CelMelExtensions extMel;
    private CelPolyStringExtensions extPolyString;
    private CelFormatExtensions extFormat;
    private CelLdapExtensions extLdap;
    private CelObjectExtensions extObject;
    private CelLogExtensions extLog;

    private ImmutableList<? extends CelCompilerLibrary> allCompilerLibraries;
    private ImmutableList<? extends CelRuntimeLibrary> allRuntimeLibraries;

    public MidPointCelExtensionManager(Protector protector, BasicExpressionFunctions basicExpressionFunctions, CelOptions celOptions) {
        this.protector = protector;
        this.basicExpressionFunctions = basicExpressionFunctions;
        this.celOptions = celOptions;
        initializeExtensions();
    }

    @SuppressWarnings("DataFlowIssue")
    private void initializeExtensions() {
        extMel = CelMelExtensions.library(protector, basicExpressionFunctions).latest();
        extPolyString = CelPolyStringExtensions.library(celOptions, basicExpressionFunctions).latest();
        extFormat = CelFormatExtensions.library(basicExpressionFunctions).latest();
        extLdap = CelLdapExtensions.library(basicExpressionFunctions).latest();
        extObject = CelObjectExtensions.library().latest();
        extLog = CelLogExtensions.library().latest();

        allCompilerLibraries = ImmutableList.of(
                CelExtensions.strings(),
                CelExtensions.bindings(),
                CelExtensions.math(celOptions),
                CelExtensions.encoders(celOptions),
                CelExtensions.sets(celOptions),
                CelExtensions.lists(),
                CelExtensions.regex(),
                CelExtensions.comprehensions(),
                CelExtensions.optional(),
                extMel,
                extPolyString,
                extFormat,
                extLdap,
                extObject,
                extLog
        );

        allRuntimeLibraries = ImmutableList.of(
                CelExtensions.strings(),
                CelExtensions.math(celOptions),
                CelExtensions.encoders(celOptions),
                CelExtensions.sets(celOptions),
                CelExtensions.lists(),
                CelExtensions.regex(),
                CelExtensions.comprehensions(),
                CelExtensions.optional(),
                extMel,
                extPolyString,
                extFormat,
                extLdap,
                extObject,
                extLog
        );
    }

    public Iterable<? extends CelCompilerLibrary> allCompilerLibraries() {
        return allCompilerLibraries;
    }

    public Iterable<? extends CelRuntimeLibrary> allRuntimeLibraries() {
        return allRuntimeLibraries;
    }

}
