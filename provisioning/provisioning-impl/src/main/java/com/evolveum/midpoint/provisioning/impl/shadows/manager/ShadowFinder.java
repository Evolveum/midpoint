/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.schema.processor.ShadowsNormalizationUtil.transformQueryValues;
import static com.evolveum.midpoint.schema.GetOperationOptions.zeroStalenessOptions;
import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.util.ObjectSet;
import com.evolveum.midpoint.schema.util.RawRepoShadow;

import com.evolveum.midpoint.util.DebugUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification.WithPrimary;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Takes care of finding shadows in the repository. This is one of public classes of this package.
 *
 * Naming:
 *
 * - get: returns a single shadow (by OID)
 * - lookup: returns a single shadow (by some other criteria)
 * - search: returns a collection of shadows
 *
 * There should be no side effects of methods (like creating a shadow if nothing is found) here.
 *
 * Ideally, all read accesses related to the repository should go through this class.
 * (Exceptions where appropriate.)
 */
@Component
public class ShadowFinder {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowFinder.class);

    @VisibleForTesting
    public static final String OP_HANDLE_OBJECT_FOUND = ShadowFinder.class.getName() + "." + HANDLE_OBJECT_FOUND;

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    /** A convenience method. */
    public @NotNull PrismObject<ShadowType> getShadow(@NotNull String oid, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return getShadowBean(oid, null, result)
                .asPrismObject();
    }

    /** Simply gets a repo shadow from the repository. No magic here. */
    private @NotNull ShadowType getShadowBean(
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) throws ObjectNotFoundException, SchemaException {
        return repositoryService
                .getObject(ShadowType.class, oid, options, result)
                .asObjectable();
    }

    /** A convenience method. */
    public @NotNull ShadowType getShadowBean(@NotNull String oid, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return getShadowBean(oid, null, result);
    }

    /** A convenience method. (The context is used mainly to provide resource information.) */
    public @NotNull RepoShadow getRepoShadow(
            @NotNull ProvisioningContext ctx, @NotNull String oid, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return getRepoShadow(ctx.toWildcard(), oid, null, result);
    }

    /** A convenience method. (The context is used mainly to provide resource information.) */
    public @NotNull RepoShadow getRepoShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return ctx.toWildcard().adoptRawRepoShadow(
                getShadowBean(oid, options, result));
    }

    /** A convenience method. */
    public @NotNull RawRepoShadow getRepoShadow(
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return RawRepoShadow.of(
                getShadowBean(oid, options, result));
    }

    /** Iteratively searches for shadows in the repository. No magic except for handling matching rules. */
    public SearchResultMetadata searchShadowsIterative(
            ProvisioningContext ctx,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ResultHandler<ShadowType> repoHandler,
            OperationResult result) throws SchemaException {
        ObjectQuery repoQuery = transformQueryValues(query, ctx.getObjectDefinitionRequired());
        LOGGER.trace("Searching shadows iteratively using transformed query:\n{}", DebugUtil.debugDumpLazily(repoQuery, 1));
        return repositoryService.searchObjectsIterative(
                ShadowType.class, repoQuery,
                repoHandler.providingOwnOperationResult(OP_HANDLE_OBJECT_FOUND),
                options, true, result);
    }

    /** Non-iteratively searches for shadows in the repository. No magic except for handling matching rules. */
    public SearchResultList<PrismObject<ShadowType>> searchShadows(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        ObjectQuery repoQuery = transformQueryValues(query, ctx.getObjectDefinitionRequired());
        LOGGER.trace("Searching shadows using transformed query:\n{}", DebugUtil.debugDumpLazily(repoQuery, 1));
        return repositoryService.searchObjects(ShadowType.class, repoQuery, options, parentResult);
    }

    /** Simply counts the shadows in repository. No magic except for handling matching rules. */
    public int countShadows(
            ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {
        ObjectQuery repoQuery = transformQueryValues(query, ctx.getObjectDefinitionRequired());
        LOGGER.trace("Counting shadows using transformed query:\n{}", DebugUtil.debugDumpLazily(repoQuery, 1));
        return repositoryService.countObjects(ShadowType.class, repoQuery, options, result);
    }

    /**
     * Looks up a live shadow by primary identifier.
     * Unlike {@link #lookupShadowByIndexedPrimaryIdValue(ProvisioningContext, String, OperationResult)} this method
     * uses stored attributes to execute the query.
     *
     * The `basicInfo` flag is used to control whether we need to fetch the full shadow or just basic information (useful for
     * optimizing searches for embedded shadows).
     *
     * TODO we could consider using the indexed primary ID value, as we are looking for live shadows anyway.
     *  But we need to think carefully about the border cases, like shadows not yet created on the resource.
     */
    public @Nullable RepoShadow lookupLiveRepoShadowByPrimaryId(
            ProvisioningContext ctx, WithPrimary identification, boolean basicInfo, OperationResult result)
            throws SchemaException, ConfigurationException {
        return executeLiveRepoShadowByPrimaryIdQuery(
                ctx,
                createQueryByPrimaryId(ctx, identification),
                basicInfo,
                "by primary identifier " + identification,
                result);
    }

    /**
     * A variant of {@link #lookupLiveRepoShadowByPrimaryId(ProvisioningContext, WithPrimary, boolean, OperationResult)}
     * where we don't know the object class. (Used e.g. for delete changes.)
     */
    public @Nullable RepoShadow lookupLiveRepoShadowByPrimaryIdWithoutObjectClass(
            ProvisioningContext ctx, ResourceObjectIdentifier.Primary<?> primaryIdentifier, OperationResult result)
            throws SchemaException, ConfigurationException {
        return executeLiveRepoShadowByPrimaryIdQuery(
                ctx,
                createQueryByPrimaryIdWithoutObjectClass(ctx, primaryIdentifier),
                false,
                "by primary identifier " + primaryIdentifier + " (without object class)",
                result);
    }

    private @Nullable RepoShadow executeLiveRepoShadowByPrimaryIdQuery(
            ProvisioningContext ctx, ObjectQuery query, boolean basicInfo, String context, OperationResult result)
            throws SchemaException, ConfigurationException {
        LOGGER.trace("Searching for shadow {} using query:\n{}", context, query.debugDumpLazily(1));
        var optionsBuilder = GetOperationOptionsBuilder.create()
                .staleness(0L); // no caching!
        if (basicInfo) {
            optionsBuilder = optionsBuilder
                    .item(ShadowType.F_OPERATION_EXECUTION)
                    .retrieve(RetrieveOption.EXCLUDE);
        }
        var shadowsFound = searchRepoShadows(query, optionsBuilder.build(), result);
        LOGGER.trace("Found {} shadows (live or dead)", shadowsFound.size());

        var rawRepoShadow = RawRepoShadow.selectLiveShadow(shadowsFound, context);
        if (rawRepoShadow != null) {
            return ctx.adoptRawRepoShadow(rawRepoShadow);
        } else {
            return null;
        }
    }

    /**
     * Looks up a shadow by primary identifier value.
     *
     * Unlike {@link #lookupLiveRepoShadowByPrimaryId(ProvisioningContext, WithPrimary, boolean, OperationResult)}, this method
     * queries directly the shadow.primaryIdentifierValue property. (And does not ask for shadow liveness.)
     */
    public @Nullable RepoShadow lookupShadowByIndexedPrimaryIdValue(
            ProvisioningContext ctx, String primaryIdentifierValue, OperationResult result)
            throws SchemaException, ConfigurationException {

        if (primaryIdentifierValue == null) {
            return null; // Just to avoid searching for all dead accounts
        }

        ObjectQuery query = createQueryByPrimaryIdValue(ctx, primaryIdentifierValue);
        LOGGER.trace("Searching for shadow by primaryIdentifierValue using filter:\n{}", query.debugDumpLazily(1));

        return selectSingleShadow(
                ctx,
                searchRepoShadows(query, zeroStalenessOptions(), result), // zero staleness = no caching!
                lazy(() -> "primary identifier value " + primaryIdentifierValue + " (impossible because of DB constraint)"));
    }

    private static @Nullable RepoShadow selectSingleShadow(
            @NotNull ProvisioningContext ctx, @NotNull List<PrismObject<ShadowType>> rawShadows, Object context)
            throws SchemaException, ConfigurationException {
        LOGGER.trace("Selecting from {} objects", rawShadows.size());

        if (rawShadows.isEmpty()) {
            return null;
        } else if (rawShadows.size() > 1) {
            LOGGER.error("Too many shadows ({}) for {}", rawShadows.size(), context);
            LOGGER.debug("Shadows:\n{}", DebugUtil.debugDumpLazily(rawShadows));
            throw new IllegalStateException("More than one shadow for " + context);
        } else {
            return ctx.adoptRawRepoShadow(rawShadows.get(0));
        }
    }

    /**
     * Looks up a live shadow by provided attributes (all must match).
     * Usually these are identifiers from an association value.
     * They must have proper definitions applied.
     */
    public @Nullable RepoShadow lookupLiveShadowByAllAttributes(
            ProvisioningContext ctx, Collection<? extends ShadowSimpleAttribute<?>> attributes, OperationResult result)
            throws SchemaException, ConfigurationException {

        ObjectQuery query = createQueryBySelectedAttributes(ctx, attributes);
        LOGGER.trace("Searching for shadow using filter (repo):\n{}", query.debugDumpLazily());

        List<PrismObject<ShadowType>> shadows = searchRepoShadows(query, null, result);

        RawRepoShadow rawRepoShadow =
                RawRepoShadow.selectLiveShadow(shadows, "when looking by attributes: " + attributes);
        if (rawRepoShadow != null) {
            return ctx.adoptRawRepoShadow(rawRepoShadow);
        } else {
            return null;
        }
    }

    /**
     * Returns dead shadows "compatible" (having the same primary identifier) as given shadow that is to be added.
     */
    public @NotNull Collection<PrismObject<ShadowType>> searchForPreviousDeadShadows(
            ProvisioningContext ctx, ResourceObjectShadow objectToAdd, OperationResult result)
            throws SchemaException {

        var primaryIdentification = objectToAdd.getPrimaryIdentification();
        if (primaryIdentification == null) {
            LOGGER.trace("No primary identifier. So there are obviously no relevant previous dead shadows.");
            return List.of();
        }

        ObjectQuery query = createQueryByPrimaryId(ctx, primaryIdentification);
        LOGGER.trace("Searching for dead shadows using filter:\n{}", query.debugDumpLazily(1));

        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, zeroStalenessOptions(), result); // no caching!
        List<PrismObject<ShadowType>> deadShadowsFound = shadowsFound.stream()
                .filter(shadowFound -> ShadowUtil.isDead(shadowFound))
                .toList();

        LOGGER.trace("looking for previous dead shadows, found {} objects. Dead among them: {}",
                shadowsFound.size(), deadShadowsFound.size());
        return deadShadowsFound;
    }

    /** Search shadows with given identifiers (any one can match - i.e. OR clause is created). May return dead ones. */
    public @NotNull List<PrismObject<ShadowType>> searchShadowsByAnySecondaryIdentifier(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentification.SecondaryOnly secondaryIdentification,
            @NotNull OperationResult result)
            throws SchemaException {

        var secondaryIdentifiers = secondaryIdentification.getSecondaryIdentifiers();

        // this is guaranteed; but double checking to avoid massive searches
        Preconditions.checkArgument(!secondaryIdentifiers.isEmpty());

        // TODO why do we create OR query here? What about the performance?
        S_FilterEntry q = prismContext.queryFor(ShadowType.class)
                .block();
        for (ResourceObjectIdentifier<?> identifier : secondaryIdentifiers) {
            q = q.filter(identifier.getAttribute().normalizationAwareEqFilter()).or();
        }
        ObjectQuery query = q.none().endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(secondaryIdentification.getObjectClassName())
                .build();
        LOGGER.trace("Searching for repo shadow using filter on secondary identifiers:\n{}", query.debugDumpLazily());

        // TODO: check for errors
        return searchRepoShadows(query, null, result);
    }

    private @NotNull ObjectQuery createQueryByPrimaryId(
            @NotNull ProvisioningContext ctx, @NotNull WithPrimary primaryIdentification) throws SchemaException {
        var identifier = primaryIdentification.getPrimaryIdentifier();
        return prismContext.queryFor(ShadowType.class)
                .filter(identifier.normalizationAwareEqFilter())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(primaryIdentification.getObjectClassName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                .build();
    }

    private @NotNull ObjectQuery createQueryByPrimaryIdWithoutObjectClass(
            @NotNull ProvisioningContext ctx, @NotNull ResourceObjectIdentifier.Primary<?> primaryIdentifier) throws SchemaException {
        return prismContext.queryFor(ShadowType.class)
                .filter(primaryIdentifier.normalizationAwareEqFilter())
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                .build();
    }

    private @NotNull ObjectQuery createQueryByPrimaryIdValue(ProvisioningContext ctx, String primaryIdentifierValue) {
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).eq(primaryIdentifierValue)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(ctx.getObjectClassNameRequired())
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                .build();
    }

    /**
     * Constructs a repo query that matches all provided attributes (there must be at least one, definitions must be present).
     * The attributes must be normalized according to their definition.
     *
     * Each of the attributes must either have exactly one value, or must be single-valued.
     */
    private ObjectQuery createQueryBySelectedAttributes(
            ProvisioningContext ctx, Collection<? extends ShadowSimpleAttribute<?>> attributes) throws SchemaException {

        Preconditions.checkArgument(
                !attributes.isEmpty(), "Attributes to search by must not be empty for %s", ctx);

        var objectDefinition = ctx.getObjectDefinition();

        S_FilterEntry q = prismContext.queryFor(ShadowType.class);
        if (objectDefinition != null) {
            q = q.item(ShadowType.F_OBJECT_CLASS).eq(objectDefinition.getTypeName()).and();
        }
        for (ShadowSimpleAttribute<?> attribute : attributes) {
            q = q.filter(attribute.normalizationAwareEqFilter()).and();
        }
        return q.item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid()).build();
    }

    /**
     * Here we avoid using `distinct` option in repo, and will do the distinct manually.
     * We assume that there are only a few results, as we use this method when looking by identifiers.
     */
    private @NotNull List<PrismObject<ShadowType>> searchRepoShadows(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result)
            throws SchemaException {
        var objects = repositoryService.searchObjects(ShadowType.class, query, options, result);
        return ObjectSet.ofPrismObjects(objects)
                .asPrismObjectList();
    }
}
