/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr.triggerSetter;

import com.evolveum.midpoint.model.api.expr.OptimizingTriggerCreator;
import com.evolveum.midpoint.model.impl.expr.MidpointFunctionsImpl;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.toMillis;
import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

/**
 *  This is a preliminary implementation.
 */
public class OptimizingTriggerCreatorImpl implements OptimizingTriggerCreator {

    private static final Trace LOGGER = TraceManager.getTrace(OptimizingTriggerCreatorImpl.class);

    private static final String OP_CREATE_TRIGGER = OptimizingTriggerCreatorImpl.class.getName() + ".createTrigger";

    /** For testing cluster-wide scenarios. */
    @TestOnly
    public static boolean useGlobalState = true;

    private final TriggerCreatorGlobalState globalState;
    private final MidpointFunctionsImpl midpointFunctions;

    /**
     * How many milliseconds after current time the trigger should be fired. An example: 60 seconds.
     */
    private final long fireAfter;

    /**
     * What is the safety margin, i.e. how many milliseconds before the trigger fire time we need to create a new trigger.
     * An example: 10 seconds.
     *
     * This means that if a trigger was created for a given object to fire at 10:00:00,000, we accept it until
     * 9:59:50,000. After that time we create a new trigger even if it's before the fire time of 10:00:00,000.
     *
     * This is to handle situations where trigger creation is done before related changes (that require future recomputation)
     * are taken. For example, when processing asynchronous messages, we first create a trigger (in UCF transformation expression)
     * and only then we pass a change to be recorded in the shadow. We assume it can take up to 10 seconds to update the
     * shadow. So if there's an existing trigger that is close to be fired (10 seconds or less), we rather create a new one.
     */
    private final long safetyMargin;

    public OptimizingTriggerCreatorImpl(
            TriggerCreatorGlobalState globalState,
            MidpointFunctionsImpl midpointFunctions,
            long fireAfter,
            long safetyMargin) {
        this.globalState = globalState;
        this.midpointFunctions = midpointFunctions;
        this.fireAfter = fireAfter;
        this.safetyMargin = safetyMargin;
    }

    @Override
    public boolean createForNamedUser(@NotNull String name)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        return createTriggerIfNeeded(new TriggerHolderSpecification.Named(UserType.class, name));
    }

    @Override
    public boolean createForNamedObject(@NotNull Class<? extends ObjectType> type, @NotNull String name)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        return createTriggerIfNeeded(new TriggerHolderSpecification.Named(type, name));
    }

    @Override
    public boolean createForObject(@NotNull Class<? extends ObjectType> type, @NotNull String oid)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        return createTriggerIfNeeded(new TriggerHolderSpecification.Referenced(type, oid));
    }

    private boolean createTriggerIfNeeded(TriggerHolderSpecification key)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        long now = System.currentTimeMillis();

        CreatedTrigger triggerFoundOnCurrentNode = useGlobalState ? globalState.getLastCreatedTrigger(key) : null;
        if (triggerFoundOnCurrentNode != null && now < triggerFoundOnCurrentNode.getFireTime() - safetyMargin) {
            LOGGER.trace("Found relevant record of last created trigger for {}: {} - no need to create another",
                    key, triggerFoundOnCurrentNode);
            return false;
        } else if (triggerFoundOnCurrentNode == null) {
            // We may run in cluster, or the cache could be invalidated in the meanwhile.
            LOGGER.trace("Found no cached record of last created trigger for {}, will look right in the object", key);
        } else {
            // If we run in the cluster, a newer trigger could be created on another node. (Although the cache would get
            // invalidated eventually.) If we run in the single-node environment, the trigger probably does not exist,
            // but for simplicity we check anyway.
            LOGGER.trace("Found expired record of last created trigger for {}: {}, will look right in the object",
                    key, triggerFoundOnCurrentNode);
        }

        // Let's check right in the object
        ObjectType existingObject = getObject(key);
        if (existingObject == null) {
            return false; // should not occur
        }

        var suitableTriggers = existingObject.getTrigger().stream()
                .filter(t -> RecomputeTriggerHandler.HANDLER_URI.equals(t.getHandlerUri()))
                .filter(t -> now < toMillis(t.getTimestamp()) - safetyMargin)
                .toList();
        if (!suitableTriggers.isEmpty()) {
            LOGGER.trace("Found {} suitable trigger(s) on {}: {}, no need to create another one",
                    suitableTriggers.size(), key, suitableTriggers);
            return false;
        }

        CreatedTrigger triggerCreated = createTrigger(existingObject, key, now + fireAfter);
        if (useGlobalState) {
            globalState.recordCreatedTrigger(key, triggerCreated);
        }
        return true;
    }

    private CreatedTrigger createTrigger(ObjectType existingObject, TriggerHolderSpecification key, long triggerTimestamp)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        RepositoryService repositoryService = midpointFunctions.getRepositoryService();
        TriggerType trigger = new TriggerType()
                .handlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .timestamp(XmlTypeConverter.createXMLGregorianCalendar(triggerTimestamp));
        List<ItemDelta<?, ?>> itemDeltas = PrismContext.get().deltaFor(key.getType())
                .item(ObjectType.F_TRIGGER).add(trigger)
                .asItemDeltas();
        repositoryService.modifyObject(existingObject.getClass(), existingObject.getOid(), itemDeltas, getCurrentResult());
        return new CreatedTrigger(existingObject.getOid(), triggerTimestamp);
    }

    private @Nullable ObjectType getObject(TriggerHolderSpecification key) throws SchemaException, ObjectNotFoundException {
        RepositoryService repositoryService = midpointFunctions.getRepositoryService();
        PrismContext prismContext = midpointFunctions.getPrismContext();

        String oid = key.getOid();
        if (oid != null) {
            return repositoryService
                    .getObject(key.getType(), oid, readOnly(), getCurrentResult())
                    .asObjectable();
        }

        ObjectQuery query = key.createQuery(prismContext);
        if (query == null) {
            throw new IllegalStateException("No OID nor query for " + key);
        }
        SearchResultList<? extends PrismObject<? extends ObjectType>> objects =
                repositoryService.searchObjects(key.getType(), query, readOnly(), getCurrentResult());
        if (objects.isEmpty()) {
            LOGGER.warn("No object found for {}; no trigger will be set", key); // or should we throw ObjectNotFoundException?
            return null;
        } else if (objects.size() > 1) {
            LOGGER.warn("More than one object found for {}; trigger will be considered only for the first one: {}", key, objects);
        }
        return objects.get(0).asObjectable();
    }

    private @NotNull OperationResult getCurrentResult() {
        return midpointFunctions.getCurrentResult(OP_CREATE_TRIGGER);
    }
}
