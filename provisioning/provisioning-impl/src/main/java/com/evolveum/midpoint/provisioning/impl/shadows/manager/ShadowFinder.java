/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import static java.util.Collections.emptyList;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectLiveShadow;
import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import org.jetbrains.annotations.NotNull;
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
 * - lookup: returns a single shadow
 * - search: returns a collection of shadows
 *
 * There should be no side effects of methods here.
 */
@Component
public class ShadowFinder {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowFinder.class);

    private static final String OP_HANDLE_OBJECT_FOUND = ShadowFinder.class.getName() + "." + HANDLE_OBJECT_FOUND;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaService schemaService;

    /** Simply gets a repo shadow from the repository. No magic here. No side effects. */
    public PrismObject<ShadowType> getShadow(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(ShadowType.class, oid, null, result);
    }

    /** Iteratively searches for shadows in the repository. No magic except for handling matching rules. No side effects. */
    public SearchResultMetadata searchShadowsIterative(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<ShadowType> repoHandler,
            OperationResult result) throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.searchObjectsIterative(
                ShadowType.class, repoQuery,
                repoHandler.providingOwnOperationResult(OP_HANDLE_OBJECT_FOUND),
                options, true, result);
    }

    /** Non-iteratively searches for shadows in the repository. No magic except for handling matching rules. No side effects. */
    public SearchResultList<PrismObject<ShadowType>> searchShadows(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.searchObjects(ShadowType.class, repoQuery, options, parentResult);
    }

    /** Simply counts the shadows in repository. No magic except for handling matching rules. No side effects. */
    public int countShadows(ProvisioningContext ctx, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws SchemaException {
        ObjectQuery repoQuery = normalizeQueryValues(query, ctx.getObjectDefinition());
        return repositoryService.countObjects(ShadowType.class, repoQuery, options, result);
    }

    /** Looks up live (or any other, if there's none) shadow by primary identifier(s). Side effects: none. */
    public ShadowType lookupLiveOrAnyShadowByPrimaryIds(
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
    public ShadowType lookupLiveShadowByPrimaryId(
            ProvisioningContext ctx, PrismProperty<?> primaryIdentifier, QName objectClass, OperationResult result)
            throws SchemaException {

        ObjectQuery query = createQueryByPrimaryId(ctx, primaryIdentifier, objectClass);

        LOGGER.trace("Searching for shadow by primary identifier using query:\n{}", query.debugDumpLazily(1));
        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, zeroStalenessOptions(), result);
        LOGGER.trace("Found {} shadows (live or dead)", shadowsFound.size());

        PrismObject<ShadowType> liveShadow =
                selectLiveShadow(shadowsFound, "when looking by primary identifier " + primaryIdentifier);
        checkConsistency(liveShadow);
        return asObjectable(liveShadow);
    }

    /** Side effects: none. */
    private List<PrismObject<ShadowType>> searchShadowsByPrimaryIds(ProvisioningContext ctx,
            Collection<ResourceAttribute<?>> identifiers, OperationResult result)
            throws SchemaException, ConfigurationException {
        ObjectQuery query = createQueryBySelectedIds(ctx, identifiers, true);
        try {
            return searchRepoShadows(query, null, result);
        } catch (SchemaException ex) {
            throw ex.wrap("Failed to search shadow according to the primary identifiers in: " + identifiers);
        }
    }

    /**
     * Looks up a shadow with given secondary identifiers (any one must match).
     * If there are no secondary identifiers, null is returned.
     * If there is no matching shadow, null is returned.
     * If there are more matching shadows, an exception is thrown.
     *
     * Side effects: none.
     *
     * Unlike {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)} this method
     * queries directly the shadow.primaryIdentifierValue property. (And does not ask for shadow liveness.)
     */
    public PrismObject<ShadowType> lookupShadowByIndexedPrimaryIdValue(
            ProvisioningContext ctx, String primaryIdentifierValue, OperationResult result) throws SchemaException {

        ObjectQuery query = createQueryByPrimaryIdValue(ctx, primaryIdentifierValue);
        LOGGER.trace("Searching for shadow by primaryIdentifierValue using filter:\n{}", query.debugDumpLazily(1));

        return ProvisioningUtil.selectSingleShadow(
                searchRepoShadows(query, zeroStalenessOptions(), result),
                lazy(() -> "primary identifier value " + primaryIdentifierValue + " (impossible because of DB constraint)"));
    }

    private Collection<SelectorOptions<GetOperationOptions>> zeroStalenessOptions() {
        return schemaService.getOperationOptionsBuilder()
                .staleness(0L) // Explicitly avoid all caches. We want to avoid shadow duplication.
                .build();
    }

    /** Looks up a live shadow by all available identifiers (all must match). Side effects: none. */
    public ShadowType lookupLiveShadowByAllIds(
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
    public Collection<PrismObject<ShadowType>> searchForPreviousDeadShadows(
            ProvisioningContext ctx, ShadowType shadowToAdd, OperationResult result)
            throws SchemaException {

        PrismProperty<?> identifier = ProvisioningUtil.getSingleValuedPrimaryIdentifier(shadowToAdd);
        if (identifier == null) {
            LOGGER.trace("No primary identifier. So there are obviously no relevant previous dead shadows.");
            return emptyList();
        }

        ObjectQuery query = createQueryByPrimaryId(ctx, identifier, shadowToAdd.getObjectClass());
        LOGGER.trace("Searching for dead shadows using filter:\n{}", query.debugDumpLazily(1));

        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, zeroStalenessOptions(), result);
        List<PrismObject<ShadowType>> deadShadowsFound = shadowsFound.stream()
                .filter(shadowFound -> Boolean.TRUE.equals(shadowFound.asObjectable().isDead()))
                .collect(Collectors.toList());

        LOGGER.trace("looking for previous dead shadows, found {} objects. Dead among them: {}",
                shadowsFound.size(), deadShadowsFound.size());
        return deadShadowsFound;
    }

    public PrismObject<ShadowType> lookupShadowBySecondaryIds(ProvisioningContext ctx,
            Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult result)
            throws SchemaException {
        List<PrismObject<ShadowType>> shadows = searchShadowsBySecondaryIds(ctx, secondaryIdentifiers, result);
        return ProvisioningUtil.selectSingleShadowRelaxed(shadows, lazy(() -> "secondary identifiers: " + secondaryIdentifiers));
    }

    private List<PrismObject<ShadowType>> searchShadowsBySecondaryIds(
            ProvisioningContext ctx, Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult result)
            throws SchemaException {

        if (secondaryIdentifiers.isEmpty()) {
            LOGGER.trace("Shadow does not contain secondary identifier. Skipping lookup shadows according to it.");
            return emptyList();
        }

        S_FilterEntry q = prismContext.queryFor(ShadowType.class)
                .block();
        for (ResourceAttribute<?> secondaryIdentifier : secondaryIdentifiers) {
            // There may be identifiers that come from associations and they will have parent set to association/identifiers
            // For the search to succeed we need all attribute to have "attributes" parent path.
            secondaryIdentifier = ShadowUtil.fixAttributePath(secondaryIdentifier);
            q = q.item(secondaryIdentifier.getPath(), secondaryIdentifier.getDefinition())
                    .eq(getNormalizedValues(secondaryIdentifier, ctx.getObjectDefinitionRequired()))
                    .or();
        }
        ObjectQuery query = q.none().endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
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

        boolean identifierFound = false;

        S_FilterEntry q = prismContext.queryFor(ShadowType.class);

        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinition();
        for (PrismProperty<?> identifier : identifiers) {
            ResourceAttributeDefinition<?> rAttrDef;
            PrismPropertyValue<?> identifierValue = identifier.getValue();
            if (objectDefinition == null) {
                // If there is no specific object class definition then the identifier definition
                // must be the same in all object classes and that means that we can use
                // definition from any of them.
                ResourceObjectTypeDefinition anyDefinition = ctx.getResourceSchema().getObjectTypeDefinitions().iterator().next();
                rAttrDef = anyDefinition.findAttributeDefinition(identifier.getElementName());
                if (primaryIdentifiersOnly && !anyDefinition.isPrimaryIdentifier(identifier.getElementName())) {
                    continue;
                }
            } else {
                if (primaryIdentifiersOnly && !objectDefinition.isPrimaryIdentifier(identifier.getElementName())) {
                    continue;
                }
                rAttrDef = objectDefinition.findAttributeDefinition(identifier.getElementName());
            }

            if (rAttrDef == null) {
                throw new SchemaException("No definition for " + identifier.getElementName());
            }
            String normalizedIdentifierValue = (String) getNormalizedAttributeValue(identifierValue, rAttrDef);
            //noinspection unchecked
            PrismPropertyDefinition<String> def = (PrismPropertyDefinition<String>) identifier.getDefinition();
            q = q.itemWithDef(def, ShadowType.F_ATTRIBUTES, def.getItemName()).eq(normalizedIdentifierValue).and();
            identifierFound = true;
        }

        if (!identifierFound) {
            throw new SchemaException("Identifiers not found. Cannot create search query by identifier.");
        }

        if (objectDefinition != null) {
            q = q.item(ShadowType.F_OBJECT_CLASS).eq(objectDefinition.getTypeName()).and();
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
                GetOperationOptions.updateToDistinct(options),
                result);
    }
}
