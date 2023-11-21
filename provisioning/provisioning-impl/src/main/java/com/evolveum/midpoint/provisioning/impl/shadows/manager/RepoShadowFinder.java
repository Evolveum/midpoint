/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil.*;
import static com.evolveum.midpoint.schema.GetOperationOptions.updateToDistinct;
import static com.evolveum.midpoint.schema.GetOperationOptions.zeroStalenessOptions;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
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
public class RepoShadowFinder {

    private static final Trace LOGGER = TraceManager.getTrace(RepoShadowFinder.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    /** Simply gets a repo shadow from the repository. No magic here. */
    public @NotNull PrismObject<ShadowType> getShadow(@NotNull String oid, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(ShadowType.class, oid, null, result);
    }

    /** A convenience method. */
    public @NotNull ShadowType getShadowBean(@NotNull String oid, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return getShadow(oid, result)
                .asObjectable();
    }

    /** A convenience method. */
    public @NotNull RepoShadow getRepoShadow(
            @NotNull ProvisioningContext ctx, @NotNull String oid, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return getRepoShadow(ctx, oid, null, result);
    }

    /** A convenience method. */
    public @NotNull RepoShadow getRepoShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return ctx.adoptRepoShadow(
                getShadowBean(oid, options, result));
    }

    public @NotNull ShadowType getShadowBean(
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) throws ObjectNotFoundException, SchemaException {
        return repositoryService
                .getObject(ShadowType.class, oid, options, result)
                .asObjectable();
    }

    /** Iteratively searches for shadows in the repository. No magic except for handling matching rules. */
    public SearchResultMetadata searchShadowsIterative(
            ProvisioningContext ctx,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ResultHandler<ShadowType> repoHandler,
            OperationResult result) throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.searchObjectsIterative(
                ShadowType.class, repoQuery, repoHandler, options, true, result);
    }

    /** Non-iteratively searches for shadows in the repository. No magic except for handling matching rules. */
    public SearchResultList<PrismObject<ShadowType>> searchShadows(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.searchObjects(ShadowType.class, repoQuery, options, parentResult);
    }

    /** Simply counts the shadows in repository. No magic except for handling matching rules. */
    public int countShadows(
            ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.countObjects(ShadowType.class, repoQuery, options, result);
    }

    /**
     * Looks up live (or any other, if there's none) shadow by primary identifier(s).
     *
     * This method, unlike many others in this class, accepts a wildcard context, assuming that the primary identifiers
     * are the same for all object classes, and that the identifier values are unique across object classes (!)
     *
     * This method is not inlined to keep the consistency with
     * {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)}.
     */
    public @Nullable RepoShadow lookupLiveOrAnyShadowByPrimaryId(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectIdentifier.Primary<?> primaryIdentifier,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException {
        return selectLiveOrAnyShadow(
                ctx,
                searchShadowsByPrimaryId(ctx, primaryIdentifier, result));
    }

    /**
     * Returns live shadow (if there's one) or any of the dead ones.
     * (We ignore the possibility of conflicting information in shadows.)
     */
    private static RepoShadow selectLiveOrAnyShadow(ProvisioningContext ctx, List<PrismObject<ShadowType>> shadows)
            throws SchemaException, ConfigurationException {
        RepoShadow liveShadow = RepoShadow.selectLiveShadow(ctx, shadows, "");
        if (liveShadow != null) {
            return liveShadow;
        } else if (shadows.isEmpty()) {
            return null;
        } else {
            return ctx.adoptRepoShadow(shadows.get(0));
        }
    }

    /**
     * Looks up a live shadow by primary identifier.
     * Unlike {@link #lookupShadowByIndexedPrimaryIdValue(ProvisioningContext, String, OperationResult)} this method
     * uses stored attributes to execute the query.
     *
     * @param objectClass Intentionally not taken from the context - yet. See the explanation in
     * {@link com.evolveum.midpoint.provisioning.impl.shadows.ShadowAcquisition#objectClass}.
     */
    public @Nullable RepoShadow lookupLiveShadowByPrimaryId(
            ProvisioningContext ctx, PrismProperty<?> primaryIdentifier, QName objectClass, OperationResult result)
            throws SchemaException, ConfigurationException {

        ObjectQuery query = createQueryByPrimaryId(ctx, primaryIdentifier, objectClass);

        LOGGER.trace("Searching for shadow by primary identifier using query:\n{}", query.debugDumpLazily(1));
        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, zeroStalenessOptions(), result); // no caching!
        LOGGER.trace("Found {} shadows (live or dead)", shadowsFound.size());

        RepoShadow liveShadow =
                RepoShadow.selectLiveShadow(ctx, shadowsFound, "when looking by primary identifier " + primaryIdentifier);
        checkConsistency(liveShadow);
        return liveShadow;
    }

    private @NotNull List<PrismObject<ShadowType>> searchShadowsByPrimaryId(
            ProvisioningContext ctx, ResourceObjectIdentifier.Primary<?> primaryIdentifier, OperationResult result)
            throws SchemaException {
        return searchRepoShadows(
                createQueryBySelectedAttributes(ctx, List.of(primaryIdentifier.getAttribute())),
                null,
                result);
    }

    /**
     * Looks up a shadow by primary identifier value.
     *
     * Unlike {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)} this method
     * queries directly the shadow.primaryIdentifierValue property. (And does not ask for shadow liveness.)
     */
    public @Nullable RepoShadow lookupShadowByIndexedPrimaryIdValue(
            ProvisioningContext ctx, String primaryIdentifierValue, OperationResult result)
            throws SchemaException, ConfigurationException {

        ObjectQuery query = createQueryByPrimaryIdValue(ctx, primaryIdentifierValue);
        LOGGER.trace("Searching for shadow by primaryIdentifierValue using filter:\n{}", query.debugDumpLazily(1));

        return RepoShadow.selectSingleShadow(
                ctx,
                searchRepoShadows(query, zeroStalenessOptions(), result), // zero staleness = no caching!
                lazy(() -> "primary identifier value " + primaryIdentifierValue + " (impossible because of DB constraint)"));
    }

    /**
     * Looks up a live shadow by provided attributes (all must match).
     * Usually these are identifiers from an association value.
     * They must have proper definitions applied.
     */
    public @Nullable RepoShadow lookupLiveShadowByAllAttributes(
            ProvisioningContext ctx, Collection<? extends ResourceAttribute<?>> attributes, OperationResult result)
            throws SchemaException, ConfigurationException {

        ObjectQuery query = createQueryBySelectedAttributes(ctx, attributes);
        LOGGER.trace("Searching for shadow using filter (repo):\n{}", query.debugDumpLazily());

        List<PrismObject<ShadowType>> shadows = searchRepoShadows(query, null, result);

        RepoShadow singleShadow =
                RepoShadow.selectLiveShadow(ctx, shadows, "when looking by attributes: " + attributes);
        checkConsistency(singleShadow);
        return singleShadow;
    }

    /**
     * Returns dead shadows "compatible" (having the same primary identifier) as given shadow that is to be added.
     */
    public @NotNull Collection<PrismObject<ShadowType>> searchForPreviousDeadShadows(
            ProvisioningContext ctx, ResourceObject objectToAdd, OperationResult result)
            throws SchemaException {

        PrismProperty<?> identifier = objectToAdd.getSingleValuedPrimaryIdentifier();
        if (identifier == null) {
            LOGGER.trace("No primary identifier. So there are obviously no relevant previous dead shadows.");
            return List.of();
        }

        ObjectQuery query = createQueryByPrimaryId(ctx, identifier, objectToAdd.getObjectClass());
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

        ResourceObjectDefinition objDef = ctx.getObjectDefinitionRequired();

        // TODO why do we create OR query here? What about the performance?
        S_FilterEntry q = prismContext.queryFor(ShadowType.class)
                .block();
        for (ResourceObjectIdentifier<?> identifier : secondaryIdentifiers) {
            q = q.item(identifier.getSearchPath(), identifier.getDefinition())
                    .eq(identifier.getNormalizedValues())
                    .or();
        }
        ObjectQuery query = q.none().endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(objDef.getTypeName())
                .build();
        LOGGER.trace("Searching for shadow using filter on secondary identifiers:\n{}", query.debugDumpLazily());

        // TODO: check for errors
        return searchRepoShadows(query, null, result);
    }

    private void checkConsistency(RepoShadow shadow) {
        ProvisioningUtil.checkShadowActivationConsistency(shadow);
    }

    private @NotNull ObjectQuery createQueryByPrimaryId(
            @NotNull ProvisioningContext ctx, @NotNull PrismProperty<?> identifier, @NotNull QName objectClass)
            throws SchemaException {
        try {
            // If the query is to be used against the repository, we should not provide matching rules here. See MID-5547.
            PrismPropertyDefinition<?> def = identifier.getDefinition();
            return prismContext.queryFor(ShadowType.class)
                    .itemWithDef(def, ShadowType.F_ATTRIBUTES, def.getItemName())
                    .eq(getNormalizedValues(identifier, ctx.getObjectDefinitionRequired()))
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
                    .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                    .build();
        } catch (SchemaException e) {
            throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
        }
    }

    private @NotNull ObjectQuery createQueryByPrimaryIdValue(ProvisioningContext ctx, String primaryIdentifierValue) {
        return prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).eq(primaryIdentifierValue)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(ctx.getObjectClassNameRequired())
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                .build();
    }

    /** Constructs a query that matches all provided attributes (there must be at least one, definitions must be present). */
    private ObjectQuery createQueryBySelectedAttributes(
            ProvisioningContext ctx, Collection<? extends ResourceAttribute<?>> attributes)
            throws SchemaException {

        Preconditions.checkArgument(
                !attributes.isEmpty(), "Attributes to search by must not be empty for %s", ctx);

        var objectDefinition = ctx.getObjectDefinition();

        S_FilterEntry q = prismContext.queryFor(ShadowType.class);
        if (objectDefinition != null) {
            q = q.item(ShadowType.F_OBJECT_CLASS).eq(objectDefinition.getTypeName()).and();
        }
        for (ResourceAttribute<?> attribute : attributes) {
            var attrDef = MiscUtil.argNonNull(attribute.getDefinition(), "No definition for %s", attribute);
            q = q.itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getItemName())
                    .eq(getNormalizedAttributeValue(attribute.getValue(), attrDef))
                    .and();
        }
        return q.item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid()).build();
    }

    private @NotNull List<PrismObject<ShadowType>> searchRepoShadows(
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {
        return repositoryService.searchObjects(
                ShadowType.class,
                query,
                updateToDistinct(options),
                result);
    }
}
