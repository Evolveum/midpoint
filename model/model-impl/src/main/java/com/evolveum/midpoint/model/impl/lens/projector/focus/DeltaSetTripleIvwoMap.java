/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collection of plus/minus/zero sets of values with origins (mappings or similar providers), keyed by target item path.
 *
 * Reason for existence: to avoid unintelligible type names (like `PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>>`)
 * in the code + to provide common methods here.
 */
@Experimental
public class DeltaSetTripleIvwoMap extends PathKeyedMap<DeltaSetTriple<ItemValueWithOrigin<?, ?>>> {

    /** Puts the specified triple into the map; either "as is", or merging it with existing triple for the given path. */
    public <V extends PrismValue, D extends ItemDefinition<?>> void putOrMerge(
            @NotNull ItemPath outputPath,
            @Nullable DeltaSetTriple<ItemValueWithOrigin<V, D>> outputTriple) {
        // This hack is needed because of Java generics limitations (or my lack of understanding of that mechanism).
        //noinspection rawtypes,unchecked
        putOrMergeInternal(outputPath, (DeltaSetTriple) outputTriple);
    }

    private void putOrMergeInternal(
            @NotNull ItemPath outputPath,
            @Nullable DeltaSetTriple<ItemValueWithOrigin<?, ?>> outputTriple) {
        if (outputTriple != null) {
            DeltaSetTriple<ItemValueWithOrigin<?, ?>> mapTriple = get(outputPath);
            if (mapTriple == null) {
                put(outputPath, outputTriple);
            } else {
                mapTriple.merge(outputTriple);
            }
        }
    }

    public void putOrMergeAll(ItemPath pathPrefix, DeltaSetTripleIvwoMap otherMap) {
        for (var entry : otherMap.entrySet()) {
            putOrMergeInternal(pathPrefix.append(entry.getKey()), entry.getValue());
        }
    }

    public void putOrMergeAll(DeltaSetTripleIvwoMap otherMap) {
        for (var entry : otherMap.entrySet()) {
            putOrMergeInternal(entry.getKey(), entry.getValue());
        }
    }
}
