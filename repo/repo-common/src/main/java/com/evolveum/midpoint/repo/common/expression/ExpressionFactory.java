/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.api.Cacheable;
import com.evolveum.midpoint.repo.cache.registry.CacheRegistry;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;

/**
 * Factory for expressions and registry for expression evaluator factories.
 *
 * @author semancik
 *
 */
public class ExpressionFactory implements Cacheable {

    private Map<QName,ExpressionEvaluatorFactory> evaluatorFactoriesMap = new HashMap<>();
    private ExpressionEvaluatorFactory defaultEvaluatorFactory;
    @NotNull private Map<ExpressionIdentifier, Expression<?,?>> cache = new HashMap<>();
    final private PrismContext prismContext;
    private ObjectResolver objectResolver;                    // using setter to allow Spring to handle circular references
    final private SecurityContextManager securityContextManager;
    private LocalizationService localizationService;
    private CacheRegistry cacheRegistry;

    public ExpressionFactory(SecurityContextManager securityContextManager, PrismContext prismContext,
            LocalizationService localizationService) {
        this.prismContext = prismContext;
        this.securityContextManager = securityContextManager;
        this.localizationService = localizationService;
    }

    public void setObjectResolver(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    public void setCacheRegistry(CacheRegistry cacheRegistry) {
        this.cacheRegistry = cacheRegistry;
    }

    @PostConstruct
    public void register() {
        cacheRegistry.registerCacheableService(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCacheableService(this);
    }

    public <V extends PrismValue,D extends ItemDefinition> Expression<V,D> makeExpression(ExpressionType expressionType,
            D outputDefinition, ExpressionProfile expressionProfile, String shortDesc, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        ExpressionIdentifier eid = new ExpressionIdentifier(expressionType, outputDefinition);
        //noinspection unchecked
        Expression<V,D> expression = (Expression<V,D>) cache.get(eid);
        if (expression == null) {
            expression = createExpression(expressionType, outputDefinition, expressionProfile, shortDesc, task, result);
            cache.put(eid, expression);
        }
        return expression;
    }

    public <T> Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> makePropertyExpression(
            ExpressionType expressionType, QName outputPropertyName, ExpressionProfile expressionProfile, String shortDesc, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        //noinspection unchecked
        PrismPropertyDefinition<T> outputDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(outputPropertyName);
        return makeExpression(expressionType, outputDefinition, expressionProfile, shortDesc, task, result);
    }

    private <V extends PrismValue,D extends ItemDefinition> Expression<V,D> createExpression(ExpressionType expressionType,
            D outputDefinition, ExpressionProfile expressionProfile, String shortDesc, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        Expression<V,D> expression = new Expression<>(expressionType, outputDefinition, expressionProfile, objectResolver, securityContextManager, prismContext);
        expression.parse(this, shortDesc, task, result);
        return expression;
    }

    public ExpressionEvaluatorFactory getEvaluatorFactory(QName elementName) {
        return evaluatorFactoriesMap.get(elementName);
    }

    public void registerEvaluatorFactory(ExpressionEvaluatorFactory factory) {
        evaluatorFactoriesMap.put(factory.getElementName(), factory);
    }

    public ExpressionEvaluatorFactory getDefaultEvaluatorFactory() {
        return defaultEvaluatorFactory;
    }

    public void setDefaultEvaluatorFactory(ExpressionEvaluatorFactory defaultEvaluatorFactory) {
        this.defaultEvaluatorFactory = defaultEvaluatorFactory;
    }

    class ExpressionIdentifier {
        private ExpressionType expressionType;
        private ItemDefinition outputDefinition;

        ExpressionIdentifier(ExpressionType expressionType, ItemDefinition outputDefinition) {
            super();
            this.expressionType = expressionType;
            this.outputDefinition = outputDefinition;
        }

        public ExpressionType getExpressionType() {
            return expressionType;
        }

        public void setExpressionType(ExpressionType expressionType) {
            this.expressionType = expressionType;
        }

        public ItemDefinition getOutputDefinition() {
            return outputDefinition;
        }

        public void setOutputDefinition(ItemDefinition outputDefinition) {
            this.outputDefinition = outputDefinition;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((expressionType == null) ? 0 : expressionType.hashCode());
            result = prime * result + ((outputDefinition == null) ? 0 : outputDefinition.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExpressionIdentifier other = (ExpressionIdentifier) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (expressionType == null) {
                if (other.expressionType != null)
                    return false;
            } else if (!expressionType.equals(other.expressionType))
                return false;
            if (outputDefinition == null) {
                if (other.outputDefinition != null)
                    return false;
            } else if (!outputDefinition.equals(other.outputDefinition))
                return false;
            return true;
        }

        private ExpressionFactory getOuterType() {
            return ExpressionFactory.this;
        }
    }

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(FunctionLibraryType.class)) {
            // Currently we don't attempt to select entries to be cleared based on function library OID
            cache = new HashMap<>();
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(
                new SingleCacheStateInformationType(prismContext)
                        .name(ExpressionFactory.class.getName())
                        .size(cache.size())
        );
    }

    @Override
    public void dumpContent() {
        // Implement eventually
    }
}
