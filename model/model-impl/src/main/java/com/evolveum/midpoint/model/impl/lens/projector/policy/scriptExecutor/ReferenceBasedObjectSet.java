/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * Object set presented as a set of references.
 */
class ReferenceBasedObjectSet extends PartlyReferenceBasedObjectSet {

    ReferenceBasedObjectSet(ActionContext actx, OperationResult result) {
        super(actx, result);
    }

    @Override
    void collectLinkSources() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        try (LinkSourceFinder sourceFinder = new LinkSourceFinder(actx, result)) {
            addReferences(sourceFinder.getSourcesAsReferences());
        }
    }

    private void addReferences(List<PrismReferenceValue> references) {
        references.forEach(o -> individualObjects.put(o.getOid(), o));
    }

    Collection<PrismReferenceValue> asReferenceValues() {
        return individualObjects.values();
    }
}
