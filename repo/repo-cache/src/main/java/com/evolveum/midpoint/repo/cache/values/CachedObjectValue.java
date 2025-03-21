/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.values;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.evolveum.midpoint.prism.PrismConstants.VALUE_METADATA_CONTAINER_NAME;

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
                    if (visitable instanceof Item<?, ?> item) {
                        if (item.isIncomplete()) {
                            allItemsComplete.set(false);
                            return false;
                        }
                        // We are not interested in the value metadata, so we can skip visiting property & reference values
                        return item instanceof PrismContainer<?>
                                && !item.getElementName().equals(VALUE_METADATA_CONTAINER_NAME);
                    } else {
                        // Values should be visited
                        assert visitable instanceof PrismContainerValue<?>;
                        return true;
                    }
                });
        return allItemsComplete.get();
    }
}
