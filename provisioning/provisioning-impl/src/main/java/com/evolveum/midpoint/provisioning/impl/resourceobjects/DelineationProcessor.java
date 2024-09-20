/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Converts object type delineation into {@link SearchHierarchyConstraints} and filters
 * (that are added to the client-supplied query).
 *
 * Works in two modes, see {@link #determineQueryWithConstraints(ProvisioningContext, ObjectQuery, OperationResult)}
 * and the other method.
 */
class DelineationProcessor {

    /** The delination to apply. */
    @NotNull private final ResourceObjectSetDelineation delineation;

    /** The current object definition. Not used to get the delineation, though! */
    @NotNull private final ResourceObjectDefinition definition;

    /** The provisioning context - a wildcard one. */
    @NotNull private final ProvisioningContext ctx;

    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    private DelineationProcessor(
            @NotNull ResourceObjectSetDelineation delineation,
            @NotNull ResourceObjectDefinition definition,
            @NotNull ProvisioningContext ctx) {
        this.definition = definition;
        this.delineation = delineation;
        this.ctx = ctx.toWildcard();
    }

    /**
     * This method gets the object definition and the delineation from the context.
     * This is the standard mode, used for the majority of provisioning operations.
     */
    static @NotNull QueryWithConstraints determineQueryWithConstraints(
            @NotNull ProvisioningContext ctx,
            @Nullable ObjectQuery clientQuery,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        ResourceObjectDefinition definition = getEffectiveDefinition(ctx);
        return new DelineationProcessor(definition.getDelineation(), definition, ctx)
                .execute(clientQuery, result);
    }

    /**
     * Gets object definition and the delineation from the client. Used in special cases.
     * The context is used only as a wildcard one.
     */
    static @NotNull QueryWithConstraints determineQueryWithConstraints(
            @NotNull ProvisioningContext wildcardCtx,
            @NotNull ResourceObjectDefinition definition,
            @NotNull ResourceObjectSetDelineation delineation,
            @Nullable ObjectQuery objectQuery,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        wildcardCtx.assertWildcard();
        return new DelineationProcessor(delineation, definition, wildcardCtx)
                .execute(objectQuery, result);
    }

    private @NotNull QueryWithConstraints execute(@Nullable ObjectQuery clientQuery, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return new QueryWithConstraints(
                ObjectQueryUtil.addConjunctions(clientQuery, delineation.getFilterClauses()),
                determineSearchHierarchyConstraints(result));
    }

    private SearchHierarchyConstraints determineSearchHierarchyConstraints(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {
        ResourceObjectReferenceType baseContextRef = delineation.getBaseContext();
        SearchHierarchyScope scope = delineation.getSearchHierarchyScope();

        var baseContextIdentification = determineBaseContextIdentification(baseContextRef, result);
        if (baseContextIdentification != null || scope != null) {
            return new SearchHierarchyConstraints(baseContextIdentification, scope);
        } else {
            return null;
        }
    }

    private @Nullable ResourceObjectIdentification.WithPrimary determineBaseContextIdentification(
            ResourceObjectReferenceType baseContextRef, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (baseContextRef == null) {
            return null;
        }

        ShadowType baseContextShadow;
        try {
            // We request the use of raw object class definition to avoid endless loops during base context determination.
            baseContextShadow = b.resourceObjectReferenceResolver.resolveUsingRawClass(
                    ctx, baseContextRef, "base context specification in " + definition, result);
        } catch (RuntimeException e) {
            throw new SystemException("Cannot resolve base context for " + definition + ", specified as " + baseContextRef, e);
        }
        if (baseContextShadow == null) {
            throw new ObjectNotFoundException(
                    "Base context not found for " + definition + ", specified as " + baseContextRef,
                    ShadowType.class,
                    null);
        }
        ResourceObjectDefinition baseContextObjectDefinition =
                java.util.Objects.requireNonNull(
                        ctx.getResourceSchema().findDefinitionForShadow(baseContextShadow),
                        () -> "Couldn't determine definition for " + baseContextRef);
        return ResourceObjectIdentification.fromCompleteShadow(baseContextObjectDefinition, baseContextShadow);
    }

    /**
     * Returns the definition to use when search is to be invoked. Normally, we use type or class definition, as provided
     * by the context. But there is a special case when the client asks for the whole class, but the schema machinery provides
     * us with a type definition instead. Here we resolve this.
     */
    private static @NotNull ResourceObjectDefinition getEffectiveDefinition(ProvisioningContext ctx) {
        ResourceObjectDefinition definition = ctx.getObjectDefinitionRequired();
        if (definition.getTypeDefinition() == null) {
            return definition;
        }

        Boolean wholeClass = ctx.getWholeClass();
        stateCheck(
                wholeClass != null,
                "Cannot decide between searching for object type and object class: definition is for a type (%s) but"
                        + " the 'whole class' flag is not present, in: %s", definition, ctx);

        if (wholeClass) {
            return definition.getObjectClassDefinition();
        } else {
            return definition;
        }
    }
}
