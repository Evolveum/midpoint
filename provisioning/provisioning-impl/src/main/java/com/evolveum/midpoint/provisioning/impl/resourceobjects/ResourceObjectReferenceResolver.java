/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType.*;

import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.util.QueryConversionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifier;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Resolves resource objects or secondary identifiers.
 *
 * This class invokes functionality outside "resource objects" package by looking shadows up
 * via {@link ShadowFinder} and {@link ShadowsFacade}.
 *
 * Intentionally not a public class.
 *
 * @author semancik
 */
@Component
class ResourceObjectReferenceResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectReferenceResolver.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ShadowFinder shadowFinder;
    @Autowired private ShadowsFacade shadowsFacade;

    /**
     * Resolves a {@link ResourceObjectReferenceType}. Uses raw class definition for this purpose.
     */
    @Nullable ShadowType resolveUsingRawClass(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectReferenceType resourceObjectReference,
            @NotNull String desc,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ObjectReferenceType shadowRef = resourceObjectReference.getShadowRef();
        ResourceObjectReferenceResolutionFrequencyType resolutionFrequency =
                Objects.requireNonNullElse(resourceObjectReference.getResolutionFrequency(), ONCE);

        String shadowOid = getOid(shadowRef);
        if (shadowOid != null) {
            if (resolutionFrequency != ALWAYS) {
                PrismObject<ShadowType> shadow = shadowFinder.getShadow(shadowOid, result);
                shadowsFacade.applyDefinition(shadow, ctx.getTask(), result);
                return shadow.asObjectable();
            }
        } else if (resolutionFrequency == NEVER) {
            throw new ConfigurationException("No shadowRef OID in " + desc + " and resolution frequency set to NEVER");
        }

        QName objectClassName =
                QNameUtil.qualifyIfNeeded(
                        configNonNull(
                                resourceObjectReference.getObjectClass(),
                                "No object class name in object reference in %s", desc),
                        MidPointConstants.NS_RI);

        ProvisioningContext subCtx = ctx.spawnForObjectClassWithRawDefinition(objectClassName);
        subCtx.assertDefinition();

        ObjectQuery refQuery =
                ObjectQueryUtil.createQuery(
                        QueryConversionUtil.parseFilter(
                                resourceObjectReference.getFilter(), subCtx.getObjectDefinitionRequired()));
        // No variables. At least not now. We expect that mostly constants will be used here.
        VariablesMap variables = new VariablesMap();
        ObjectQuery evaluatedRefQuery =
                ExpressionUtil.evaluateQueryExpressions(
                        refQuery, variables, MiscSchemaUtil.getExpressionProfile(), expressionFactory,
                        desc, ctx.getTask(), result);

        ObjectFilter completeFilter = prismContext.queryFactory().createAnd(
                ObjectQueryUtil.createResourceAndObjectClassFilter(ctx.getResource().getOid(), objectClassName),
                evaluatedRefQuery.getFilter());

        ObjectQuery completeQuery = prismContext.queryFactory().createQuery(completeFilter);

        // TODO: implement "repo" search strategies, don't forget to apply definitions

        Holder<PrismObject<ShadowType>> shadowHolder = new Holder<>();
        ResultHandler<ShadowType> handler = (shadow, objResult) -> {
            if (shadowHolder.getValue() != null) {
                throw new IllegalStateException("More than one search results for " + desc);
            }
            shadowHolder.setValue(shadow);
            return true;
        };

        shadowsFacade.searchObjectsIterative(subCtx, completeQuery, null, handler, result);

        // TODO: implement storage of OID (ONCE search frequency)

        return shadowHolder.getValue().asObjectable();
    }

    /**
     * Resolve primary identifier from a collection of identifiers that may contain only secondary identifiers.
     *
     * We accept also dead shadows, but only if there is only one. (This is a bit inconsistent, should be fixed somehow.)
     * Actually, we could be more courageous, and reject dead shadows altogether, as we use the result for object fetching;
     * but there is a theoretical chance that the shadow is dead in the repo but alive on the resource.
     */
    ResourceObjectIdentification.WithPrimary resolvePrimaryIdentifier(
            ProvisioningContext ctx, ResourceObjectIdentification<?> identification, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (identification == null) {
            return null;
        }
        if (identification instanceof ResourceObjectIdentification.WithPrimary primary) {
            return primary;
        }
        ShadowType repoShadow = shadowFinder.lookupShadowByAnyIdentifier(ctx, identification.getSecondaryIdentifiers(), result);
        if (repoShadow == null) {
            // TODO: we could attempt resource search here
            throw new ObjectNotFoundException(
                    "No repository shadow for %s, cannot resolve identifiers (%s)".formatted(
                            identification, ctx.getExceptionDescription()),
                    ShadowType.class,
                    null);
        }

        var shadowCtx = ctx.applyAttributesDefinition(repoShadow);

        ResourceObjectDefinition objDef = shadowCtx.getObjectDefinitionRequired();
        ResourceObjectIdentifier.Primary<?> primaryIdentifier =
                ResourceObjectIdentifiers.of(objDef, repoShadow)
                        .getPrimaryIdentifierRequired();

        LOGGER.trace("Resolved {} to {} (object class {})", identification, primaryIdentifier, objDef);

        // The secondary identifiers may be different between the fetched shadow and original values provided by client.
        // Let us ignore that for now, and provide the original values with the resolved primary identifier.
        return identification.withPrimary(primaryIdentifier);
    }
}
