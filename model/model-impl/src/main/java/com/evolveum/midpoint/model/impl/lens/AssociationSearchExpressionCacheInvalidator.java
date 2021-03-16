/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import org.jetbrains.annotations.NotNull;

/**
 * TODO
 */
class AssociationSearchExpressionCacheInvalidator implements ResourceOperationListener, ResourceObjectChangeListener {

    private final AssociationSearchExpressionEvaluatorCache cache;

    AssociationSearchExpressionCacheInvalidator(AssociationSearchExpressionEvaluatorCache cache) {
        this.cache = cache;
    }

    @Override
    public void notifyChange(@NotNull ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {
        cache.invalidate(change.getResource(), change.getShadowedResourceObject());
    }

    @Override
    public void notifySuccess(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        notifyAny(operationDescription);
    }

    // we are quite paranoid, so we'll process also failures and in-progress events

    @Override
    public void notifyFailure(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        notifyAny(operationDescription);
    }

    @Override
    public void notifyInProgress(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {
        notifyAny(operationDescription);
    }

    private void notifyAny(ResourceOperationDescription operationDescription) {
        cache.invalidate(operationDescription.getResource(), operationDescription.getCurrentShadow());
    }

    @Override
    public String getName() {
        return "AbstractSearchExpressionEvaluatorCache invalidator";
    }
}
