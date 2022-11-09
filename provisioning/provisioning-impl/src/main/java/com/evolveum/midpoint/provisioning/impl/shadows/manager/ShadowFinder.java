/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectLiveShadow;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Takes care of finding shadows in the repository.
 *
 * Intentionally not publicly visible. All access to this class should go through {@link ShadowManager}.
 *
 * Naming:
 *
 * - lookup: returns a single shadow
 * - search: returns a collection of shadows
 *
 * Usually does not modify anything. An exception: `markShadowExists` call in {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)}.
 * (Maybe there will be later similar actions also in other cases where we look for live shadows.)
 */
@Component
class ShadowFinder {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowFinder.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private Helper helper;
    @Autowired private SchemaService schemaService;
    @Autowired private ShadowUpdater shadowUpdater;

    /**
     * Object class name is intentionally not taken from ctx - yet.
     * See the explanation in {@link com.evolveum.midpoint.provisioning.impl.shadows.ShadowAcquisition#objectClass}.
     * Side effects: none.
     */
    ShadowType lookupLiveShadowByPrimaryId(
            ProvisioningContext ctx, PrismProperty<?> primaryIdentifier, QName objectClass, OperationResult result)
            throws SchemaException {

        ObjectQuery query = createQueryByPrimaryId(ctx, primaryIdentifier, objectClass);

        LOGGER.trace("Searching for shadow by primary identifier using query:\n{}", query.debugDumpLazily(1));
        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, zeroStalenessOptions(), result);
        LOGGER.trace("Found {} shadows (live or dead)", shadowsFound.size());

        PrismObject<ShadowType> liveShadow = selectLiveShadow(shadowsFound);
        checkConsistency(liveShadow);
        return asObjectable(liveShadow);
    }

    /** Side effects: none. */
    List<PrismObject<ShadowType>> searchShadowsByPrimaryIds(ProvisioningContext ctx,
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
     * Unlike {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)} this method
     * queries directly the shadow.primaryIdentifierValue property. (And does not ask for shadow liveness.)
     *
     * Side effects: none.
     */
    PrismObject<ShadowType> lookupShadowByIndexedPrimaryIdValue(
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

    ShadowType lookupLiveShadowByAllIds(
            ProvisioningContext ctx, ResourceAttributeContainer identifierContainer, OperationResult result)
            throws SchemaException, ConfigurationException {

        //noinspection rawtypes,unchecked
        ObjectQuery query = createQueryBySelectedIds(
                ctx, (Collection) identifierContainer.getValue().getItems(), false);
        LOGGER.trace("Searching for shadow using filter (repo):\n{}", query.debugDumpLazily());

        List<PrismObject<ShadowType>> shadows = searchRepoShadows(query, null, result);

        PrismObject<ShadowType> singleShadow = ProvisioningUtil.selectLiveShadow(shadows);
        checkConsistency(singleShadow);
        return asObjectable(singleShadow);
    }

    Collection<PrismObject<ShadowType>> searchForPreviousDeadShadows(
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

    PrismObject<ShadowType> lookupShadowBySecondaryIds(ProvisioningContext ctx,
            Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult result)
            throws SchemaException {
        List<PrismObject<ShadowType>> shadows = searchShadowsBySecondaryIds(ctx, secondaryIdentifiers, result);
        return ProvisioningUtil.selectSingleShadow(shadows, lazy(() -> "secondary identifiers: " + secondaryIdentifiers));
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
                    .eq(getNormalizedValue(secondaryIdentifier, ctx.getObjectDefinitionRequired()))
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

    private <T> List<PrismPropertyValue<T>> getNormalizedValue(PrismProperty<T> attr, ResourceObjectDefinition objDef)
            throws SchemaException {
        ResourceAttributeDefinition<?> attrDef = objDef.findAttributeDefinition(attr.getElementName());
        QName matchingRuleQName = requireNonNull(attrDef).getMatchingRuleQName();
        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, attrDef.getTypeName());
        List<PrismPropertyValue<T>> normalized = new ArrayList<>();
        for (PrismPropertyValue<T> origPValue : attr.getValues()) {
            T normalizedValue = matchingRule.normalize(origPValue.getValue());
            PrismPropertyValue<T> normalizedPValue = origPValue.clone();
            normalizedPValue.setValue(normalizedValue);
            normalized.add(normalizedPValue);
        }
        return normalized;
    }

    private @NotNull ObjectQuery createQueryByPrimaryId(
            @NotNull ProvisioningContext ctx, @NotNull PrismProperty<?> identifier, @NotNull QName objectClass)
            throws SchemaException {
        try {
            // If the query is to be used against the repository, we should not provide matching rules here. See MID-5547.
            PrismPropertyDefinition<?> def = identifier.getDefinition();
            return prismContext.queryFor(ShadowType.class)
                    .itemWithDef(def, ShadowType.F_ATTRIBUTES, def.getItemName())
                    .eq(getNormalizedValue(identifier, ctx.getObjectDefinitionRequired()))
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
            String normalizedIdentifierValue = (String) helper.getNormalizedAttributeValue(identifierValue, rAttrDef);
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
