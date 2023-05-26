/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.api.Cache;
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
public class ExpressionFactory implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionFactory.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    private final PrismContext prismContext = PrismContext.get();
    private final SecurityContextManager securityContextManager;
    private final LocalizationService localizationService;
    private final Map<QName, ExpressionEvaluatorFactory> evaluatorFactoriesMap = new HashMap<>();

    @Autowired private CacheRegistry cacheRegistry;

    @NotNull private final Map<ExpressionIdentifier, Expression<?, ?>> cache = new ConcurrentHashMap<>();

    // These are set from XML as properties, I'm not sure whether they can be autowired,
    // as there are various subclasses for both of them:
    private ExpressionEvaluatorFactory defaultEvaluatorFactory;
    private ObjectResolver objectResolver;

    public ExpressionFactory(SecurityContextManager securityContextManager, LocalizationService localizationService) {
        this.securityContextManager = securityContextManager;
        this.localizationService = localizationService;
    }

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    public void setObjectResolver(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public SecurityContextManager getSecurityContextManager() {
        return securityContextManager;
    }

    public <V extends PrismValue, D extends ItemDefinition<?>> Expression<V, D> makeExpression(
            ExpressionType expressionType, D outputDefinition, ExpressionProfile expressionProfile,
            String shortDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        ExpressionIdentifier eid = new ExpressionIdentifier(expressionType, outputDefinition, expressionProfile);
        try {
            //noinspection unchecked
            return (Expression<V, D>) cache.computeIfAbsent(eid, expressionIdentifier ->
                    createExpression(expressionType, outputDefinition, expressionProfile, shortDesc, task, result));
        } catch (TunnelException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SchemaException) {
                throw (SchemaException) cause;
            } else if (cause instanceof ObjectNotFoundException) {
                throw (ObjectNotFoundException) cause;
            } else if (cause instanceof SecurityViolationException) {
                throw (SecurityViolationException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new SystemException(cause);
            }
        }
    }

    public <T> Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> makePropertyExpression(
            ExpressionType expressionType, QName outputPropertyName,
            ExpressionProfile expressionProfile, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        //noinspection unchecked
        PrismPropertyDefinition<T> outputDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(outputPropertyName);
        return makeExpression(expressionType, outputDefinition, expressionProfile, shortDesc, task, result);
    }

    @NotNull
    private <V extends PrismValue, D extends ItemDefinition<?>> Expression<V, D> createExpression(ExpressionType expressionType,
            D outputDefinition, ExpressionProfile expressionProfile, String shortDesc, Task task, OperationResult result) {
        try {
            Expression<V, D> expression = new Expression<>(expressionType, outputDefinition, expressionProfile, objectResolver, securityContextManager, prismContext);
            expression.parse(this, shortDesc, task, result);
            return expression;
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException e) {
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

    static class ExpressionIdentifier {
        private final ExpressionType expressionBean;
        private final ItemDefinition<?> outputDefinition;
        private final String expressionProfileIdentifier;
        private final int hashCode;

        private ExpressionIdentifier(ExpressionType expressionBean, ItemDefinition<?> outputDefinition,
                ExpressionProfile expressionProfile) {

            this.expressionBean = expressionBean != null ? expressionBean.clone() : null;
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
            if (!(o instanceof ExpressionIdentifier)) {
                return false;
            }
            ExpressionIdentifier that = (ExpressionIdentifier) o;
            return Objects.equals(expressionBean, that.expressionBean) &&
                    Objects.equals(outputDefinition, that.outputDefinition) &&
                    Objects.equals(expressionProfileIdentifier, that.expressionProfileIdentifier);
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
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
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
