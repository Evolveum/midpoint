/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.AssignmentUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerable;

/**
 * This is a kind of ad-hoc consolidation of whole assignments:
 *
 * There are some proposals to delete assignments, coming from both legacy and association mappings.
 * Any of these can be vetoed by either association mappings referring to an assignment that should be kept (because
 * it is correlated to an association), or by newly created assignments - either from legacy or association mappings.
 *
 * LIMITED IMPLEMENTATION: we only consider association-related assignments for now
 */
class AssignmentsConsolidation {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentsConsolidation.class);

    @NotNull private final AssignmentsProcessingContext assignmentsProcessingContext;

    @Nullable private final AssignmentHolderType target;

    /** Working copy: we will remove deltas from this list as needed. */
    @NotNull private final List<ItemDelta<?, ?>> itemDeltas;

    /** Working copy: we will remove assignments from this map as needed. */
    @NotNull private final Map<Long, AssignmentType> assignmentsToAdd;

    AssignmentsConsolidation(
            @NotNull AssignmentsProcessingContext assignmentsProcessingContext,
            @NotNull Collection<ItemDelta<?, ?>> itemDeltas,
            @Nullable PrismContainerValue<?> target) {
        this.assignmentsProcessingContext = assignmentsProcessingContext;
        this.target = asContainerable(target) instanceof AssignmentHolderType aht ? aht : null;
        this.itemDeltas = new ArrayList<>(itemDeltas);
        this.assignmentsToAdd = new HashMap<>(assignmentsProcessingContext.getAssignmentsToAdd());
    }

    Collection<? extends ItemDelta<?, ?>> consolidate() throws SchemaException {

        fillAssignmentsToAdd();

        var rv = new ArrayList<ItemDelta<?, ?>>(appendAssignmentAddDeltas());

        if (target == null) {
            return rv; // no current object -> there are no assignments to delete
        }

        main: for (Long idToDelete : assignmentsProcessingContext.getAssignmentsToDelete()) {
            LOGGER.trace("Considering whether to delete assignment {}", idToDelete);
            if (assignmentsProcessingContext.getAssignmentsToKeep().contains(idToDelete)) {
                LOGGER.trace(" -> not deleting, as it's present among those we want to keep");
                continue;
            }
            var assignmentToDelete = AssignmentUtil.getAssignmentRequired(target, idToDelete);
            for (AssignmentType assignmentToAdd : assignmentsToAdd.values()) {
                // We ignore PCV IDs, as they are certainly different. We also ignore metadata, as they are probably
                // different as well (existing assignment contains them, while the new one does not). We hope that other
                // operational data are not relevant here. But one could expect that; as mappings should not produce such data.
                if (assignmentToAdd.asPrismContainerValue().equals(assignmentToDelete.asPrismContainerValue(), EquivalenceStrategy.REAL_VALUE)) {
                    LOGGER.trace(" -> not deleting, as it's present among those we want to add: {}", assignmentToAdd);
                    assignmentsToAdd.remove(assignmentToAdd.getId());
                    continue main;
                }
            }
            LOGGER.trace(" -> will delete, as there's no reason of not doing so");
            rv.add(
                    PrismContext.get().deltaFor(AssignmentHolderType.class)
                            .item(FocusType.F_ASSIGNMENT)
                            .delete(assignmentToDelete.clone())
                            .asItemDelta());
        }
        return rv;
    }

    /** Here we apply partial deltas to the assignments which are being added. */
    private void fillAssignmentsToAdd() throws SchemaException {
        var iterator = itemDeltas.iterator();
        while (iterator.hasNext()) {
            var delta = iterator.next();
            var path = delta.getPath();
            if (path.size() < 3 || !path.startsWith(AssignmentHolderType.F_ASSIGNMENT)) {
                continue; // we look for assignment[x]/...
            }
            var secondAndAfter = path.rest();
            Long id = secondAndAfter.firstToIdOrNull();
            if (id == null) {
                continue;
            }
            var assignmentToAdd = assignmentsToAdd.get(id);
            if (assignmentToAdd == null) {
                continue;
            }
            delta.applyTo(assignmentToAdd.asPrismContainerValue(), secondAndAfter.rest());
            iterator.remove();
        }
    }

    private @NotNull Collection<? extends ItemDelta<?, ?>> appendAssignmentAddDeltas() throws SchemaException {
        Collection<ItemDelta<?, ?>> assignmentsAddDeltas =
                PrismContext.get().deltaFor(AssignmentHolderType.class)
                        .item(FocusType.F_ASSIGNMENT)
                        .addRealValues(
                                CloneUtil.cloneCollectionMembers(assignmentsToAdd.values()))
                        .asItemDeltas();
        return MiscUtil.concat(assignmentsAddDeltas, itemDeltas);
    }
}
