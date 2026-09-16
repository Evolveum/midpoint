/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExpressionConfigItem;

import com.evolveum.midpoint.task.api.ExpressionProfileSupplier;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

/**
 * Factory for expressions and registry for expression evaluator factories.
 *
 * @author semancik
 */
public class ExpressionFactory implements CacheInvalidationListener, CacheDiagnostics {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionFactory.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private final PrismContext prismContext = PrismContext.get();
    private final SecurityContextManager securityContextManager;
    private final LocalizationService localizationService;
    private final Map<QName, ExpressionEvaluatorFactory> evaluatorFactoriesMap = new HashMap<>();

    @Autowired private CacheDiagnosticsService cacheDiagnosticsService;
    @Autowired private CacheInvalidationDispatcher cacheInvalidationDispatcher;

    @NotNull private final Map<ExpressionIdentifier, Expression<?, ?>> cache = new ConcurrentHashMap<>();

    // These are set from XML as properties, I'm not sure whether they can be autowired,
    // as there are various subclasses for both of them:
    private ExpressionEvaluatorFactory defaultEvaluatorFactory;
    private ObjectResolver objectResolver;

    /** Computes profiles from trust descriptors. Currently defined system-wide. */
    private ExpressionProfileSupplier expressionProfileSupplier;

    // Used by Spring
    public ExpressionFactory(
            SecurityContextManager securityContextManager,
            LocalizationService localizationService) {
        this.securityContextManager = securityContextManager;
        this.localizationService = localizationService;
    }

    @VisibleForTesting
    public ExpressionFactory(LocalizationService localizationService, ExpressionProfileSupplier expressionProfileSupplier) {
        this.securityContextManager = null;
        this.localizationService = localizationService;
        this.expressionProfileSupplier = expressionProfileSupplier;
    }

    @PostConstruct
    public void register() {
        cacheDiagnosticsService.registerCache(this);
        cacheInvalidationDispatcher.registerListener(this);
    }

    @PreDestroy
    public void unregister() {
        cacheDiagnosticsService.unregisterCache(this);
        cacheInvalidationDispatcher.unregisterListener(this);
    }

    public void setObjectResolver(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    public void setExpressionProfileSupplier(ExpressionProfileSupplier expressionProfileSupplier) {
        this.expressionProfileSupplier = expressionProfileSupplier;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public @Nullable SecurityContextManager getSecurityContextManager() {
        return securityContextManager; // may be null in low-level tests
    }

    /**
     * Temporary method, until migrated to {@link #makeExpression(ExpressionConfigItem, ItemDefinition, String, Task,
     * OperationResult)}.
     *
     * As for origin, we use {@link ConfigurationItemOrigin#undeterminedSafe()}, as the origin is *no longer* used
     * for expression profile determination. We cannot provide diagnostics context via
     * (hence {@link ConfigurationItemOrigin#embedded(Object)}) because {@link ExpressionType} is a prism property real value,
     * so it does not have a link to its parent object.
     */
    @Deprecated // use the variant with config item instead
    public <V extends PrismValue, D extends ItemDefinition<?>> Expression<V, D> makeExpression(
            @Nullable ExpressionType expressionBean,
            D outputDefinition,
            String shortDesc,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
        return makeExpression(
                expressionBean != null ? // This is temporary, see the javadoc
                        ExpressionConfigItem.of(
                                expressionBean, ConfigurationItemOrigin.undeterminedSafe()) :
                        null,
                outputDefinition, shortDesc, task, result
        );
    }

    /**
     * Note that the expression profile is provided here explicitly. The origin of `expressionCI` is not used for that purpose.
     * (Only for easy access to configuration properties and error reporting.)
     */
    public <V extends PrismValue, D extends ItemDefinition<?>> Expression<V, D> makeExpression(
            @Nullable ExpressionConfigItem expressionCI,
            D outputDefinition,
            String shortDesc,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
        var expressionProfile = getExpressionProfile(expressionCI, task, result);
        ExpressionIdentifier eid = new ExpressionIdentifier(expressionCI, outputDefinition, expressionProfile);
        try {
            //noinspection unchecked
            return (Expression<V, D>) cache.computeIfAbsent(eid, expressionIdentifier ->
                    createExpression(expressionCI, outputDefinition, expressionProfile, shortDesc, task, result));
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException schemaException) {
                throw schemaException;
            } else if (cause instanceof ConfigurationException configurationException) {
                throw configurationException;
            } else if (cause instanceof ObjectNotFoundException objectNotFoundException) {
                throw objectNotFoundException;
            } else if (cause instanceof SecurityViolationException securityViolationException) {
                throw securityViolationException;
            } else if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new SystemException(cause);
            }
        }
    }

    private @NotNull ExpressionProfile getExpressionProfile(
            @Nullable ExpressionConfigItem expressionCI,
            Task task,
            OperationResult result) throws SecurityViolationException {
        if (expressionCI == null) {
            // For simplicity, we assume asIs is the default expression. That was the case for ages.
            MiscUtil.stateCheck(SchemaConstantsGenerated.C_AS_IS.equals(defaultEvaluatorFactory.getElementName()),
                    "Default evaluator factory is not 'asIs' but %s", defaultEvaluatorFactory.getElementName());
            // Although we could return ExpressionProfile.full() here, limiting the profile is much safer.
            return ExpressionProfile.asIsOnly();
        } else {
            var trustDescriptor = expressionCI.getTrustDescriptorRequired();
            return MiscUtil.stateNonNull(expressionProfileSupplier,
                            "Expression profile supplier in ExpressionFactory is not set. A problem in Spring wiring?")
                    .getExpressionProfile(trustDescriptor, task, result);
        }
    }

    public <T> Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> makePropertyExpression(
            ExpressionType expressionType, QName outputPropertyName, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
        //noinspection unchecked
        PrismPropertyDefinition<T> outputDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(outputPropertyName);
        return makeExpression(expressionType, outputDefinition, shortDesc, task, result);
    }

    private @NotNull <V extends PrismValue, D extends ItemDefinition<?>> Expression<V, D> createExpression(
            @Nullable ExpressionConfigItem expressionCI,
            @Nullable D outputDefinition,
            @NotNull ExpressionProfile expressionProfile,
            @NotNull String shortDesc,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return Expression.create(
                    expressionCI, outputDefinition, expressionProfile,
                    this, shortDesc, task, result);
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | ConfigurationException e) {
            throw new TunnelException(e);
        }
    }

    public ExpressionEvaluatorFactory getEvaluatorFactory(QName elementName) {
        return evaluatorFactoriesMap.get(elementName);
    }

    public void registerEvaluatorFactory(ExpressionEvaluatorFactory factory) {
        evaluatorFactoriesMap.put(factory.getElementName(), factory);
    }

    ExpressionEvaluatorFactory getDefaultEvaluatorFactory() {
        return defaultEvaluatorFactory;
    }

    public void setDefaultEvaluatorFactory(ExpressionEvaluatorFactory defaultEvaluatorFactory) {
        this.defaultEvaluatorFactory = defaultEvaluatorFactory;
    }

    public @NotNull PrismContext getPrismContext() {
        return prismContext;
    }

    public @NotNull ObjectResolver getObjectResolver() {
        return Objects.requireNonNull(objectResolver, "no object resolver");
    }

    static class ExpressionIdentifier {
        @Nullable private final ExpressionType expressionBean;
        private final ItemDefinition<?> outputDefinition;
        private final String expressionProfileIdentifier; // nullable but eventually non-null
        private final int hashCode;

        private ExpressionIdentifier(
                ExpressionConfigItem expressionCI,
                ItemDefinition<?> outputDefinition,
                ExpressionProfile expressionProfile) {

            this.expressionBean = expressionCI != null ? expressionCI.value().clone() : null;
            this.outputDefinition = cloneDefinitionIfNeeded(outputDefinition);
            this.expressionProfileIdentifier = expressionProfile != null ? expressionProfile.getIdentifier() : null;

            this.hashCode = computeHashCode();
        }

        @Nullable
        private ItemDefinition<?> cloneDefinitionIfNeeded(ItemDefinition<?> outputDefinition) {
            if (outputDefinition != null) {
                if (outputDefinition.isImmutable()) {
                    return outputDefinition;
                } else {
                    // We assume that majority of the cases will be that definition is immutable,
                    // so cloning will not be necessary.
                    if (outputDefinition instanceof PrismContainerDefinition) {
                        PERFORMANCE_ADVISOR.info("Deep clone of container definition: {}. This can harm performance.", outputDefinition);
                    }
                    ItemDefinition<?> clone = outputDefinition.deepClone(DeepCloneOperation.notUltraDeep());
                    clone.freeze();
                    return clone;
                }
            } else {
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ExpressionIdentifier that)) {
                return false;
            }
            return Objects.equals(expressionBean, that.expressionBean)
                    && Objects.equals(outputDefinition, that.outputDefinition)
                    && Objects.equals(expressionProfileIdentifier, that.expressionProfileIdentifier);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        int computeHashCode() {
            return Objects.hash(expressionBean, outputDefinition, expressionProfileIdentifier);
        }
    }

    @Override
    public Collection<CacheInvalidationEventSpecification> getEventSpecifications() {
        return CacheInvalidationEventSpecification.ALL_AVAILABLE_EVENTS; // TODO narrow the scope
    }

    @Override
    public <O extends ObjectType> void invalidate(Class<O> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(FunctionLibraryType.class)) {
            LOGGER.trace("Invalidating expression factory cache");
            // Currently we don't attempt to select entries to be cleared based on function library OID
            cache.clear();
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(
                new SingleCacheStateInformationType()
                        .name(ExpressionFactory.class.getName())
                        .size(cache.size()));
    }

    @Override
    public void dumpContent() {
        // Implement eventually
    }
}
