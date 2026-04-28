/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.midpointStatisticsComputation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Handles the iteration over focus objects which are associated with shadows satisfying the provided query.
 *
 * To iterate through all relevant focus objects, this class does two searches:
 *
 * . Focus objects with linkRef(s) to the shadow with specified resource/kind/intent (using linkRef dereference)
 * . Shadows matching criteria, resolving focus owners from correlation state
 *
 * Focus objects found during first search are not processed again if they are also find during the second search.
 */
public class FocusObjectsByAssociatedShadowsIteration {
    private static final Trace LOGGER = TraceManager.getTrace(FocusObjectsByAssociatedShadowsIteration.class);

    private final RepositoryService repositoryService;

    private final QName focusType;
    private final Class<FocusType> focusTypeClass;

    /**
     * Tracks focus OIDs already processed to avoid duplicates.
     *
     * These are used in the second search which goes through the shadows. If the correlated owner candidate has
     * already been processed by first search (it is present in this step), it can be skipped.
     */
    private final Set<String> alreadyProcessedObjects = new HashSet<>();

    /**
     * Tracks all shadow OIDs that are already linked to focus objects.
     *
     * This set contains shadows, which were already seen by the iteration over focus objects. That means we don't
     * need to check their correlation state in the second search.
     */
    private final Set<String> allLinkedShadows = new HashSet<>();

    /**
     * Creates a new iteration instance.
     *
     * @param repositoryService The repository service for object searches
     * @param focusType The focus type to search for
     */
    FocusObjectsByAssociatedShadowsIteration(RepositoryService repositoryService, QName focusType) {
        this.repositoryService = repositoryService;
        this.focusType = focusType;
        this.focusTypeClass = ObjectTypes.getObjectTypeClass(focusType);
    }

    /**
     * Returns an "iteration object" that walks through focus objects related to shadows matching provided shadows query.
     */
    IterationItemConsumer iterate(ObjectQuery associatedShadowsQuery) {
        return (handler, opResult) -> {
            try {
                iterateOverFocusObjects(associatedShadowsQuery.getFilter(), handler, opResult);
                iterateOverShadowsForCorrelatedOwners(associatedShadowsQuery, handler, opResult);
            } catch (SchemaException e) {
                throw SystemException.unexpected(e, "Unexpected exception during iterating over focuses by object "
                        + "type of their associated shadows.");
            }
        };
    }

    /**
     * Iterates over focus objects that have shadows matching the resource/kind/intent criteria.
     */
    private void iterateOverFocusObjects(ObjectFilter associatedShadowsFilter, ResultHandler<FocusType> handler,
            OperationResult opResult)
            throws SchemaException {
        final ObjectFilter shadowFilter = pathToShadowViaLinkRefDereference(associatedShadowsFilter);
        final ObjectQuery query = PrismContext.get().queryFor(focusTypeClass)
                .filter(shadowFilter)
                .build();

        LOGGER.trace("Starting search for focus objects with query: {}", query);

        // Execute the search and submit items for processing
        repositoryService.searchObjectsIterative(
                focusTypeClass,
                query,
                (focus, parentResult) -> {
                    alreadyProcessedObjects.add(focus.getOid());
                    final List<String> shadowsLinkedWithFocus = focus.asObjectable().getLinkRef().stream()
                            .map(AbstractReferencable::getOid)
                            .toList();
                    allLinkedShadows.addAll(shadowsLinkedWithFocus);
                    return handler.handle(focus, parentResult);
                },
                Collections.emptyList(),
                true,
                opResult);
    }

    /**
     * Iterates over shadows matching criteria and resolves focus owners from correlation data.
     * This finds focus objects that are correlated but not yet linked.
     */
    private void iterateOverShadowsForCorrelatedOwners(ObjectQuery shadowQuery, ResultHandler<FocusType> handler,
            OperationResult opResult)
            throws SchemaException {

        LOGGER.trace("Starting search for focus objects through correlated owner candidates of shadows with query: {}",
                shadowQuery);
        repositoryService.searchObjectsIterative(
                ShadowType.class,
                shadowQuery,
                (shadow, parentResult) -> {
                    if (allLinkedShadows.contains(shadow.getOid())) {
                        return true;
                    }
                    return Optional.ofNullable(shadow.asObjectable().getCorrelation())
                            .map(ShadowCorrelationStateType::getResultingOwner)
                            .filter(ownerRef -> QNameUtil.match(ownerRef.getType(),this.focusType))
                            .map(AbstractReferencable::getOid)
                            .filter(Predicate.not(alreadyProcessedObjects::contains))
                            .flatMap(resultingOwnerOid -> resolveOwner(resultingOwnerOid, shadow, parentResult))
                            .map(owner -> handler.handle(owner, parentResult))
                            .orElse(true);
                },
                Collections.emptyList(),
                true,
                opResult);
    }

    private Optional<PrismObject<FocusType>> resolveOwner(String resultingOwner, PrismObject<ShadowType> shadow,
            OperationResult result) {
        try {
            return Optional.of(repositoryService
                    .getObject(focusTypeClass, resultingOwner, Collections.emptyList(), result));
        } catch (ObjectNotFoundException e) {
            LOGGER.debug("Shadow {} references as its resulting owner the object {} which does not exist.", shadow,
                    resultingOwner);
            return Optional.empty();
        } catch (SchemaException e) {
            throw SystemException.unexpected(e,
                    "Unexpected schema exception when reading resulting owner candidate of the shadow");
        }
    }

    private ObjectFilter pathToShadowViaLinkRefDereference(ObjectFilter associatedShadowsFilter) {
        final ItemPath objectReference = ItemPath.create(FocusType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE);
        final ItemDefinition<?> definition =
                PrismContext.get().getSchemaRegistry()
                        .findComplexTypeDefinitionByType(this.focusType)
                        .findItemDefinition(objectReference);
        final ObjectFilter shadowFilter = associatedShadowsFilter.clone();
        shadowFilter.transformItemPaths(objectReference, definition,
                (parentPath, parentDefinition, filter) -> ItemPath.create(objectReference, filter.getFullPath()));
        return shadowFilter;
    }

    public interface IterationItemConsumer {
        /**
         * Iterates through the relevant focus objects, calling the provided handler for each of them.
         *
         * @param itemHandler The handler to process each focus object
         * @param opResult The operation result used to searching for the focus objects
         */
        void consumeItemsWith(ResultHandler<FocusType> itemHandler, OperationResult opResult);
    }
}
