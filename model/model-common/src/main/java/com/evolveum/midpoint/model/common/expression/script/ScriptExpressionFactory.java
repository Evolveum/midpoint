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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.model.common.expression.functions.CustomFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Creates {@link ScriptExpression} instances. They evaluate Groovy/JS/Python/Velocity/... scripts.
 *
 * Responsibilities:
 *
 * . creates {@link ScriptExpression} instances from {@link ScriptExpressionEvaluatorType} beans;
 * . manages {@link ScriptEvaluator} instances for individual languages (Groovy, JavaScript, ...);
 * . caches custom function libraries (it is a {@link Cache} because for this purpose).
 *
 * @author Radovan Semancik
 */
public class ScriptExpressionFactory implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpressionFactory.class);
    private static final Trace LOGGER_CACHE_CONTENT =
            TraceManager.getTrace(ScriptExpressionFactory.class.getName() + ".content");

    private static final String DEFAULT_LANGUAGE = "http://midpoint.evolveum.com/xml/ns/public/expression/language#Groovy";

    /** Indexed by full language URL, always non-null. Values are non-null as well. Concurrency is just for sure. */
    @NotNull private final Map<String, ScriptEvaluator> evaluatorMap = new ConcurrentHashMap<>();

    @NotNull private final ObjectResolver objectResolver;

    @NotNull private final PrismContext prismContext;

    /** Null only in low-level tests. */
    @Nullable private final RepositoryService repositoryService;

    /** Null only in low-level tests. */
    @Nullable private final CacheRegistry cacheRegistry;

    /** Initialized at startup. The collection is immutable. */
    @NotNull private final Collection<FunctionLibrary> standardFunctionLibraries;

    /** The collection is immutable. */
    private volatile Collection<FunctionLibrary> cachedCustomFunctionLibraries;

    @PostConstruct
    public void register() {
        if (cacheRegistry != null) {
            cacheRegistry.registerCache(this);
        }
    }

    @PreDestroy
    public void unregister() {
        if (cacheRegistry != null) {
            cacheRegistry.unregisterCache(this);
        }
    }

    // Invoked by Spring
    public ScriptExpressionFactory(
            @NotNull PrismContext prismContext,
            @NotNull RepositoryService repositoryService,
            @NotNull Collection<FunctionLibrary> standardFunctionLibraries,
            @NotNull Collection<ScriptEvaluator> evaluators,
            @NotNull CacheRegistry cacheRegistry,
            @NotNull ObjectResolver objectResolver) {
        this.prismContext = prismContext;
        this.repositoryService = Objects.requireNonNull(repositoryService);
        this.standardFunctionLibraries = Collections.unmodifiableCollection(standardFunctionLibraries);
        registerEvaluators(evaluators);
        this.cacheRegistry = Objects.requireNonNull(cacheRegistry); // Important to be non-null to ensure consistency
        this.objectResolver = objectResolver;
    }

    @VisibleForTesting
    public ScriptExpressionFactory(
            @NotNull Collection<FunctionLibrary> standardFunctionLibraries,
            @NotNull ObjectResolver objectResolver) {
        this.prismContext = PrismContext.get();
        this.repositoryService = null;
        this.standardFunctionLibraries = Collections.unmodifiableCollection(standardFunctionLibraries);
        this.cacheRegistry = null;
        this.objectResolver = objectResolver;
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
    @NotNull Collection<FunctionLibrary> getStandardFunctionLibraries() {
        return standardFunctionLibraries;
    }

    public ScriptExpression createScriptExpression(
            @NotNull ScriptExpressionEvaluatorType scriptExpressionBean,
            ItemDefinition<?> outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            String shortDesc,
            OperationResult result)
            throws ExpressionSyntaxException, SecurityViolationException {

        String language = getLanguage(scriptExpressionBean);
        ScriptEvaluator evaluator = getEvaluator(language, shortDesc);
        ScriptExpression expression = new ScriptExpression(evaluator, scriptExpressionBean);
        expression.setPrismContext(prismContext);
        expression.setOutputDefinition(outputDefinition);
        expression.setObjectResolver(objectResolver);
        Collection<FunctionLibrary> allFunctionLibraries = new ArrayList<>(standardFunctionLibraries);
        allFunctionLibraries.addAll(
                getCustomFunctionLibraries(expressionFactory, result));
        expression.setFunctions(allFunctionLibraries);

        // It is not very elegant to process expression profile and script expression profile here.
        // It is somehow redundant, as it was already pre-processed in the expression evaluator/factory
        // We are throwing that out and we are processing it again. But maybe this is consequence of having
        // the duality of Expression and ScriptExpression ... maybe the ScriptExpression is unnecessary abstraction
        // and it should be removed.
        expression.setExpressionProfile(expressionProfile);
        expression.setScriptExpressionProfile(
                processScriptExpressionProfile(
                        expressionProfile,
                        evaluator.getLanguageUrl(), // We need "normalized" language URI here hence not taking from script bean
                        shortDesc));

        return expression;
    }

    private ScriptExpressionProfile processScriptExpressionProfile(
            ExpressionProfile expressionProfile, @NotNull String language, String shortDesc) throws SecurityViolationException {
        if (expressionProfile == null) {
            return null;
        }
        ExpressionEvaluatorProfile evaluatorProfile =
                expressionProfile.getEvaluatorProfile(ScriptExpressionEvaluatorFactory.ELEMENT_NAME);
        if (evaluatorProfile == null) {
            if (expressionProfile.getDefaultDecision() == AccessDecision.ALLOW) {
                return null;
            } else {
                throw new SecurityViolationException(
                        "Access to script expression evaluator not allowed (expression profile: %s) in %s"
                                .formatted(expressionProfile.getIdentifier(), shortDesc));
            }
        }
        ScriptExpressionProfile scriptProfile = evaluatorProfile.getScriptExpressionProfile(language);
        if (scriptProfile != null) {
            return scriptProfile;
        }

        if (evaluatorProfile.getDecision() == AccessDecision.ALLOW) {
            return null;
        } else {
            throw new SecurityViolationException("Access to script language " + language +
                    " not allowed (expression profile: " + expressionProfile.getIdentifier() + ") in " + shortDesc);
        }
    }

    private @NotNull Collection<FunctionLibrary> getCustomFunctionLibraries(
            ExpressionFactory expressionFactory, OperationResult result)
            throws ExpressionSyntaxException {
        Collection<FunctionLibrary> current = cachedCustomFunctionLibraries;
        if (current != null) {
            return current;
        }

        if (repositoryService == null) {
            LOGGER.warn("No repository service set for ScriptExpressionFactory; custom functions will not be loaded. This"
                    + " can occur during low-level testing; never during standard system execution.");
            return List.of(); // intentionally not caching this value
        }

        Collection<FunctionLibrary> fetched = fetchCustomFunctionLibraries(expressionFactory, result);
        cachedCustomFunctionLibraries = fetched;
        return fetched;
    }

    private @NotNull Collection<FunctionLibrary> fetchCustomFunctionLibraries(
            ExpressionFactory expressionFactory, OperationResult result)
            throws ExpressionSyntaxException {
        assert repositoryService != null;
        Map<String, FunctionLibrary> customLibrariesMap = new HashMap<>();
        ResultHandler<FunctionLibraryType> functionLibraryHandler = (object, parentResult) -> {
            LOGGER.trace("Found {}", object);
            // TODO: determine profile from function library archetype
            ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
            FunctionLibrary customLibrary = new FunctionLibrary();
            String libraryName = object.getName().getOrig();
            customLibrary.setVariableName(libraryName);
            customLibrary.setGenericFunctions(
                    new CustomFunctions(object.asObjectable(), expressionFactory, expressionProfile));
            customLibrary.setNamespace(MidPointConstants.NS_FUNC_CUSTOM);
            FunctionLibrary existing = customLibrariesMap.get(libraryName);
            if (existing != null) {
                LOGGER.warn("Multiple custom libraries with the name of '{}'? {} and {}", libraryName, existing, customLibrary);
            }
            customLibrariesMap.put(libraryName, customLibrary);
            return true;
        };
        OperationResult subResult = result
                .createMinorSubresult(ScriptExpressionFactory.class.getName() + ".searchCustomFunctions");
        try {
            LOGGER.trace("Searching for function libraries");
            repositoryService.searchObjectsIterative(
                    FunctionLibraryType.class, null, functionLibraryHandler, createReadOnlyCollection(), true, subResult);
        } catch (SchemaException | RuntimeException e) {
            subResult.recordFatalError("Failed to initialize custom functions", e);
            throw new ExpressionSyntaxException(
                    "An error occurred during custom libraries initialization. " + e.getMessage(), e);
        } finally {
            subResult.close();
        }
        LOGGER.debug("Function libraries found: {}", customLibrariesMap.size());
        return Collections.unmodifiableCollection(
                new ArrayList<>(
                        customLibrariesMap.values()));
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

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(FunctionLibraryType.class)) {
            LOGGER.trace("Invalidating custom functions library cache");
            // Currently we don't try to select entries to be cleared based on OID
            cachedCustomFunctionLibraries = null;
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType()
                .name(ScriptExpressionFactory.class.getName())
                .size(emptyIfNull(cachedCustomFunctionLibraries).size()));
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CACHE_CONTENT.isInfoEnabled()) {
            Collection<FunctionLibrary> cached = cachedCustomFunctionLibraries;
            if (cached != null) {
                cached.forEach(v -> LOGGER_CACHE_CONTENT.info("Cached function library: {}", v));
            } else {
                LOGGER_CACHE_CONTENT.info("Custom function library cache is not yet initialized");
            }
        }
    }
}
