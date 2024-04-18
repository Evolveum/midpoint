/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO DOC
 */
public class ThreeWayMerger<O extends ObjectType> {

    private record FragmentKey(FragmentSide side, ItemDelta<?, ?> delta) {
    }

    /**
     * TODO DOCUMENTATION
     *
     * Notes:
     *
     * * if we want to use current object and don't touch admin changes
     * (loose all dev changes in initial objects) then no delta is needed, no change in repository is needed
     * * if we want to use current initial and replace all admin changes this diff is needed
     *
     * @param left E.g. current version of vanilla initial object (4.8)
     * @param base E.g. previous version of object in repository (4.4)
     * @param right E.g. current version of object in repository
     */
    public MergeResult computeChanges(@NotNull PrismObject<O> left, @NotNull PrismObject<O> base, @NotNull PrismObject<O> right)
            throws SchemaException, ConfigurationException {

        // todo use mergers here, this should create nicer deltas when working correctly
        //  no add/delete whole container values when there's no PCV id, instead of changes in container value directly

        //        GenericItemMerger merger = new GenericItemMerger(null, new PathKeyedMap<>(), MergeStrategy.FULL);
        //
        //        PrismObject<O> leftMerged = base.clone();
        //        new BaseMergeOperation<>(leftMerged.asObjectable(), left.asObjectable(), merger).execute();
        //
        //        PrismObject<O> rightMerged = base.clone();
        //        new BaseMergeOperation<>(rightMerged.asObjectable(), right.asObjectable(), merger).execute();

        ObjectDelta<O> leftDelta = base.diff(left);
        ObjectDelta<O> rightDelta = base.diff(right);

        MergeResult mergeResult = checkBasicConditions(leftDelta, rightDelta);
        if (mergeResult != null) {
            return mergeResult;
        }

        if (leftDelta.isEmpty() && rightDelta.isEmpty()) {
            return new MergeResult();
        }

        Map<FragmentKey, List<ItemDelta<?, ?>>> map = new HashMap<>();

        // first pass map to figure out related deltas based on paths
        for (ItemDelta<?, ?> delta : leftDelta.getModifications()) {
            firstDeltaPass(delta, FragmentSide.LEFT, rightDelta.getModifications(), map, true);
        }

        for (ItemDelta<?, ?> delta : rightDelta.getModifications()) {
            firstDeltaPass(delta, FragmentSide.RIGHT, leftDelta.getModifications(), map, false);
        }

        List<MergeFragment> result = new ArrayList<>();

        // second pass to create fragments
        for (Map.Entry<FragmentKey, List<ItemDelta<?, ?>>> entry : map.entrySet()) {
            FragmentKey key = entry.getKey();
            ItemDelta<?, ?> delta = key.delta();
            List<ItemDelta<?, ?>> otherDeltas = entry.getValue();

            Set<SingleModification> otherChanges = createSimpleChanges(otherDeltas);

            // delta (prism values and its) modifications
            for (SingleModification change : createSimpleChanges(List.of(delta))) {
                // todo these following for loops are incorrect, we should search for value:
                //  1. equivalent to deltaValue
                //  2. if deltaValue PCV ID is not null then for the value with the same ID
                //  3. if there's merger available then maybe search even for equivalent values? Later!!!
                // other values should be non - conflicting
                List<SingleModification> otherConflicting = findOtherConflictingChanges(change.value(), otherChanges);
                if (otherConflicting.isEmpty()) {
                    result.add(
                            MergeFragment.leftOnlyChange(
                                    change.toSingleItemDelta()));
                    continue;
                }

                boolean conflict = otherConflicting.stream().anyMatch(sm ->
                        change.type() != sm.type()
                                || !change.value().equals(sm.value(), EquivalenceStrategy.DATA));

                result.add(
                        new MergeFragment(
                                List.of(change.toSingleItemDelta()),
                                otherConflicting.stream()
                                        .map(SingleModification::toSingleItemDelta)
                                        .collect(Collectors.toList()),
                                conflict));
            }
        }

        return new MergeResult(result);
    }

    private MergeResult checkBasicConditions(ObjectDelta<O> leftDelta, ObjectDelta<O> rightDelta) {
        // todo how to handle object deletion and object add?

        if (leftDelta.isEmpty()) {
            return new MergeResult(
                    rightDelta.getModifications().stream()
                            .map(d -> MergeFragment.rightOnlyChange(d))
                            .collect(Collectors.toList()));
        }

        if (rightDelta.isEmpty()) {
            return new MergeResult(
                    leftDelta.getModifications().stream()
                            .map(d -> MergeFragment.leftOnlyChange(d))
                            .collect(Collectors.toList()));
        }

        return null;
    }

    private List<SingleModification> findOtherConflictingChanges(PrismValue value, Set<SingleModification> changes) {
        // todo natural keys should be used here as well probably
        // todo figure out how to find child items conflicting changes

        if (value.getParent().getDefinition().isSingleValue()) {
            // if it's single value all changes should be used
            return new ArrayList<>(changes);
        }
        if (value instanceof PrismContainerValue<?> pcv) {
            if (pcv.getId() != null) {
                return changes.stream()
                        .filter(sm -> sm.value() instanceof PrismContainerValue<?>)
                        .filter(sm -> Objects.equals(pcv.getId(), ((PrismContainerValue<?>) sm.value()).getId()))
                        .toList();
            }
        }

        return changes.stream()
                .filter(sm -> sm.value().equals(value, EquivalenceStrategy.DATA))
                .toList();
    }

    private void firstDeltaPass(
            ItemDelta<?, ?> delta,
            FragmentSide deltaSide,
            Collection<? extends ItemDelta<?, ?>> otherDeltas,
            Map<FragmentKey, List<ItemDelta<?, ?>>> map,
            boolean addEquivalents) {

        ItemPath path = delta.getPath();

        FragmentKey key = new FragmentKey(deltaSide, delta);

        List<ItemDelta<?, ?>> fragments = map.getOrDefault(key, new ArrayList<>());

        boolean skip = false;
        for (ItemDelta<?, ?> otherDelta : otherDeltas) {
            ItemPath otherPath = otherDelta.getPath();

            if (path.equivalent(otherPath)) {
                // both sides modified the same path
                if (addEquivalents) {
                    fragments.add(otherDelta);
                }
                skip = !addEquivalents;
            } else if (otherPath.startsWith(path)) {
                // conflict, since right item delta modifies part of item that is modified by parent item on left side
                fragments.add(otherDelta);
            } else if (path.startsWith(otherPath)) {
                // conflict, since left item delta modifies part of item that is modified by delta on right side
                // this will be sorted out from the other side
                skip = true;
            } else {
                // no conflict, no related delta on other side
            }
        }

        if (!skip) {
            map.putIfAbsent(key, fragments);
        }
    }

    private Set<SingleModification> createSimpleChanges(List<ItemDelta<?, ?>> deltas) {
        Set<SingleModification> changes = new HashSet<>();

        deltas.forEach(d -> {
            addCollectionToSimpleChanges(d.getValuesToAdd(), ModificationType.ADD, changes, d);
            addCollectionToSimpleChanges(d.getValuesToDelete(), ModificationType.DELETE, changes, d);
            addCollectionToSimpleChanges(d.getValuesToReplace(), ModificationType.REPLACE, changes, d);
        });

        return changes;
    }

    private void addCollectionToSimpleChanges(
            Collection<? extends PrismValue> values, ModificationType type, Set<SingleModification> changes,
            ItemDelta<?, ?> sourceDelta) {

        if (values == null) {
            return;
        }

        values.forEach(v -> changes.add(new SingleModification(v, type, sourceDelta)));
    }
}
