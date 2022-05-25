/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.evolveum.midpoint.provisioning.util.QueryConversionUtil.parseFilters;

/**
 * Converts object type delineation into {@link SearchHierarchyConstraints} and additional filters to client-supplied query.
 */
@Component
class DelineationProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectSearchOperation.class);

    @Autowired private ResourceObjectReferenceResolver resourceObjectReferenceResolver;

    QueryWithConstraints determineQueryWithConstraints(ProvisioningContext ctx, ObjectQuery clientQuery, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new QueryWithConstraints(
                createEffectiveQuery(ctx, clientQuery),
                determineSearchHierarchyConstraints(ctx, result));
    }

    private SearchHierarchyConstraints determineSearchHierarchyConstraints(ProvisioningContext ctx, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {
        if (!ctx.isTypeBased()) {
            return null;
        }
        ResourceObjectTypeDefinition objectTypeDef = ctx.getObjectTypeDefinitionRequired();
        ResourceObjectReferenceType baseContextRef = objectTypeDef.getBaseContext();
        SearchHierarchyScope scope = objectTypeDef.getSearchHierarchyScope();

        ResourceObjectIdentification baseContextIdentification = determineBaseContextIdentification(baseContextRef, ctx, result);

        if (baseContextIdentification != null || scope != null) {
            return new SearchHierarchyConstraints(baseContextIdentification, scope);
        } else {
            return null;
        }
    }

    // ctx is type-based
    @Nullable
    private ResourceObjectIdentification determineBaseContextIdentification(
            ResourceObjectReferenceType baseContextRef, ProvisioningContext ctx, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (baseContextRef == null) {
            return null;
        }

        ResourceObjectTypeDefinition objectTypeDef = ctx.getObjectTypeDefinitionRequired();
        PrismObject<ShadowType> baseContextShadow;
        try {
            // We request the use of raw object class definition to avoid endless loops during base context determination.
            baseContextShadow = resourceObjectReferenceResolver.resolve(
                    ctx, baseContextRef, true, "base context specification in " + objectTypeDef, result);
        } catch (RuntimeException e) {
            throw new SystemException("Cannot resolve base context for "+ objectTypeDef +", specified as "+ baseContextRef, e);
        }
        if (baseContextShadow == null) {
            throw new ObjectNotFoundException("Base context not found for " + objectTypeDef + ", specified as " + baseContextRef);
        }
        ResourceObjectDefinition baseContextObjectDefinition =
                java.util.Objects.requireNonNull(
                        ResourceObjectDefinitionResolver.getDefinitionForShadow(ctx.getResourceSchema(), baseContextShadow),
                        () -> "Couldn't determine definition for " + baseContextRef);
        return ShadowUtil.getResourceObjectIdentification(baseContextShadow, baseContextObjectDefinition);
    }

    /**
     * Combines client-specified query and the definition of the object type into a single query.
     */
    private ObjectQuery createEffectiveQuery(ProvisioningContext ctx, ObjectQuery clientQuery) throws SchemaException {
        ResourceObjectDefinition definition = ctx.getObjectDefinitionRequired();
        LOGGER.trace("Computing effective query for {}", definition);
        if (!(definition instanceof ResourceObjectTypeDefinition)) {
            LOGGER.trace(" -> not a type definition, no change");
            return clientQuery;
        }
        ResourceObjectTypeDefinition typeDefinition = (ResourceObjectTypeDefinition) definition;
        List<SearchFilterType> filterClauses = typeDefinition.getDelineation().getAllFilterClauses();
        LOGGER.trace(" -> found {} filter clause(s)", filterClauses.size());
        ObjectQuery effectiveQuery = ObjectQueryUtil.addConjunctions(
                clientQuery,
                parseFilters(filterClauses, ctx.getObjectDefinitionRequired()));
        LOGGER.trace("Effective query:\n{}", DebugUtil.debugDumpLazily(effectiveQuery, 1));
        return effectiveQuery;
    }
}
