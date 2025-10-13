/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * Object set represented as full prism objects.
 */
class FullDataBasedObjectSet extends ObjectSet<PrismObjectValue<?>> {

    FullDataBasedObjectSet(ActionContext actx, OperationResult result) {
        super(actx, result);
    }

    @Override
    void collectLinkSources() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        try (LinkSourceFinder sourceFinder = new LinkSourceFinder(actx, result)) {
            addObjects(sourceFinder.getSourcesAsObjects());
        }
    }

    @Override
    PrismObjectValue<?> toIndividualObject(PrismObject<?> object) {
        return object.getValue();
    }

    Collection<PrismObjectValue<?>> asObjectValues() {
        return individualObjects.values();
    }
}
