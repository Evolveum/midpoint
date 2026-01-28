/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;

import com.google.common.collect.ImmutableList;
import dev.cel.common.CelOptions;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensions;
import dev.cel.runtime.CelRuntimeLibrary;

public class MidPointCelExtensionManager {

    private final BasicExpressionFunctions basicExpressionFunctions;
    private final CelOptions celOptions;

    private CelMelExtensions extMel;
    private CelPolyStringExtensions extPolyString;
    private CelFormatExtensions extFormat;
    private CelPrismItemsExtensions extPrismItems;

    private ImmutableList<? extends CelCompilerLibrary> allCompilerLibraries;
    private ImmutableList<? extends CelRuntimeLibrary> allRuntimeLibraries;

    public MidPointCelExtensionManager(BasicExpressionFunctions basicExpressionFunctions, CelOptions celOptions) {
        this.basicExpressionFunctions = basicExpressionFunctions;
        this.celOptions = celOptions;
        initializeExtensions();
    }

    private void initializeExtensions() {
        extMel = CelMelExtensions.library(basicExpressionFunctions).latest();
        extPolyString = CelPolyStringExtensions.library(celOptions, basicExpressionFunctions).latest();
        extFormat = CelFormatExtensions.library(basicExpressionFunctions).latest();
        extPrismItems = CelPrismItemsExtensions.library().latest();

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
                mel(),
                polystring(),
                format(),
                prismItems()
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
                mel(),
                polystring(),
                format(),
                prismItems()
        );
    }

    public CelMelExtensions mel() {
        return extMel;
    }

    public CelPolyStringExtensions polystring() {
        return extPolyString;
    }

    public CelFormatExtensions format() {
        return extFormat;
    }

    public CelPrismItemsExtensions prismItems() {
        return extPrismItems;
    }

    public Iterable<? extends CelCompilerLibrary> allCompilerLibraries() {
        return allCompilerLibraries;
    }

    public Iterable<? extends CelRuntimeLibrary> allRuntimeLibraries() {
        return allRuntimeLibraries;
    }
}
