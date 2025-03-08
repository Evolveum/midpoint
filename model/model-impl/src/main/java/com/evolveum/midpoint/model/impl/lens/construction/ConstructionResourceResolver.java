/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

/**
 * Resolves construction resource reference.
 */
class ConstructionResourceResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionResourceResolver.class);

    private final ResourceObjectConstruction<?, ?> construction;
    private final Task task;
    private final OperationResult result;

    ConstructionResourceResolver(ResourceObjectConstruction<?, ?> construction, Task task, OperationResult result) {
        this.construction = construction;
        this.task = task;
        this.result = result;
    }

    @NotNull ResolvedConstructionResource resolveResource() throws ObjectNotFoundException, SchemaException {
        if (construction.constructionBean == null) {
            throw new IllegalStateException("No construction bean"); // TODO use type safety to avoid this
        }
        ObjectReferenceType resourceRef = construction.constructionBean.getResourceRef();
        if (resourceRef != null) {
            PrismObject<ResourceType> resourceFromRef = resourceRef.asReferenceValue().getObject();
            if (resourceFromRef != null) {
                return new ResolvedConstructionResource(resourceFromRef.asObjectable());
            } else {
                ReferentialIntegrityType refIntegrity = getReferentialIntegrity(resourceRef);
                try {
                    @NotNull ResourceType resource;
                    if (resourceRef.getOid() == null) {
                        resource = resolveResourceRefFilter(" resolving resource ", task, result);
                    } else {
                        resource = LensUtil.getResourceReadOnly(construction.lensContext, resourceRef.getOid(),
                                ModelBeans.get().provisioningService, task, result);
                    }
                    return new ResolvedConstructionResource(resource);
                } catch (ObjectNotFoundException e) {
                    if (refIntegrity == ReferentialIntegrityType.STRICT) {
                        throw e.wrap(
                                "Resource reference seems to be invalid in account construction in " + construction.source);
                    } else if (refIntegrity == ReferentialIntegrityType.RELAXED) {
                        LOGGER.warn("Resource reference couldn't be resolved in {}: {}", construction.source, e.getMessage(), e);
                        return new ResolvedConstructionResource(true);
                    } else if (refIntegrity == ReferentialIntegrityType.LAX) {
                        LOGGER.debug("Resource reference couldn't be resolved in {}: {}", construction.source, e.getMessage(), e);
                        return new ResolvedConstructionResource(false);
                    } else {
                        throw new IllegalStateException("Unsupported referential integrity: "
                                + resourceRef.getReferentialIntegrity());
                    }
                } catch (SecurityViolationException | CommunicationException | ConfigurationException e) {
                    throw new SystemException("Couldn't fetch the resource in account construction in "
                            + construction.source + ": " + e.getMessage(), e);
                } catch (ExpressionEvaluationException e) {
                    throw new SystemException(
                            "Couldn't evaluate filter expression for the resource in account construction in "
                                    + construction.source + ": " + e.getMessage(),
                            e);
                }
            }
        } else {
            throw new IllegalStateException("No resourceRef in resource object construction in " + construction.source);
        }
    }

    private ReferentialIntegrityType getReferentialIntegrity(ObjectReferenceType resourceRef) {
        ReferentialIntegrityType value = resourceRef.getReferentialIntegrity();
        if (value == null || value == ReferentialIntegrityType.DEFAULT) {
            return ReferentialIntegrityType.STRICT;
        } else {
            return value;
        }
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    private ResourceType resolveResourceRefFilter(String sourceDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        VariablesMap variables = ModelImplUtils
                .getDefaultVariablesMap(
                        construction.getFocusOdoAbsolute().getNewObject().asObjectable(),
                        null, null, null);
        ModelImplUtils.addAssignmentPathVariables(construction.getAssignmentPathVariables(), variables);
        LOGGER.debug("Expression variables for filter evaluation: {}", variables);

        assert construction.constructionBean != null;
        assert construction.constructionConfigItem != null; // TODO use CI for resourceRef and the filter here
        ObjectFilter origFilter = PrismContext.get().getQueryConverter()
                .parseFilter(construction.constructionBean.getResourceRef().getFilter(), ResourceType.class);
        LOGGER.debug("Orig filter {}", origFilter);
        // TODO skip determination of expression profile if there is no expression in the filter
        ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(
                origFilter, variables,
                ModelBeans.get().expressionProfileManager.determineExpressionProfileStrict(
                        construction.constructionConfigItem.originFor(
                                ConstructionType.F_RESOURCE_REF.append(ObjectReferenceType.F_FILTER)),
                        task, result),
                ModelBeans.get().expressionFactory,
                " evaluating resource filter expression ", task, result);
        LOGGER.debug("evaluatedFilter filter {}", evaluatedFilter);

        if (evaluatedFilter == null) {
            throw new SchemaException(
                    "The OID is null and filter could not be evaluated in assignment targetRef in " + construction.source);
        }

        ObjectQuery query = PrismContext.get().queryFactory().createQuery(evaluatedFilter);

        Collection<PrismObject<ResourceType>> matchingResources =
                ModelBeans.get().modelObjectResolver.searchObjects(
                        ResourceType.class, query, readOnly(), task, result);

        // TODO consider referential integrity settings
        if (CollectionUtils.isEmpty(matchingResources)) {
            throw new ObjectNotFoundException(
                    "Got no resource from repository, filter: " + evaluatedFilter + ", in " + sourceDescription,
                    ResourceType.class,
                    null);
        }

        if (matchingResources.size() > 1) {
            throw new IllegalArgumentException("Got more than one target from repository, filter:"
                    + evaluatedFilter + ", class:" + ResourceType.class + " in " + sourceDescription);
        }

        return matchingResources.iterator().next().asObjectable();
    }
}
