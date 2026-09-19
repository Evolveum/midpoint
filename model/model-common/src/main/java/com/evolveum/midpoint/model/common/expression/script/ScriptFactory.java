/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.SystemObjectCache;

import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationExpressionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryManager;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;

/**
 * Creates {@link Script} instances. They evaluate Groovy/JS/Python/Velocity/... scripts.
 *
 * Responsibilities:
 *
 * . creates {@link Script} instances from {@link ScriptExpressionEvaluatorType} beans;
 * . manages {@link ScriptExecutor} instances for individual languages (Groovy, JavaScript, ...);
 *
 * @author Radovan Semancik
 */
public class ScriptFactory {

    private static final String DEFAULT_LANGUAGE = "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";

    private static final Trace LOGGER = TraceManager.getTrace(ScriptFactory.class);

    /** Indexed by full language URL, always non-null. Values are non-null as well. Concurrency is just for sure. */
    @NotNull private final Map<String, ScriptExecutor> executorMap = new ConcurrentHashMap<>();

    @NotNull private final ObjectResolver objectResolver;

    @NotNull private final PrismContext prismContext;

    /** Null only in low-level tests. */
    @Nullable private final FunctionLibraryManager functionLibraryManager;

    @Nullable private final SystemObjectCache systemObjectCache;

    /** Initialized at startup. The collection is immutable. */
    @NotNull private final Collection<FunctionLibraryBinding> builtInLibraryBindings;

    private String systemDefaultLanguage = null;

    // Invoked by Spring
    public ScriptFactory(
            @NotNull PrismContext prismContext,
            @NotNull Collection<FunctionLibraryBinding> builtInLibraryBindings,
            @NotNull Collection<ScriptExecutor> executors,
            @NotNull ObjectResolver objectResolver,
            @NotNull FunctionLibraryManager functionLibraryManager,
            @NotNull SystemObjectCache systemObjectCache) {
        this.prismContext = prismContext;
        this.builtInLibraryBindings = Collections.unmodifiableCollection(builtInLibraryBindings);
        registerExecutors(executors);
        this.objectResolver = objectResolver;
        this.functionLibraryManager = functionLibraryManager;
        this.systemObjectCache = systemObjectCache;
    }

    @VisibleForTesting
    public ScriptFactory(
            @NotNull Collection<FunctionLibraryBinding> builtInLibraryBindings,
            @NotNull ObjectResolver objectResolver) {
        this.prismContext = PrismContext.get();
        this.builtInLibraryBindings = Collections.unmodifiableCollection(builtInLibraryBindings);
        this.objectResolver = objectResolver;
        this.functionLibraryManager = null;
        this.systemObjectCache = null;
    }

    private void registerExecutors(@NotNull Collection<ScriptExecutor> executors) {
        for (ScriptExecutor executor : executors) {
            registerExecutor(executor);
        }
    }

    @VisibleForTesting
    public void registerExecutor(@NotNull ScriptExecutor evaluator) {
        registerExecutor(evaluator.getLanguageUrl(), evaluator);
    }

    private void registerExecutor(@NotNull String language, @NotNull ScriptExecutor evaluator) {
        if (executorMap.containsKey(language)) {
            throw new IllegalArgumentException("Evaluator for language " + language + " already registered");
        }
        executorMap.put(language, evaluator);
    }

    @VisibleForTesting
    public void replaceExecutor(@NotNull ScriptExecutor evaluator) {
        executorMap.put(evaluator.getLanguageUrl(), evaluator);
    }

    @VisibleForTesting
    public @NotNull ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    @VisibleForTesting
    @NotNull Collection<FunctionLibraryBinding> getBuiltInLibraryBindings() {
        return builtInLibraryBindings;
    }

    /**
     * Creates a script.
     *
     * Note that the caller is responsible for providing an {@link ExpressionEvaluatorProfile}, even though it can be easily
     * derived from the {@link ExpressionProfile}. The evaluator profile is what matters; the root expression profile is there
     * mainly to decide about called function libraries (and later maybe other features).
     */
    public Script createScript(
            @NotNull ScriptExpressionEvaluatorType scriptExpressionBean,
            @Nullable ItemDefinition<?> outputDefinition,
            @NotNull ExpressionProfile expressionProfile,
            @NotNull ExpressionEvaluatorProfile evaluatorExpressionProfile,
            String shortDesc,
            OperationResult result)
            throws ExpressionSyntaxException, SecurityViolationException {

        var language = determineLanguage(scriptExpressionBean, result);
        var executor = getExecutor(language, shortDesc);

        var qualifiedLanguageUri = executor.getLanguageUrl(); // The URI in script bean may be unqualified or missing
        var scriptLanguageExpressionProfile = evaluatorExpressionProfile.getScriptLanguageExpressionProfile(qualifiedLanguageUri);

        var script = new Script(
                scriptExpressionBean, executor, expressionProfile, scriptLanguageExpressionProfile);
        script.setPrismContext(prismContext);
        script.setOutputDefinition(outputDefinition);
        script.setObjectResolver(objectResolver);
        Collection<FunctionLibraryBinding> allLibraryBindings = new ArrayList<>(builtInLibraryBindings);
        allLibraryBindings.addAll(
                getRepoFunctionLibraryBindings(result));
        script.setFunctionLibraryBindings(allLibraryBindings);
        return script;
    }

    private @NotNull Collection<FunctionLibraryBinding> getRepoFunctionLibraryBindings(OperationResult result)
            throws ExpressionSyntaxException {
        if (functionLibraryManager != null) {
            return functionLibraryManager.getFunctionLibraryBindings(result);
        } else {
            return List.of();
        }
    }

    private @NotNull ScriptExecutor getExecutor(String languageUri, String shortDesc) throws ExpressionSyntaxException {
        ScriptExecutor executor = getExecutorSimple(languageUri);
        if (executor != null) {
            return executor;
        }

        if (QNameUtil.isUnqualified(languageUri)) {
            List<Map.Entry<String, ScriptExecutor>> matching = executorMap.entrySet().stream()
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
    public @Nullable ScriptExecutor getExecutorSimple(String languageUri) {
        return executorMap.get(languageUri);
    }

    private String determineLanguage(ScriptExpressionEvaluatorType expressionBean, OperationResult result) {
        if (systemDefaultLanguage == null) {
            initDefaultLanguage(result);
        }
        return Objects.requireNonNullElse(expressionBean.getLanguage(), systemDefaultLanguage);
    }

    private void initDefaultLanguage(OperationResult result) {
        if (systemObjectCache != null) {
            SystemConfigurationExpressionsType expressionsConfig = null;
            try {
                var systemConfiguration = systemObjectCache.getSystemConfiguration(result);
                expressionsConfig = systemConfiguration != null ? systemConfiguration.asObjectable().getExpressions() : null;
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Schema error when determining default scripting language", e);
            }
            if (expressionsConfig != null) {
                systemDefaultLanguage = expressionsConfig.getDefaultScriptLanguage();
            }
        }
        if (systemDefaultLanguage == null) {
            systemDefaultLanguage = DEFAULT_LANGUAGE;
        }
    }

}
