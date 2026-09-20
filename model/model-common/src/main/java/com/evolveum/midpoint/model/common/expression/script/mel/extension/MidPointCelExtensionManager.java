/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;

import com.evolveum.midpoint.prism.crypto.Protector;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ScriptLanguageExpressionProfile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import dev.cel.common.CelOptions;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.extensions.CelExtensions;
import dev.cel.runtime.CelRuntimeLibrary;
import dev.cel.runtime.RuntimeEquality;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MidPointCelExtensionManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointCelExtensionManager.class);

    private final Protector protector;
    private final BasicExpressionFunctions basicExpressionFunctions;
    private final MidpointFunctions midpointExpressionFunctions;
    private final CelOptions celOptions;
    private final RuntimeEquality runtimeEquality;

    private final Map<String,CelExtensionLibrary.FeatureSet> libraryMap = new HashMap<>();

    public MidPointCelExtensionManager(
            Protector protector,
            BasicExpressionFunctions basicExpressionFunctions,
            MidpointFunctions midpointExpressionFunctions,
            CelOptions celOptions,
            RuntimeEquality runtimeEquality) {
        this.protector = protector;
        this.basicExpressionFunctions = basicExpressionFunctions;
        this.midpointExpressionFunctions = midpointExpressionFunctions;
        this.celOptions = celOptions;
        this.runtimeEquality = runtimeEquality;
        initializeExtensions();
    }

    private void initializeExtensions() {

        // Note: We do NOT want stock CelExtensions.strings() (CelStringExtensions) here!
        // We are implementing string operations slightly differently.
        // E.g. substring() does not die when indexes point beyond end of string.
        // Our versions of string operations is implemented in CelMelExtensions.
        registerLibrary("bindings", CelExtensions.bindings());
        registerLibrary("math", CelExtensions.math(celOptions));
        registerLibrary("encoders", CelExtensions.encoders(celOptions));
        registerLibrary("sets", CelExtensions.sets(celOptions));
        registerLibrary("lists", CelExtensions.lists());
        registerLibrary("regex", CelExtensions.regex());
        registerLibrary("comprehensions", CelExtensions.comprehensions());
//        registerLibrary("optional", CelExtensions.optional());

        registerLibrary(CelMelExtensions.library(celOptions, protector, basicExpressionFunctions, runtimeEquality));
        registerLibrary(CelFormatExtensions.library(basicExpressionFunctions));
        registerLibrary(CelLdapExtensions.library(basicExpressionFunctions));
        registerLibrary(CelObjectExtensions.library(midpointExpressionFunctions));
        registerLibrary(CelLogExtensions.library());
        registerLibrary(CelSecretExtensions.library(protector, basicExpressionFunctions));
        registerLibrary(CelMidPointExtensions.library(midpointExpressionFunctions));
    }

    private void registerLibrary(String name, CelExtensionLibrary.FeatureSet featureSet) {
        if (libraryMap.containsKey(name)) {
            throw new IllegalStateException("Duplicate registration of CEL library "+name);
        }
        libraryMap.put(name, featureSet);
    }

    private void registerLibrary(CelExtensionLibrary<?> library) {
        registerLibrary(library.name(), library.latest());
    }

    public Iterable<? extends CelCompilerLibrary> getCompilerLibraries(
            ScriptLanguageExpressionProfile scriptLanguageExpressionProfile) {
        return libraryMap.entrySet().stream()
                .map(e -> toCompilerLibrary(scriptLanguageExpressionProfile, e))
                .filter(Objects::nonNull)
                .toList();
    }

    private CelCompilerLibrary toCompilerLibrary(
            ScriptLanguageExpressionProfile scriptLanguageExpressionProfile,
            Map.Entry<String, CelExtensionLibrary.FeatureSet> entry) {
        if (!isAllowed(scriptLanguageExpressionProfile, entry.getKey())) {
            return null;
        }
        CelExtensionLibrary.FeatureSet feature = entry.getValue();
        if (feature instanceof CelCompilerLibrary lib) {
            return lib;
        } else {
            return null;
        }
    }

    public Iterable<? extends CelRuntimeLibrary> getRuntimeLibraries(
            ScriptLanguageExpressionProfile scriptLanguageExpressionProfile) {
        return libraryMap.entrySet().stream()
                .map(e -> toRuntimeLibrary(scriptLanguageExpressionProfile, e))
                .filter(Objects::nonNull)
                .toList();
    }

    private CelRuntimeLibrary toRuntimeLibrary(
            ScriptLanguageExpressionProfile scriptExpressionProfile, Map.Entry<String, CelExtensionLibrary.FeatureSet> entry) {
        if (!isAllowed(scriptExpressionProfile, entry.getKey())) {
            return null;
        }
        CelExtensionLibrary.FeatureSet feature = entry.getValue();
        if (feature instanceof CelRuntimeLibrary lib) {
            return lib;
        } else {
            return null;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAllowed(ScriptLanguageExpressionProfile languageProfile, String name) {
        return languageProfile.decidePackageAccess(name) == AccessDecision.ALLOW;
    }
}
