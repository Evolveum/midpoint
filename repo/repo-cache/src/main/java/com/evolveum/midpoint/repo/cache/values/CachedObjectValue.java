/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.values;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public interface CachedObjectValue<T extends ObjectType> {

    /**
     * Is this object complete, i.e., are there no incomplete items in it?
     *
     * This is a very simple mechanism to allow resolving operations with "include" options from the cache.
     * We assume that if a cached object is complete, then it's safe to return it from the cache regardless of any
     * "include" or "exclude" retrieval options that might be in place.
     *
     * Note that we assume that the object was retrieved WITHOUT any "exclude" options.
     * (Such excluded items are not marked as incomplete.)
     */
    boolean isComplete();

    PrismObject<T> getObject();

    static boolean computeCompleteFlag(@NotNull PrismObject<?> object) {
        if (SelectorOptions.isRetrievedFullyByDefault(object.getCompileTimeClass())) {
            return true; // unfortunately, this is quite rare
        }
        var allItemsComplete = new AtomicBoolean(true);
        object.acceptVisitor(
                visitable -> {
                    // TODO it would be better (from the performance viewpoint) if we could abort on the first occurrence
                    //  of an incomplete item
                    if (visitable instanceof Item<?, ?> item && item.isIncomplete()) {
                        allItemsComplete.set(false);
                        return false;
                    } else {
                        return true;
                    }
                });
        return allItemsComplete.get();
    }
}
