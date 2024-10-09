/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.delta;

import com.evolveum.midpoint.prism.ItemModifyResult;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sql.data.common.RObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Abstract superclass for all Update classes.
 */
abstract class BaseUpdate {

    final RObject object;
    final ItemDelta<?, ?> delta;
    final UpdateContext ctx;
    final ObjectDeltaUpdater beans;

    BaseUpdate(RObject object, ItemDelta<?, ?> delta, UpdateContext ctx) {
        this.object = object;
        this.delta = delta;
        this.ctx = ctx;
        this.beans = ctx.beans;
    }

    /**
     * Checks result and decides whether it's delete operation.
     * Return true also for "replace" with null value.
     */
    protected boolean isDelete() {
        Collection<ItemModifyResult<? extends PrismValue>> result = (Collection) delta.applyResults();
        if (result == null) {
            return false;
        }

        Map<ItemModifyResult.ActualApplyOperation, List<ItemModifyResult>> changes = result.stream()
                .collect(Collectors.groupingBy(ItemModifyResult::operation));

        if (changes.keySet().size() == 1) {
            if (changes.containsKey(ItemModifyResult.ActualApplyOperation.DELETED)) {
                return true;
            }

            if (changes.containsKey(ItemModifyResult.ActualApplyOperation.MODIFIED)) {
                List<ItemModifyResult> modifications = changes.get(ItemModifyResult.ActualApplyOperation.MODIFIED);
                return modifications.stream().allMatch(r -> r.finalValue == null);
            }
        }

        return false;
    }

    /**
     * Return single {@link PrismValue} from collection of {@link ItemModifyResult} objects or null if not available
     */
    protected <V extends PrismValue> V getSingleValue() {
        Collection<ItemModifyResult<? extends PrismValue>> result = (Collection) delta.applyResults();
        if (result == null) {
            return null;
        }

        return (V) result.stream()
                .filter(r -> !r.isDeleted() && !r.isUnmodified())
                .map(r -> r.finalValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
