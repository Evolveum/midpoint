/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.delta.ItemDelta;

import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.evolveum.midpoint.prism.delta.PropertyDeltaCollectionsUtil.findPropertyDelta;

/**
 * Modifications to be applied to the repository shadow, either in the repository
 * or in the "refined" in-memory representation.
 */
public class RepoShadowModifications implements DebugDumpable {

    /** Operations on "refined" shadow. */
    @NotNull private final List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();

    /** Operations on "raw" object in the repository. */
    @NotNull private final List<ItemDelta<?, ?>> rawItemDeltas = new ArrayList<>();

    /** The modifications must not refer attributes. */
    public static RepoShadowModifications of(Collection<? extends ItemDelta<?, ?>> itemDeltas) {
        RepoShadowModifications shadowModifications = new RepoShadowModifications();
        shadowModifications.addAll(itemDeltas);
        return shadowModifications;
    }

    public @NotNull List<ItemDelta<?, ?>> getItemDeltas() {
        return itemDeltas;
    }

    public @NotNull List<ItemDelta<?, ?>> getRawItemDeltas() {
        return rawItemDeltas;
    }

    /** The modifications must not refer attributes. */
    public void addAll(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        itemDeltas.addAll(modifications);
        rawItemDeltas.addAll(modifications);
    }

    public void addAll(
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications, @NotNull ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        for (ItemDelta<?, ?> modification : modifications) {
            Optional<AttributePath> attributePath = AttributePath.optionalOf(modification.getPath());
            if (attributePath.isPresent()) {
                var def = objectDefinition.findAttributeDefinitionRequired(attributePath.get().getAttributeName());
                if (def instanceof ShadowSimpleAttributeDefinition<?> simpleDef) {
                    add(modification, simpleDef);
                } else {
                    // TODO implement caching of reference attributes
                    addNonRawOnly(modification);
                }
            } else {
                add(modification);
            }
        }
    }

    public void addAll(@NotNull RepoShadowModifications modifications) {
        itemDeltas.addAll(modifications.itemDeltas);
        rawItemDeltas.addAll(modifications.rawItemDeltas);
    }

    public void add(@Nullable ItemDelta<?, ?> modification) {
        if (modification != null) {
            itemDeltas.add(modification);
            rawItemDeltas.add(modification);
        }
    }

    public void addRawOnly(@Nullable ItemDelta<?, ?> modification) {
        if (modification != null) {
            rawItemDeltas.add(modification);
        }
    }

    public void addNonRawOnly(@Nullable ItemDelta<?, ?> modification) {
        if (modification != null) {
            itemDeltas.add(modification);
        }
    }

    public void add(ItemDelta<?, ?> modification, ShadowSimpleAttributeDefinition<?> attrDef) throws SchemaException {
        ItemDelta<?, ?> rawModification = modification.clone();
        // We have to suppress the type parameters, because - in fact - we change the type of the values.
        //noinspection rawtypes,unchecked
        ((ItemDelta) rawModification).applyDefinition(attrDef.toNormalizationAware(), true);
        add(modification, rawModification);
    }

    public void add(ItemDelta<?, ?> modification, ItemDelta<?, ?> rawModification) {
        itemDeltas.add(modification);
        rawItemDeltas.add(rawModification);
    }

    @Override
    public String debugDump(int indent) {
        // TODO implement more seriously
        return DebugUtil.debugDump(itemDeltas, indent);
    }

    public boolean isEmpty() {
        return itemDeltas.isEmpty() && rawItemDeltas.isEmpty();
    }

    public RepoShadowModifications shallowCopy() {
        var copy = new RepoShadowModifications();
        copy.itemDeltas.addAll(itemDeltas);
        copy.rawItemDeltas.addAll(rawItemDeltas);
        return copy;
    }

    /** TODO we assume that the raw/non-raw deltas touch the same paths. */
    public boolean hasItemDelta(ItemPath path) {
        return ItemDeltaCollectionsUtil.findPropertyDelta(itemDeltas, path) != null;
    }

    public boolean changedToDead() {
        PropertyDelta<Object> deadDelta = findPropertyDelta(itemDeltas, (ItemPath) ShadowType.F_DEAD);
        return deadDelta != null &&
                (containsTrue(deadDelta.getRealValuesToAdd()) || containsTrue(deadDelta.getRealValuesToReplace()));
    }

    private boolean containsTrue(Collection<?> values) {
        return values != null && values.contains(Boolean.TRUE);
    }

    public int size() {
        return itemDeltas.size();
    }

    public int sizeRaw() {
        return rawItemDeltas.size();
    }
}
