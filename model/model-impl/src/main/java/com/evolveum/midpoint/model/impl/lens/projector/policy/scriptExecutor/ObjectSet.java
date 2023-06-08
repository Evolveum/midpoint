/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LinkTargetObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExecutionObjectType;

/**
 * a set of objects that are to be processed.
 * The set can be in the form of:
 * - full prism objects (for synchronous task execution)
 * - prism references (for asynchronous 'single run with generated input' task execution)
 * - object query (for asynchronous 'iterative scripting' task execution)
 *
 * @param <IO> Representation of individual objects: PrismObjectValue or PrismReferenceValue.
 */
abstract class ObjectSet<IO extends PrismValue> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectSet.class);

    @NotNull final ActionContext actx;
    @NotNull final PolicyRuleScriptExecutor beans;
    @Nullable final ScriptExecutionObjectType objectSpec;
    final OperationResult result;
    private boolean collected;

    /**
     * Individual objects that we know by OID. Besides these there could be other ones (from link sources).
     * We use OID-keyed map to avoid duplicate values.
     */
    final Map<String, IO> individualObjects = new HashMap<>();

    ObjectSet(ActionContext actx, OperationResult result) {
        this.actx = actx;
        this.beans = actx.beans;
        this.objectSpec = actx.action.getObject();
        this.result = result;
    }

    void collect() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        checkNotCollected();

        if (objectSpec == null) {
            PrismObject<? extends ObjectType> focus = actx.focusContext.getObjectAny();
            if (focus != null) {
                addObject(focus);
            }
        } else {
            if (objectSpec.getCurrentObject() != null) {
                PrismObject<? extends ObjectType> focus = actx.focusContext.getObjectAny();
                if (currentObjectMatches(focus, objectSpec.getCurrentObject())) {
                    addObject(focus);
                }
            }
            if (!objectSpec.getLinkTarget().isEmpty() || !objectSpec.getNamedLinkTarget().isEmpty()) {
                try (LinkTargetFinder targetFinder = new LinkTargetFinder(actx, result)) {
                    for (LinkTargetObjectSelectorType linkTargetSelector : objectSpec.getLinkTarget()) {
                        addObjects(targetFinder.getTargets(linkTargetSelector));
                    }
                    for (String namedLinkTarget : objectSpec.getNamedLinkTarget()) {
                        addObjects(targetFinder.getTargets(namedLinkTarget));
                    }
                }
            }
            if (!objectSpec.getLinkSource().isEmpty() || !objectSpec.getNamedLinkSource().isEmpty()) {
                collectLinkSources();
            }
        }
    }

    private void checkNotCollected() {
        if (collected) {
            throw new IllegalStateException("Already collected");
        } else {
            collected = true;
        }
    }

    void checkCollected() {
        if (!collected) {
            throw new IllegalStateException("Not collected");
        }
    }

    /**
     * Link sources have to be collected in implementation-specific way: as a query,
     * as object references or as full objects.
     */
    abstract void collectLinkSources()
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException;

    private boolean currentObjectMatches(PrismObject<?> object, ObjectSelectorType selector) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        //noinspection unchecked,rawtypes
        return actx.beans.repositoryService.selectorMatches(
                selector, (PrismObject) object, null, LOGGER, "current object");
    }

    void addObjects(Collection<PrismObject<? extends ObjectType>> objects) {
        objects.forEach(this::addObject);
    }

    private void addObject(PrismObject<? extends ObjectType> o) {
        individualObjects.put(o.getOid(), toIndividualObject(o));
    }

    abstract IO toIndividualObject(PrismObject<?> object);

}
