/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil.*;
import static com.evolveum.midpoint.schema.GetOperationOptions.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectLiveShadow;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
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
 * There should be no side effects of methods here.
 *
 * Ideally, all read accesses related to the repository should go through this class.
 * (Exceptions where appropriate.)
 */
@Component
public class ShadowFinder {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowFinder.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    /** Simply gets a repo shadow from the repository. No magic here. No side effects. */
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
    public @NotNull ShadowType getShadowBean(
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return repositoryService
                .getObject(ShadowType.class, oid, options, result)
                .asObjectable();
    }

    /** Iteratively searches for shadows in the repository. No magic except for handling matching rules. No side effects. */
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

    /** Non-iteratively searches for shadows in the repository. No magic except for handling matching rules. No side effects. */
    public SearchResultList<PrismObject<ShadowType>> searchShadows(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.searchObjects(ShadowType.class, repoQuery, options, parentResult);
    }

    /** Simply counts the shadows in repository. No magic except for handling matching rules. No side effects. */
    public int countShadows(
            ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.countObjects(ShadowType.class, repoQuery, options, result);
    }

    /**
     * Looks up live (or any other, if there's none) shadow by primary identifier(s). Side effects: none.
     *
     * This method, unlike many others in this class, accepts a wildcard context, assuming that the primary identifiers
     * are the same for all object classes.
     *
     * This method is not inlined to keep the consistency with
     * {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)}.
     */
    public @Nullable ShadowType lookupLiveOrAnyShadowByPrimaryIds(
            ProvisioningContext ctx, Collection<ResourceAttribute<?>> identifiers, OperationResult result)
            throws SchemaException, ConfigurationException {
        return ProvisioningUtil.selectLiveOrAnyShadow(
                searchShadowsByPrimaryIds(ctx, identifiers, result));
    }

    /**
     * Looks up a live shadow by primary identifier.
     * Unlike {@link #lookupShadowByIndexedPrimaryIdValue(ProvisioningContext, String, OperationResult)} this method
     * uses stored attributes to execute the query.
     *
     * Side effects: none.
     *
     * @param objectClass Intentionally not taken from the context - yet. See the explanation in
     * {@link com.evolveum.midpoint.provisioning.impl.shadows.ShadowAcquisition#objectClass}.
     */
    public @Nullable ShadowType lookupLiveShadowByPrimaryId(
            ProvisioningContext ctx, PrismProperty<?> primaryIdentifier, QName objectClass, OperationResult result)
            throws SchemaException {

        ObjectQuery query = createQueryByPrimaryId(ctx, primaryIdentifier, objectClass);

        LOGGER.trace("Searching for shadow by primary identifier using query:\n{}", query.debugDumpLazily(1));
        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, zeroStalenessOptions(), result); // no caching!
        LOGGER.trace("Found {} shadows (live or dead)", shadowsFound.size());

        PrismObject<ShadowType> liveShadow =
                selectLiveShadow(shadowsFound, "when looking by primary identifier " + primaryIdentifier);
        checkConsistency(liveShadow);
        return asObjectable(liveShadow);
    }

    /** Side effects: none. */
    private @NotNull List<PrismObject<ShadowType>> searchShadowsByPrimaryIds(
            ProvisioningContext ctx, Collection<ResourceAttribute<?>> identifiers, OperationResult result)
            throws SchemaException, ConfigurationException {
        ObjectQuery query = createQueryBySelectedIds(ctx, identifiers, true);
        try {
            return searchRepoShadows(query, null, result);
        } catch (SchemaException ex) {
            throw ex.wrap("Failed to search shadow according to the primary identifiers in: " + identifiers);
        }
    }

    /**
     * Looks up a shadow by primary identifier value.
     *
     * Unlike {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)} this method
     * queries directly the shadow.primaryIdentifierValue property. (And does not ask for shadow liveness.)
     */
    public @Nullable ShadowType lookupShadowByIndexedPrimaryIdValue(
            ProvisioningContext ctx, String primaryIdentifierValue, OperationResult result) throws SchemaException {

        ObjectQuery query = createQueryByPrimaryIdValue(ctx, primaryIdentifierValue);
        LOGGER.trace("Searching for shadow by primaryIdentifierValue using filter:\n{}", query.debugDumpLazily(1));

        return ProvisioningUtil.selectSingleShadow(
                searchRepoShadows(query, zeroStalenessOptions(), result), // zero staleness = no caching!
                lazy(() -> "primary identifier value " + primaryIdentifierValue + " (impossible because of DB constraint)"));
    }

    /** Looks up a live shadow by all available identifiers (all must match). Side effects: none. */
    public @Nullable ShadowType lookupLiveShadowByAllIds(
            ProvisioningContext ctx, ResourceAttributeContainer identifierContainer, OperationResult result)
            throws SchemaException, ConfigurationException {

        //noinspection rawtypes,unchecked
        ObjectQuery query = createQueryBySelectedIds(
                ctx, (Collection) identifierContainer.getValue().getItems(), false);
        LOGGER.trace("Searching for shadow using filter (repo):\n{}", query.debugDumpLazily());

        List<PrismObject<ShadowType>> shadows = searchRepoShadows(query, null, result);

        PrismObject<ShadowType> singleShadow =
                ProvisioningUtil.selectLiveShadow(shadows, "when looking by IDs: " + identifierContainer);
        checkConsistency(singleShadow);
        return asObjectable(singleShadow);
    }

    /**
     * Returns dead shadows "compatible" (having the same primary identifier) as given shadow that is to be added.
     * Side effects: none.
     */
    public @NotNull Collection<PrismObject<ShadowType>> searchForPreviousDeadShadows(
            ProvisioningContext ctx, ShadowType objectToAdd, OperationResult result)
            throws SchemaException {

        PrismProperty<?> identifier = ProvisioningUtil.getSingleValuedPrimaryIdentifier(objectToAdd);
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

    /**
     * Looks up a shadow with given identifiers (any one can match - i.e. OR clause is created).
     * If there are no identifiers, null is returned.
     * If there is no matching shadow, null is returned.
     * If there are more matching shadows, an exception is thrown.
     *
     * Side effects: none.
     */
    public @Nullable ShadowType lookupShadowByAnyIdentifier(
            ProvisioningContext ctx,
            Collection<? extends ResourceObjectIdentifier> identifiers,
            OperationResult result)
            throws SchemaException {
        List<PrismObject<ShadowType>> shadows = searchShadowsByAnyIdentifier(ctx, identifiers, result);
        return ProvisioningUtil.selectSingleShadowRelaxed(shadows, lazy(() -> "identifiers: " + identifiers));
    }

    /** Special conditions here, see {@link #lookupShadowByAnyIdentifier(ProvisioningContext, Collection, OperationResult)} */
    private List<PrismObject<ShadowType>> searchShadowsByAnyIdentifier(
            ProvisioningContext ctx, Collection<? extends ResourceObjectIdentifier> identifiers, OperationResult result)
            throws SchemaException {

        if (identifiers.isEmpty()) {
            LOGGER.trace("No identifiers present. Not looking up shadows this way.");
            return List.of();
        }

        ResourceObjectDefinition objDef = ctx.getObjectDefinitionRequired();

        S_FilterEntry q = prismContext.queryFor(ShadowType.class)
                .block();
        for (ResourceObjectIdentifier identifier : identifiers) {
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

    private void checkConsistency(PrismObject<ShadowType> shadow) {
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

    private ObjectQuery createQueryBySelectedIds(
            ProvisioningContext ctx, Collection<ResourceAttribute<?>> identifiers, boolean primaryIdentifiersOnly)
            throws SchemaException, ConfigurationException {

        S_FilterEntry q = prismContext.queryFor(ShadowType.class);

        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinition();
        ResourceObjectDefinition effectiveDefinition;
        if (objectDefinition != null) {
            q = q.item(ShadowType.F_OBJECT_CLASS).eq(objectDefinition.getTypeName()).and();
            effectiveDefinition = objectDefinition;
        } else {
            // If there is no specific object class definition then we hope the identifier definition
            // is the same in all object classes and that means that we can use definition from any of them.
            // The situation here can occur e.g. for deletion changes without object class information (wildcard LS).
            effectiveDefinition = ctx.getAnyDefinition();
        }

        boolean identifierFound = false;
        for (PrismProperty<?> identifier : identifiers) {
            if (primaryIdentifiersOnly && !effectiveDefinition.isPrimaryIdentifier(identifier.getElementName())) {
                continue;
            }
            var attrDef = effectiveDefinition.findAttributeDefinitionRequired(identifier.getElementName());
            String normalizedIdentifierValue = (String) getNormalizedAttributeValue(identifier.getValue(), attrDef);
            //noinspection unchecked
            PrismPropertyDefinition<String> def = (PrismPropertyDefinition<String>) identifier.getDefinition();
            q = q.itemWithDef(def, ShadowType.F_ATTRIBUTES, def.getItemName()).eq(normalizedIdentifierValue).and();
            identifierFound = true;
        }

        if (!identifierFound) {
            throw new SchemaException("Identifiers not found. Cannot create search query by identifier.");
        }

        return q.item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid()).build();
    }

    /** No side effects. */
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
