/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

/**
 *
 */
public class ItemDeltaCollectionsUtil {

    // 'strict' means we avoid returning deltas that only partially match. This is NOT a definite solution, see MID-4689
    public static <DD extends ItemDelta> DD findItemDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath,
            Class<DD> deltaType, boolean strict) {
        if (deltas == null) {
            return null;
        }
        for (ItemDelta<?,?> delta : deltas) {
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equivalent(propertyPath)) {
                return (DD) delta;
            }
            // e.g. when deleting credentials we match also deletion of credentials/password (is that correct?)
            if (!strict && delta instanceof ContainerDelta<?> && delta.getPath().isSubPath(propertyPath)) {
                //noinspection unchecked
                return (DD) ((ContainerDelta)delta).getSubDelta(propertyPath.remainder(delta.getPath()));
            }
        }
        return null;
    }

    public static void applyDefinitionIfPresent(Collection<? extends ItemDelta> deltas,
            PrismObjectDefinition definition, boolean tolerateNoDefinition) throws SchemaException {
        for (ItemDelta itemDelta : deltas) {
            ItemPath path = itemDelta.getPath();
            ItemDefinition itemDefinition = ((ItemDefinition) definition).findItemDefinition(path, ItemDefinition.class);
            if (itemDefinition != null) {
                itemDelta.applyDefinition(itemDefinition);
            } else if (!tolerateNoDefinition) {
                throw new SchemaException("Object type " + definition.getTypeName() + " doesn't contain definition for path " + path);
            }
        }
    }

    public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath) {
        return findItemDelta(deltas, propertyPath, PropertyDelta.class, false);
    }

    public static <X extends Containerable> ContainerDelta<X> findContainerDelta(Collection<? extends ItemDelta> deltas,
            ItemPath propertyPath) {
        return findItemDelta(deltas, propertyPath, ContainerDelta.class, false);
    }

    public static Collection<? extends ItemDelta<?,?>> findItemDeltasSubPath(Collection<? extends ItemDelta<?, ?>> deltas,
            ItemPath itemPath) {
        Collection<ItemDelta<?,?>> foundDeltas = new ArrayList<>();
        if (deltas == null) {
            return foundDeltas;
        }
        for (ItemDelta<?,?> delta : deltas) {
            if (itemPath.isSubPath(delta.getPath())) {
                foundDeltas.add(delta);
            }
        }
        return foundDeltas;
    }

    public static <D extends ItemDelta> void removeItemDelta(Collection<? extends ItemDelta> deltas, ItemPath propertyPath,
            Class<D> deltaType) {
        if (deltas == null) {
            return;
        }
        Iterator<? extends ItemDelta> deltasIterator = deltas.iterator();
        while (deltasIterator.hasNext()) {
            ItemDelta<?,?> delta = deltasIterator.next();
            if (deltaType.isAssignableFrom(delta.getClass()) && delta.getPath().equivalent(propertyPath)) {
                deltasIterator.remove();
            }
        }
    }

    public static <D extends ItemDelta> void removeItemDelta(Collection<? extends ItemDelta> deltas, ItemDelta deltaToRemove) {
        if (deltas == null) {
            return;
        }
        Iterator<? extends ItemDelta> deltasIterator = deltas.iterator();
        while (deltasIterator.hasNext()) {
            ItemDelta<?,?> delta = deltasIterator.next();
            if (delta.equals(deltaToRemove)) {
                deltasIterator.remove();
            }
        }
    }

    public static void checkConsistence(Collection<? extends ItemDelta> deltas) {
        checkConsistence(deltas, ConsistencyCheckScope.THOROUGH);
    }

    public static void checkConsistence(Collection<? extends ItemDelta> deltas, ConsistencyCheckScope scope) {
        checkConsistence(deltas, false, false, scope);
    }

    public static void checkConsistence(Collection<? extends ItemDelta> deltas, boolean requireDefinition, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        for (ItemDelta<?,?> delta : deltas) {
            delta.checkConsistence(requireDefinition, prohibitRaw, scope);
            int matches = 0;
            for (ItemDelta<?,?> other : deltas) {
                if (other == delta) {
                    matches++;
                } else if (other.equals(delta)) {
                    throw new IllegalStateException("Duplicate item delta: "+delta+" and "+other);
                }
            }
            if (matches > 1) {
                throw new IllegalStateException("The delta "+delta+" appears multiple times in the modification list");
            }
        }
    }

    public static void applyTo(Collection<? extends ItemDelta> deltas, PrismContainer propertyContainer)
            throws SchemaException {
        for (ItemDelta delta : deltas) {
            delta.applyTo(propertyContainer);
        }
    }

    public static void applyTo(Collection<? extends ItemDelta> deltas, PrismContainerValue propertyContainerValue)
            throws SchemaException {
        for (ItemDelta delta : deltas) {
            delta.applyTo(propertyContainerValue);
        }
    }

    public static void applyToMatchingPath(Collection<? extends ItemDelta> deltas, PrismContainer propertyContainer)
            throws SchemaException {
        for (ItemDelta delta : deltas) {
            delta.applyToMatchingPath(propertyContainer, ParameterizedEquivalenceStrategy.DEFAULT_FOR_DELTA_APPLICATION);
        }
    }

    public static void accept(Collection<? extends ItemDelta> modifications, Visitor visitor, ItemPath path,
            boolean recursive) {
        for (ItemDelta modification: modifications) {
            ItemPath modPath = modification.getPath();
            ItemPath.CompareResult rel = modPath.compareComplex(path);
            if (rel == ItemPath.CompareResult.EQUIVALENT) {
                modification.accept(visitor, null, recursive);
            } else if (rel == ItemPath.CompareResult.SUBPATH) {
                modification.accept(visitor, path.remainder(modPath), recursive);
            }
        }
    }

    public static <D extends ItemDelta<?,?>> Collection<D> cloneCollection(Collection<D> orig) {
        if (orig == null) {
            return null;
        }
        Collection<D> clone = new ArrayList<>(orig.size());
        for (D delta: orig) {
            clone.add((D)delta.clone());
        }
        return clone;
    }

    public static boolean hasEquivalent(Collection<? extends ItemDelta> col, ItemDelta delta) {
        for (ItemDelta colItem: col) {
            if (colItem.equivalent(delta)) {
                return true;
            }
        }
        return false;
    }

    public static void addAll(Collection<? extends ItemDelta> modifications, Collection<? extends ItemDelta> deltasToAdd) {
        if (deltasToAdd == null) {
            return;
        }
        for (ItemDelta deltaToAdd: deltasToAdd) {
            if (!modifications.contains(deltaToAdd)) {
                ((Collection)modifications).add(deltaToAdd);
            }
        }
    }

    public static void merge(Collection<? extends ItemDelta> modifications, ItemDelta delta) {
        for (ItemDelta modification: modifications) {
            if (modification.getPath().equals(delta.getPath())) {
                modification.merge(delta);
                return;
            }
        }
        ((Collection)modifications).add(delta);
    }

    public static void mergeAll(Collection<? extends ItemDelta<?, ?>> modifications,
            Collection<? extends ItemDelta<?, ?>> deltasToMerge) {
        if (deltasToMerge == null) {
            return;
        }
        for (ItemDelta deltaToMerge: deltasToMerge) {
            merge(modifications, deltaToMerge);
        }
    }

    public static boolean pathMatches(@NotNull Collection<? extends ItemDelta<?, ?>> deltas, @NotNull ItemPath path,
            int segmentsToSkip,
            boolean exactMatch) {
        for (ItemDelta<?, ?> delta : deltas) {
            ItemPath modifiedPath = delta.getPath().rest(segmentsToSkip).removeIds();   // because of extension/cities[2]/name (in delta) vs. extension/cities/name (in spec)
            if (exactMatch) {
                if (path.equivalent(modifiedPath)) {
                    return true;
                }
            } else {
                if (path.isSubPathOrEquivalent(modifiedPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <D extends ItemDelta> D findItemDelta(Collection<? extends ItemDelta> deltas, QName itemName, Class<D> deltaType) {
        return findItemDelta(deltas, ItemName.fromQName(itemName), deltaType, false);
    }

    public static ReferenceDelta findReferenceModification(Collection<? extends ItemDelta> deltas, QName itemName) {
        return findItemDelta(deltas, itemName, ReferenceDelta.class);
    }

    public static void addNotEquivalent(Collection<? extends ItemDelta> modifications,
            Collection<? extends ItemDelta> deltasToAdd) {
        if (deltasToAdd == null) {
            return;
        }
        for (ItemDelta deltaToAdd: deltasToAdd) {
            if (!hasEquivalent(modifications, deltaToAdd)) {
                ((Collection)modifications).add(deltaToAdd);
            }
        }
    }
}
