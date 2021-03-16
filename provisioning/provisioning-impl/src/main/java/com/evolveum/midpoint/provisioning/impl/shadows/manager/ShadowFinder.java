/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectLiveShadow;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

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
     */
    PrismObject<ShadowType> lookupLiveShadowByPrimaryId(ProvisioningContext ctx, PrismProperty<?> primaryIdentifier,
            QName objectClass, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {

        ObjectQuery query = createQueryByPrimaryId(ctx, primaryIdentifier, objectClass);
        LOGGER.trace("Searching for shadow by primary identifier (attributes) using filter:\n{}", DebugUtil.debugDumpLazily(query, 1));

        // Explicitly avoid all caches. We want to avoid shadow duplication.
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .staleness(0L)
                .build();

        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, options, result);
        LOGGER.trace("Found {} shadows (live or dead)", shadowsFound.size());

        PrismObject<ShadowType> liveShadow = selectLiveShadow(shadowsFound);
        if (liveShadow == null) {
            return null;
        }

        if (!checkExistsFlagForLiveShadow(liveShadow, result)) {
            return null;
        }

        checkConsistency(liveShadow);
        return liveShadow;
    }

    /** @return true if the shadow is OK; false if it does not exist any more */
    private boolean checkExistsFlagForLiveShadow(PrismObject<ShadowType> liveShadow, OperationResult result)
            throws SchemaException {

        assert ShadowUtil.isNotDead(liveShadow);

        if (!ShadowUtil.isExists(liveShadow)) {
            // This is where gestation quantum state collapses.
            // Or maybe the account was created and we have found it before the original thread could mark the shadow as alive.
            // Marking the shadow as existent should not cause much harm. It should only speed up things a little.
            // And it also avoids shadow duplication.
            return shadowUpdater.markShadowExists(liveShadow, result);
        }

        return true;
    }

    List<PrismObject<ShadowType>> searchShadowsByPrimaryIds(ProvisioningContext ctx,
            Collection<ResourceAttribute<?>> identifiers, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {

        ObjectQuery query = createQueryBySelectedIds(ctx, identifiers, true);
        try {
            return searchRepoShadows(query, null, result);
        } catch (SchemaException ex) {
            throw new SchemaException("Failed to search shadow according to the primary identifiers in: "
                    + identifiers + ". Reason: " + ex.getMessage(), ex);
        }
    }

    /**
     * Unlike {@link #lookupLiveShadowByPrimaryId(ProvisioningContext, PrismProperty, QName, OperationResult)} this method
     * queries directly the shadow.primaryIdentifierValue property. (And does not ask for shadow liveness.)
     */
    PrismObject<ShadowType> lookupShadowByIndexedPrimaryIdValue(ProvisioningContext ctx, String primaryIdentifierValue,
            OperationResult result) throws SchemaException {

        ObjectQuery query = createQueryByPrimaryIdValue(ctx, primaryIdentifierValue);
        LOGGER.trace("Searching for shadow by primaryIdentifierValue using filter:\n{}", DebugUtil.debugDumpLazily(query, 1));

        // Explicitly avoid all caches. We want to avoid shadow duplication.
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .staleness(0L)
                .build();

        return ProvisioningUtil.selectSingleShadow(
                searchRepoShadows(query, options, result),
                lazy(() -> "primary identifier value " + primaryIdentifierValue + " (impossible because of DB constraint)"));
    }

    public PrismObject<ShadowType> lookupShadowByAllIds(ProvisioningContext ctx,
            ResourceAttributeContainer identifierContainer, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        @SuppressWarnings("unchecked")
        ObjectQuery query = createQueryBySelectedIds(ctx, (Collection) identifierContainer.getValue().getItems(), false);
        LOGGER.trace("Searching for shadow using filter (repo):\n{}", query.debugDumpLazily());

        List<PrismObject<ShadowType>> shadows = searchRepoShadows(query, null, result);

        PrismObject<ShadowType> singleShadow = ProvisioningUtil.selectSingleShadow(shadows, identifierContainer);
        checkConsistency(singleShadow);
        return singleShadow;
    }

    Collection<PrismObject<ShadowType>> searchForPreviousDeadShadows(ProvisioningContext ctx,
            PrismObject<ShadowType> shadowToAdd, OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {

        PrismProperty<?> identifier = ProvisioningUtil.getSingleValuedPrimaryIdentifier(shadowToAdd);
        if (identifier == null) {
            LOGGER.trace("No primary identifier. So there are obviously no relevant previous dead shadows.");
            return emptyList();
        }

        ObjectQuery query = createQueryByPrimaryId(ctx, identifier, shadowToAdd.asObjectable().getObjectClass());
        LOGGER.trace("Searching for dead shadows using filter:\n{}", query.debugDumpLazily(1));

        List<PrismObject<ShadowType>> shadowsFound = searchRepoShadows(query, null, result);
        List<PrismObject<ShadowType>> deadShadowsFound = shadowsFound.stream()
                .filter(shadowFound -> Boolean.TRUE.equals(shadowFound.asObjectable().isDead()))
                .collect(Collectors.toList());

        LOGGER.trace("looking for previous dead shadows, found {} objects. Dead among them: {}", shadowsFound.size(),
                deadShadowsFound.size());

        return deadShadowsFound;
    }

    PrismObject<ShadowType> lookupShadowBySecondaryIds(ProvisioningContext ctx,
            Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {
        List<PrismObject<ShadowType>> shadows = searchShadowsBySecondaryIds(ctx, secondaryIdentifiers, parentResult);
        return ProvisioningUtil.selectSingleShadow(shadows, lazy(() -> "secondary identifiers: " + secondaryIdentifiers));
    }

    private List<PrismObject<ShadowType>> searchShadowsBySecondaryIds(ProvisioningContext ctx,
            Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {

        if (secondaryIdentifiers.isEmpty()) {
            LOGGER.trace("Shadow does not contain secondary identifier. Skipping lookup shadows according to name.");
            return emptyList();
        }

        S_FilterEntry q = prismContext.queryFor(ShadowType.class)
                .block();
        for (ResourceAttribute<?> secondaryIdentifier : secondaryIdentifiers) {
            // There may be identifiers that come from associations and they will have parent set to association/identifiers
            // For the search to succeed we need all attribute to have "attributes" parent path.
            secondaryIdentifier = ShadowUtil.fixAttributePath(secondaryIdentifier);
            q = q.item(secondaryIdentifier.getPath(), secondaryIdentifier.getDefinition())
                    .eq(getNormalizedValue(secondaryIdentifier, ctx.getObjectClassDefinition()))
                    .or();
        }
        ObjectQuery query = q.none().endBlock()
                .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                .build();
        LOGGER.trace("Searching for shadow using filter on secondary identifiers:\n{}", query.debugDumpLazily());

        // TODO: check for errors
        return searchRepoShadows(query, null, parentResult);
    }

    private void checkConsistency(PrismObject<ShadowType> shadow) {
        ProvisioningUtil.checkShadowActivationConsistency(shadow);
    }

    private <T> List<PrismPropertyValue<T>> getNormalizedValue(PrismProperty<T> attr, RefinedObjectClassDefinition rObjClassDef) throws SchemaException {
        RefinedAttributeDefinition<T> refinedAttributeDefinition = rObjClassDef.findAttributeDefinition(attr.getElementName());
        QName matchingRuleQName = requireNonNull(refinedAttributeDefinition).getMatchingRuleQName();
        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, refinedAttributeDefinition.getTypeName());
        List<PrismPropertyValue<T>> normalized = new ArrayList<>();
        for (PrismPropertyValue<T> origPValue : attr.getValues()) {
            T normalizedValue = matchingRule.normalize(origPValue.getValue());
            PrismPropertyValue<T> normalizedPValue = origPValue.clone();
            normalizedPValue.setValue(normalizedValue);
            normalized.add(normalizedPValue);
        }
        return normalized;
    }

    // TODO why we cannot use object class from ctx?
    private @NotNull ObjectQuery createQueryByPrimaryId(ProvisioningContext ctx, PrismProperty<?> identifier,
            QName objectClass) throws ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SchemaException {
        try {
            // If the query is to be used against the repository, we should not provide matching rules here. See MID-5547.
            PrismPropertyDefinition<?> def = identifier.getDefinition();
            return prismContext.queryFor(ShadowType.class)
                    .itemWithDef(def, ShadowType.F_ATTRIBUTES, def.getItemName()).eq(getNormalizedValue(identifier, ctx.getObjectClassDefinition()))
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClass)
                    .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                    .build();
        } catch (SchemaException e) {
            throw new SchemaException("Schema error while creating search filter: " + e.getMessage(), e);
        }
    }

    private ObjectQuery createQueryByPrimaryIdValue(ProvisioningContext ctx, String primaryIdentifierValue) throws SchemaException {
        try {
            return prismContext.queryFor(ShadowType.class)
                    .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).eq(primaryIdentifierValue)
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(ctx.getObjectClassDefinition().getTypeName())
                    .and().item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid())
                    .build();

        } catch (ExpressionEvaluationException | CommunicationException | ConfigurationException | ObjectNotFoundException e) {
            // Should not happen at this stage. And we do not want to pollute throws clauses all the way up.
            throw new SystemException(e.getMessage(), e);
        }
    }

    private ObjectQuery createQueryBySelectedIds(ProvisioningContext ctx, Collection<ResourceAttribute<?>> identifiers,
            boolean primaryIdentifiersOnly) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        boolean identifierFound = false;

        S_AtomicFilterEntry q = prismContext.queryFor(ShadowType.class);

        RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        for (PrismProperty<?> identifier : identifiers) {
            RefinedAttributeDefinition<?> rAttrDef;
            PrismPropertyValue<?> identifierValue = identifier.getValue();
            if (objectClassDefinition == null) {
                // If there is no specific object class definition then the identifier definition
                // must be the same in all object classes and that means that we can use
                // definition from any of them.
                RefinedObjectClassDefinition anyDefinition = ctx.getRefinedSchema().getRefinedDefinitions().iterator().next();
                rAttrDef = anyDefinition.findAttributeDefinition(identifier.getElementName());
                if (primaryIdentifiersOnly && !anyDefinition.isPrimaryIdentifier(identifier.getElementName())) {
                    continue;
                }
            } else {
                if (primaryIdentifiersOnly && !objectClassDefinition.isPrimaryIdentifier(identifier.getElementName())) {
                    continue;
                }
                rAttrDef = objectClassDefinition.findAttributeDefinition(identifier.getElementName());
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

        if (objectClassDefinition != null) {
            q = q.item(ShadowType.F_OBJECT_CLASS).eq(objectClassDefinition.getTypeName()).and();
        }
        return q.item(ShadowType.F_RESOURCE_REF).ref(ctx.getResourceOid()).build();
    }

    @NotNull
    private List<PrismObject<ShadowType>> searchRepoShadows(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        List<PrismObject<ShadowType>> foundShadows = repositoryService.searchObjects(ShadowType.class, query, options, result);
        MiscSchemaUtil.reduceSearchResult(foundShadows); // Is this faster than specifying the distinct option for repo?
        return foundShadows;
    }
}
