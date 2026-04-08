/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.extension;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;

import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.common.expression.script.mel.MelScriptEvaluator;
import com.evolveum.midpoint.prism.crypto.Protector;

import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptLanguageExpressionProfile;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import dev.cel.common.CelOptions;
import dev.cel.compiler.CelCompilerLibrary;
import dev.cel.extensions.CelExtensionLibrary;
import dev.cel.extensions.CelExtensions;
import dev.cel.runtime.CelRuntimeLibrary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MidPointCelExtensionManager {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointCelExtensionManager.class);

    private final Protector protector;
    private final BasicExpressionFunctions basicExpressionFunctions;
    private final MidpointFunctions midpointExpressionFunctions;
    private final CelOptions celOptions;

    private final Map<String,CelExtensionLibrary.FeatureSet> libraryMap = new HashMap<>();

    public MidPointCelExtensionManager(Protector protector, BasicExpressionFunctions basicExpressionFunctions, MidpointFunctions midpointExpressionFunctions, CelOptions celOptions) {
        this.protector = protector;
        this.basicExpressionFunctions = basicExpressionFunctions;
        this.midpointExpressionFunctions = midpointExpressionFunctions;
        this.celOptions = celOptions;
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
        registerLibrary("optional", CelExtensions.optional());

        registerLibrary(CelMelExtensions.library(celOptions, protector, basicExpressionFunctions));
        registerLibrary(CelFormatExtensions.library(basicExpressionFunctions));
        registerLibrary(CelLdapExtensions.library(basicExpressionFunctions));
        registerLibrary(CelObjectExtensions.library());
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

    public Iterable<? extends CelCompilerLibrary> getCompilerLibraries(ExpressionProfile expressionProfile) {
        ScriptLanguageExpressionProfile profile = determineProfile(expressionProfile);
        return libraryMap.entrySet().stream()
                .map(e -> toCompilerLibrary(profile, e))
                .filter(Objects::nonNull)
                .toList();
    }

    private CelCompilerLibrary toCompilerLibrary(ScriptLanguageExpressionProfile scriptExpressionProfile, Map.Entry<String, CelExtensionLibrary.FeatureSet> entry) {
        if (!isAllowed(scriptExpressionProfile, entry.getKey())) {
            return null;
        }
        CelExtensionLibrary.FeatureSet feature = entry.getValue();
        if (feature instanceof CelCompilerLibrary lib) {
            return lib;
        } else {
            return null;
        }
    }

    public Iterable<? extends CelRuntimeLibrary> getRuntimeLibraries(ExpressionProfile expressionProfile) {
        ScriptLanguageExpressionProfile profile = determineProfile(expressionProfile);
        return libraryMap.entrySet().stream()
                .map(e -> toRuntimeLibrary(profile, e))
                .filter(Objects::nonNull)
                .toList();
    }

    private CelRuntimeLibrary toRuntimeLibrary(ScriptLanguageExpressionProfile scriptExpressionProfile, Map.Entry<String, CelExtensionLibrary.FeatureSet> entry) {
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

    private ScriptLanguageExpressionProfile determineProfile(ExpressionProfile expressionProfile) {
        if (expressionProfile == null) {
            return null;
        }
        ExpressionEvaluatorProfile evaluatorProfile = expressionProfile
                .getEvaluatorsProfile()
                .getEvaluatorProfile(ScriptExpressionEvaluatorFactory.ELEMENT_NAME);
        if (evaluatorProfile == null) {
            return null;
        }
        return evaluatorProfile.getScriptExpressionProfile(MelScriptEvaluator.LANGUAGE_URL);
    }

    private boolean isAllowed(ScriptLanguageExpressionProfile scriptExpressionProfile, String name) {
        if (scriptExpressionProfile == null) {
            return true;
        }
        return scriptExpressionProfile.decidePackageAccess(name) == AccessDecision.ALLOW;
    }


}
