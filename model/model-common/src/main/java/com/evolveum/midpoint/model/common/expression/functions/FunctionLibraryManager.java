/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.functions;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionProfileManager;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.api.Cache;

import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.config.FunctionConfigItem;
import com.evolveum.midpoint.schema.config.FunctionExpressionEvaluatorConfigItem;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Manages the function libraries, especially the ones that are created by parsing {@link FunctionLibraryType} objects.
 */
@Component
public class FunctionLibraryManager implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(FunctionLibraryManager.class);
    private static final Trace LOGGER_CACHE_CONTENT =
            TraceManager.getTrace(FunctionLibraryManager.class.getName() + ".content");

    private static final String CACHE_LIBRARIES_NAME = FunctionLibraryManager.class.getName() + ".libraries";
    private static final String CACHE_BINDINGS_NAME = FunctionLibraryManager.class.getName() + ".bindings";

    private static final String OP_FETCH_FUNCTION_LIBRARIES = FunctionLibraryManager.class.getName() + ".fetchFunctionLibraries";

    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService repositoryService;
    @Autowired private CacheRegistry cacheRegistry;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ExpressionProfileManager expressionProfileManager;

    /** Indexed by OID. The collection is immutable. */
    private volatile Map<String, FunctionLibrary> cachedLibraries;

    /** The collection is immutable. */
    private volatile List<FunctionLibraryBinding> cachedLibraryBindings;

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    public @NotNull List<FunctionLibraryBinding> getFunctionLibraryBindings(OperationResult result)
            throws ExpressionSyntaxException {
        var current = cachedLibraryBindings;
        if (current != null) {
            return current;
        }

        Map<String, FunctionLibrary> libraries = getFunctionLibraries(result);
        var bindings =
                libraries.values().stream()
                        .map(lib -> lib.createBinding(expressionFactory))
                        .toList();

        cachedLibraryBindings = bindings;
        return bindings;
    }

    private @NotNull Map<String, FunctionLibrary> getFunctionLibraries(OperationResult result) throws ExpressionSyntaxException {
        var current = cachedLibraries;
        if (current != null) {
            return current;
        }
        Map<String, FunctionLibrary> fetched = fetchFunctionLibraries(result);
        cachedLibraries = fetched;
        return fetched;
    }

    private @NotNull Map<String, FunctionLibrary> fetchFunctionLibraries(OperationResult parentResult)
            throws ExpressionSyntaxException {
        OperationResult result = parentResult.createMinorSubresult(OP_FETCH_FUNCTION_LIBRARIES);
        try {
            LOGGER.trace("Searching for function libraries");
            List<PrismObject<FunctionLibraryType>> libraryObjects =
                    repositoryService.searchObjects(FunctionLibraryType.class, null, readOnly(), result);
            Map<String, FunctionLibrary> byOid = new HashMap<>();
            for (PrismObject<FunctionLibraryType> libraryObject : libraryObjects) {
                LOGGER.trace("Found {}", libraryObject);
                byOid.put(
                        libraryObject.getOid(),
                        FunctionLibrary.of(libraryObject.asObjectable()));
            }
            LOGGER.debug("Function libraries found: {}", byOid.size());
            return byOid;
        } catch (SchemaException | ConfigurationException | RuntimeException e) {
            result.recordException("Failed to initialize custom functions", e);
            throw new ExpressionSyntaxException(
                    "An error occurred during function libraries initialization. " + e.getMessage(), e);
        } finally {
            result.close();
        }
    }

    /**
     * Finds a function by name. See {@link FunctionLibrary#findFunction(String, Collection, String)} for details.
     */
    public @NotNull FunctionInLibrary findFunction(
            @NotNull FunctionExpressionEvaluatorConfigItem functionCall,
            @NotNull String contextDesc,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        var libraryOid = functionCall.getLibraryOid();
        var library = findLibrary(libraryOid, result);
        if (library == null) {
            throw new ConfigurationException(
                    "No function library with OID %s found in %s".formatted(libraryOid, contextDesc));
        }
        return new FunctionInLibrary(
                library.findFunction(functionCall.getFunctionName(), functionCall.getArgumentNames(), contextDesc),
                library);
    }

    private @Nullable FunctionLibrary findLibrary(@NotNull String libraryOid, @NotNull OperationResult result)
            throws SchemaException {
        var library =
                getFunctionLibraries(result)
                        .get(libraryOid);
        if (library != null) {
            return library;
        }
        // Let us try again - just for sure
        invalidate();
        return getFunctionLibraries(result)
                .get(libraryOid);
    }

    public <V extends PrismValue, D extends ItemDefinition<?>> Expression<V, D> createFunctionExpression(
            FunctionConfigItem function,
            D outputDefinition,
            @NotNull ExpressionProfile functionExpressionProfile,
            Task task,
            OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ConfigurationException {

        return expressionFactory
                .makeExpression(
                        function, outputDefinition, functionExpressionProfile,
                        "function execution", task, result);
    }

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(FunctionLibraryType.class)) {
            // Currently we don't try to select entries to be cleared based on OID
            invalidate();
        }
    }

    private void invalidate() {
        LOGGER.trace("Invalidating custom functions library cache");
        cachedLibraries = null;
        cachedLibraryBindings = null;
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return List.of(
                new SingleCacheStateInformationType()
                        .name(CACHE_LIBRARIES_NAME)
                        .size(emptyIfNull(cachedLibraries).size()),
                new SingleCacheStateInformationType()
                        .name(CACHE_BINDINGS_NAME)
                        .size(emptyIfNull(cachedLibraryBindings).size()));
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CACHE_CONTENT.isInfoEnabled()) {
            emptyIfNull(this.cachedLibraries).values()
                    .forEach(v -> LOGGER_CACHE_CONTENT.info("Cached function library: {}", v));
            emptyIfNull(this.cachedLibraryBindings)
                    .forEach(v -> LOGGER_CACHE_CONTENT.info("Cached function library binding: {}", v));
        }
    }

    /** TODO cache */
    public @NotNull ExpressionProfile determineFunctionExpressionProfile(
            @NotNull FunctionLibrary library, @NotNull OperationResult result) throws SchemaException, ConfigurationException {
        return expressionProfileManager.determineExpressionProfile(
                library.getLibraryObject(), result);
    }

    public void checkCallAllowed(
            @NotNull FunctionInLibrary function,
            // TODO change to not-null when profiles are ubiquitous
            @Nullable ExpressionProfile expressionProfile) throws ConfigurationException, SecurityViolationException {
        if (expressionProfile != null) {
            var decision = expressionProfile.getLibrariesProfile().decideFunctionAccess(
                    function.library.getOid(),
                    function.function.getName());
            if (decision != AccessDecision.ALLOW) {
                throw new SecurityViolationException(
                        "Access to function library method %s %s (applied expression profile '%s', libraries profile '%s')"
                                .formatted(
                                        function,
                                        decision == AccessDecision.DENY ? "denied" : "not allowed",
                                        expressionProfile.getIdentifier(),
                                        expressionProfile.getLibrariesProfile().getIdentifier()));
            }
        }
    }

    public record FunctionInLibrary(
            @NotNull FunctionConfigItem function,
            @NotNull FunctionLibrary library) {

        @Override
        public String toString() {
            return function.value().getName() + " in " + library.getLibraryObject();
        }
    }
}
