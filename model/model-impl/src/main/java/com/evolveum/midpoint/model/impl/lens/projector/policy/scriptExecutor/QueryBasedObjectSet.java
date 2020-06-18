/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.api.query.CompleteQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * Object set represented by an object query.
 */
class QueryBasedObjectSet extends PartlyReferenceBasedObjectSet {

    private CompleteQuery<?> linkedSourcesQuery;

    QueryBasedObjectSet(ActionContext actx, OperationResult result) {
        super(actx, result);
    }

    @Override
    void collectLinkSources() throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        try (LinkSourceFinder sourceFinder = new LinkSourceFinder(actx, result)) {
            linkedSourcesQuery = sourceFinder.getSourcesAsQuery();
        }
    }

    /**
     * @return Set of objects as a single CompleteQuery, suitable e.g. to be included in a iterative scripting task.
     */
    @NotNull
    CompleteQuery<?> asQuery() {
        checkCollected();
        if (individualObjects.isEmpty()) {
            if (linkedSourcesQuery != null) {
                return linkedSourcesQuery;
            } else {
                return CompleteQuery.none(AssignmentHolderType.class, beans.prismContext);
            }
        } else {
            CompleteQuery<?> explicitObjectsQuery = CompleteQuery.inOid(individualObjects.values(), beans.prismContext);
            if (linkedSourcesQuery != null) {
                return CompleteQuery.or(Arrays.asList(linkedSourcesQuery, explicitObjectsQuery), beans.prismContext);
            } else {
                return explicitObjectsQuery;
            }
        }
    }
}
