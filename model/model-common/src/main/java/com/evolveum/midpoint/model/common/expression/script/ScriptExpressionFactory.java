/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorsProfile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryManager;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptLanguageExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * Creates {@link ScriptExpression} instances. They evaluate Groovy/JS/Python/Velocity/... scripts.
 *
 * Responsibilities:
 *
 * . creates {@link ScriptExpression} instances from {@link ScriptExpressionEvaluatorType} beans;
 * . manages {@link ScriptEvaluator} instances for individual languages (Groovy, JavaScript, ...);
 *
 * @author Radovan Semancik
 */
public class ScriptExpressionFactory {

    private static final String DEFAULT_LANGUAGE = "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";

    /** Indexed by full language URL, always non-null. Values are non-null as well. Concurrency is just for sure. */
    @NotNull private final Map<String, ScriptEvaluator> evaluatorMap = new ConcurrentHashMap<>();

    @NotNull private final ObjectResolver objectResolver;

    @NotNull private final PrismContext prismContext;

    /** Null only in low-level tests. */
    @Nullable private final FunctionLibraryManager functionLibraryManager;

    /** Initialized at startup. The collection is immutable. */
    @NotNull private final Collection<FunctionLibraryBinding> builtInLibraryBindings;

    // Invoked by Spring
    public ScriptExpressionFactory(
            @NotNull PrismContext prismContext,
            @NotNull Collection<FunctionLibraryBinding> builtInLibraryBindings,
            @NotNull Collection<ScriptEvaluator> evaluators,
            @NotNull ObjectResolver objectResolver) {
        this.prismContext = prismContext;
        this.builtInLibraryBindings = Collections.unmodifiableCollection(builtInLibraryBindings);
        registerEvaluators(evaluators);
        this.objectResolver = objectResolver;
        this.functionLibraryManager = ModelCommonBeans.get().functionLibraryManager; // TODO initialize via spring?
    }

    @VisibleForTesting
    public ScriptExpressionFactory(
            @NotNull Collection<FunctionLibraryBinding> builtInLibraryBindings,
            @NotNull ObjectResolver objectResolver) {
        this.prismContext = PrismContext.get();
        this.builtInLibraryBindings = Collections.unmodifiableCollection(builtInLibraryBindings);
        this.objectResolver = objectResolver;
        this.functionLibraryManager = null;
    }

    private void registerEvaluators(@NotNull Collection<ScriptEvaluator> evaluators) {
        for (ScriptEvaluator evaluator : evaluators) {
            registerEvaluator(evaluator);
        }
    }

    @VisibleForTesting
    public void registerEvaluator(@NotNull ScriptEvaluator evaluator) {
        registerEvaluator(evaluator.getLanguageUrl(), evaluator);
    }

    private void registerEvaluator(@NotNull String language, @NotNull ScriptEvaluator evaluator) {
        if (evaluatorMap.containsKey(language)) {
            throw new IllegalArgumentException("Evaluator for language " + language + " already registered");
        }
        evaluatorMap.put(language, evaluator);
    }

    @VisibleForTesting
    public @NotNull ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    @VisibleForTesting
    @NotNull Collection<FunctionLibraryBinding> getBuiltInLibraryBindings() {
        return builtInLibraryBindings;
    }

    public ScriptExpression createScriptExpression(
            @NotNull ScriptExpressionEvaluatorType scriptExpressionBean,
            ItemDefinition<?> outputDefinition,
            ExpressionProfile expressionProfile,
            String shortDesc,
            OperationResult result)
            throws ExpressionSyntaxException, SecurityViolationException {

        String language = getLanguage(scriptExpressionBean);
        ScriptEvaluator evaluator = getEvaluator(language, shortDesc);
        ScriptExpression expression = new ScriptExpression(evaluator, scriptExpressionBean);
        expression.setPrismContext(prismContext);
        expression.setOutputDefinition(outputDefinition);
        expression.setObjectResolver(objectResolver);
        Collection<FunctionLibraryBinding> allLibraryBindings = new ArrayList<>(builtInLibraryBindings);
        allLibraryBindings.addAll(
                getRepoFunctionLibraryBindings(result));
        expression.setFunctionLibraryBindings(allLibraryBindings);

        // It is not very elegant to process expression profile and script expression profile here.
        // It is somehow redundant, as it was already pre-processed in the expression evaluator/factory
        // We are throwing that out and we are processing it again. But maybe this is consequence of having
        // the duality of Expression and ScriptExpression ... maybe the ScriptExpression is unnecessary abstraction
        // and it should be removed.
        expression.setExpressionProfile(expressionProfile);
        expression.setScriptExpressionProfile(
                getScriptLanguageExpressionProfileOrFail(
                        expressionProfile,
                        // We need "normalized" language URI here hence not taking one from the script bean
                        evaluator.getLanguageUrl(),
                        shortDesc));

        return expression;
    }

    private ScriptLanguageExpressionProfile getScriptLanguageExpressionProfileOrFail(
            ExpressionProfile expressionProfile, @NotNull String language, String shortDesc) throws SecurityViolationException {
        if (expressionProfile == null) {
            return null;
        }
        ExpressionEvaluatorsProfile evaluatorsProfile = expressionProfile.getEvaluatorsProfile();

        ExpressionEvaluatorProfile evaluatorProfile =
                evaluatorsProfile.getEvaluatorProfile(ScriptExpressionEvaluatorFactory.ELEMENT_NAME);
        if (evaluatorProfile == null) {
            if (evaluatorsProfile.getDefaultDecision() == AccessDecision.ALLOW) {
                return null;
            } else {
                throw new SecurityViolationException(
                        "Access to script expression evaluator not allowed (expression profile: %s) in %s".formatted(
                                expressionProfile.getIdentifier(), shortDesc));
            }
        }
        ScriptLanguageExpressionProfile languageProfile = evaluatorProfile.getScriptExpressionProfile(language);
        if (languageProfile == null) {
            if (evaluatorProfile.getDecision() == AccessDecision.ALLOW) {
                return null;
            } else {
                throw new SecurityViolationException(
                        "Access to script language %s not allowed (expression profile: %s) in %s".formatted(
                                language, expressionProfile.getIdentifier(), shortDesc));
            }
        }

        return languageProfile;
    }

    private @NotNull Collection<FunctionLibraryBinding> getRepoFunctionLibraryBindings(OperationResult result)
            throws ExpressionSyntaxException {
        if (functionLibraryManager != null) {
            return functionLibraryManager.getFunctionLibraryBindings(result);
        } else {
            return List.of();
        }
    }

    private @NotNull ScriptEvaluator getEvaluator(String languageUri, String shortDesc) throws ExpressionSyntaxException {
        ScriptEvaluator evaluator = getEvaluatorSimple(languageUri);
        if (evaluator != null) {
            return evaluator;
        }

        if (QNameUtil.isUnqualified(languageUri)) {
            List<Map.Entry<String, ScriptEvaluator>> matching = evaluatorMap.entrySet().stream()
                    .filter(entry -> QNameUtil.matchUri(entry.getKey(), languageUri))
                    .collect(Collectors.toList());
            if (!matching.isEmpty()) {
                return MiscUtil.extractSingleton(matching,
                                () -> new ExpressionSyntaxException(
                                        "Language " + languageUri + " matches multiple entries: " + matching))
                        .getValue();
            }
        }
        throw new ExpressionSyntaxException("Unsupported language " + languageUri + " used in script in " + shortDesc);
    }

    @VisibleForTesting
    public @Nullable ScriptEvaluator getEvaluatorSimple(String languageUri) {
        return evaluatorMap.get(languageUri);
    }

    private String getLanguage(ScriptExpressionEvaluatorType expressionBean) {
        return Objects.requireNonNullElse(expressionBean.getLanguage(), DEFAULT_LANGUAGE);
    }
}
