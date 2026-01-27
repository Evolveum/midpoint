/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.extension;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;

import dev.cel.common.CelOptions;

public class MidPointCelExtensionManager {

    private final BasicExpressionFunctions basicExpressionFunctions;
    private final CelOptions celOptions;

    private CelMelExtensions extCelMel;
    private CelPolyStringExtensions extPolyString;

    public MidPointCelExtensionManager(BasicExpressionFunctions basicExpressionFunctions, CelOptions celOptions) {
        this.basicExpressionFunctions = basicExpressionFunctions;
        this.celOptions = celOptions;
        initializeExtensions();
    }

    private void initializeExtensions() {
        extCelMel = CelMelExtensions.library(basicExpressionFunctions).latest();
        extPolyString = CelPolyStringExtensions.library(celOptions, basicExpressionFunctions).latest();
    }

    public CelMelExtensions celMel() {
        return extCelMel;
    }

    public CelPolyStringExtensions polystring() {
        return extPolyString;
    }
}
