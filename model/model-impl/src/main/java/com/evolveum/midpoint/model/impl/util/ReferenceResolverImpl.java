/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util;

import static java.util.Collections.singletonList;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.util.ReferenceResolver;

import com.evolveum.midpoint.schema.SearchResultList;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@Experimental
@Component
public class ReferenceResolverImpl implements ReferenceResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ReferenceResolverImpl.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public List<PrismObject<? extends ObjectType>> resolve(@NotNull ObjectReferenceType reference,
            Collection<SelectorOptions<GetOperationOptions>> options, @NotNull Source source,
            FilterExpressionEvaluator filterExpressionEvaluator, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        String oid = reference.getOid();

        Class<? extends ObjectType> targetClass = getTargetClass(reference);

        if (oid == null) {
            if (reference.getObject() != null) {
                return singletonList(reference.getObject());
            }

            return resolveFromFilter(targetClass, reference, options, source, filterExpressionEvaluator, task, result);
        } else {
            return singletonList(resolveFromOid(targetClass, oid, options, source, task, result));
        }
    }

    private Class<? extends ObjectType> getTargetClass(@NotNull ObjectReferenceType reference) {
        Class<? extends ObjectType> targetClass;
        if (reference.getType() != null) {
            targetClass = prismContext.getSchemaRegistry().determineClassForTypeRequired(reference.getType(), ObjectType.class);
        } else {
            throw new IllegalArgumentException("Missing type in reference " + reference);
        }
        return targetClass;
    }

    @NotNull
    private PrismObject<? extends ObjectType> resolveFromOid(Class<? extends ObjectType> targetClass, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, @NotNull Source source, Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        LOGGER.trace("Resolving {}:{} from {}", targetClass.getSimpleName(), oid, source);
        return switch (source) {
            case REPOSITORY -> repositoryService.getObject(targetClass, oid, options, result);
            case MODEL -> modelService.getObject(targetClass, oid, options, task, result);
        };
    }

    @NotNull
    private List<PrismObject<? extends ObjectType>> resolveFromFilter(Class<? extends ObjectType> targetClass,
            ObjectReferenceType reference, Collection<SelectorOptions<GetOperationOptions>> options, @NotNull Source source,
            FilterExpressionEvaluator filterExpressionEvaluator, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LOGGER.trace("Resolving filter on {} from {}", targetClass.getSimpleName(), source);
        SearchFilterType filterBean = reference.getFilter();
        if (filterBean == null) {
            throw new IllegalArgumentException("The OID and filter are both null in a reference: " + reference);
        }
        ObjectFilter filter = prismContext.getQueryConverter().parseFilter(filterBean, targetClass);
        ObjectFilter evaluatedFilter =
                filterExpressionEvaluator != null ? filterExpressionEvaluator.evaluate(filter, result) : filter;

        if (evaluatedFilter == null) {
            throw new SchemaException("The OID is null and filter could not be evaluated in " + reference);
        }
        ObjectQuery query = prismContext.queryFactory().createQuery(evaluatedFilter);
        SearchResultList<? extends PrismObject<? extends ObjectType>> objects = switch (source) {
            case REPOSITORY -> repositoryService.searchObjects(targetClass, query, options, result);
            case MODEL -> modelService.searchObjects(targetClass, query, options, task, result);
        };
        //noinspection unchecked
        return (List<PrismObject<? extends ObjectType>>) objects;
    }
}
