/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType.*;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.schema.processor.ShadowQueryConversionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifier;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
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
     *
     * The context should be a wildcard one.
     */
    @Nullable ShadowType resolveUsingRawClass(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectReferenceType resourceObjectReference,
            @NotNull String desc,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ctx.assertWildcard();

        ObjectReferenceType shadowRef = resourceObjectReference.getShadowRef();
        ResourceObjectReferenceResolutionFrequencyType resolutionFrequency =
                Objects.requireNonNullElse(resourceObjectReference.getResolutionFrequency(), ONCE);

        String shadowOid = getOid(shadowRef);
        if (shadowOid != null) {
            if (resolutionFrequency != ALWAYS) {
                return shadowFinder.getRepoShadow(ctx, shadowOid, result).getBean();
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

        ProvisioningContext subCtx = ctx.spawnForObjectClassWithClassDefinition(objectClassName);
        subCtx.assertDefinition();

        ObjectQuery refQuery =
                ObjectQueryUtil.createQuery(
                        ShadowQueryConversionUtil.parseFilter(
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

        shadowsFacade.searchShadowsIterative(subCtx, completeQuery, null, handler, result);

        // TODO: implement storage of OID (ONCE search frequency)

        return asObjectable(shadowHolder.getValue());
    }

    /**
     * Resolve primary identifier from a collection of identifiers that may contain only secondary identifiers.
     *
     * We accept also dead shadows, but only if there is only one.
     * Actually, we could be more courageous, and reject dead shadows altogether, as we use the result for object fetching;
     * but there is a theoretical chance that the shadow is dead in the repo but alive on the resource.
     */
    @NotNull ResourceObjectIdentification.WithPrimary resolvePrimaryIdentifier(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification<?> identification,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        if (identification instanceof ResourceObjectIdentification.WithPrimary primary) {
            return primary;
        } else if (identification instanceof ResourceObjectIdentification.SecondaryOnly secondaryOnly) {
            // FIXME what if there are proposed shadows here (i.e., multiple ones + without a primary identifier?)
            var repoShadows = shadowFinder.searchShadowsByAnySecondaryIdentifier(ctx, secondaryOnly, result);
            var repoShadow = selectSingleShadow(ctx, repoShadows, lazy(() -> "while resolving " + secondaryOnly));
            if (repoShadow == null) {
                // TODO: we could attempt resource search here
                throw new ObjectNotFoundException(
                        "No repository shadow for %s, cannot resolve identifiers (%s)".formatted(
                                identification, ctx.getExceptionDescription()),
                        ShadowType.class,
                        null);
            }
            var shadowCtx = ctx.applyDefinitionInNewCtx(repoShadow);

            ResourceObjectIdentifier.Primary<?> primaryIdentifier =
                    ResourceObjectIdentifiers.of(shadowCtx.getObjectDefinitionRequired(), repoShadow.getBean())
                            .getPrimaryIdentifierRequired();

            LOGGER.trace("Resolved {} to {}", identification, primaryIdentifier);

            // The secondary identifiers may be different between the fetched shadow and original values provided by client.
            // Let us ignore that for now, and provide the original values with the resolved primary identifier.
            return identification.withPrimaryAdded(primaryIdentifier);
        } else {
            throw new AssertionError(identification);
        }
    }

    /**
     * Select single live shadow but allows the existence of multiple dead shadows
     * (if no single live shadow exists). Not very nice! Transitional solution until better one is found.
     *
     * @see RawRepoShadow#selectLiveShadow(List, Object)
     * @see ShadowFinder#selectSingleShadow(ProvisioningContext, List, Object)
     */
    private static @Nullable RepoShadow selectSingleShadow(
            @NotNull ProvisioningContext ctx, @NotNull List<PrismObject<ShadowType>> shadows, Object context)
            throws SchemaException, ConfigurationException {
        var singleLive = RawRepoShadow.selectLiveShadow(shadows, context);
        if (singleLive != null) {
            return ctx.adoptRawRepoShadow(singleLive);
        }

        // all remaining shadows (if any) are dead
        if (shadows.isEmpty()) {
            return null;
        } else if (shadows.size() > 1) {
            LOGGER.error("Cannot select from {} dead shadows {}:\n{}", shadows.size(), context, DebugUtil.debugDump(shadows));
            throw new IllegalStateException("More than one [dead] shadow for " + context);
        } else {
            return ctx.adoptRawRepoShadow(shadows.get(0));
        }
    }
}
