/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.*;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.AddDeleteReplace;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.internals.TestingPaths;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Smart set of assignment values that keep track whether the assignment is new, old or changed.
 *
 * This information is used for various reasons. We specifically distinguish between assignments in objectCurrent and objectOld
 * to be able to reliably detect phantom adds: a phantom add is an assignment that is both in OLD and CURRENT objects. This is
 * important in waves greater than 0, where objectCurrent is already updated with existing assignments. (See MID-2422.)
 *
 * Each assignment is present here only once. For example, if it's present in old and current object,
 * and also referenced by a delta, it is still here represented only as a single entry.
 *
 * @author Radovan Semancik
 */
public class SmartAssignmentCollection<F extends AssignmentHolderType> implements Iterable<SmartAssignmentElement>, DebugDumpable {

    private Map<SmartAssignmentKey, SmartAssignmentElement> aMap;
    private Map<Long, SmartAssignmentElement> idMap;

    public void collect(PrismObject<F> objectCurrent, PrismObject<F> objectOld, ContainerDelta<AssignmentType> assignmentDelta,
            Collection<AssignmentType> forcedAssignments, Collection<AssignmentType> taskAssignments) throws SchemaException {
        PrismContainer<AssignmentType> assignmentContainerCurrent;
        if (objectCurrent != null) {
            assignmentContainerCurrent = objectCurrent.findContainer(FocusType.F_ASSIGNMENT);
        } else {
            assignmentContainerCurrent = null;
        }

        if (aMap == null) {
            int initialCapacity = computeInitialCapacity(assignmentContainerCurrent, assignmentDelta, forcedAssignments);
            aMap = new HashMap<>(initialCapacity);
            idMap = new HashMap<>(initialCapacity);
        }

        collectAssignments(assignmentContainerCurrent, Mode.CURRENT);

        collectVirtualAssignments(forcedAssignments);

        for (AssignmentType taskAssignment : taskAssignments) {
            //noinspection unchecked
            collectAssignment(taskAssignment.asPrismContainerValue(), Mode.CURRENT, true, null);
        }

        if (objectOld != null) {
            collectAssignments(objectOld.findContainer(FocusType.F_ASSIGNMENT), Mode.OLD);
        }

        collectAssignments(assignmentDelta);
    }

    private void collectAssignments(PrismContainer<AssignmentType> assignmentContainer, Mode mode) throws SchemaException {
        if (assignmentContainer != null) {
            for (PrismContainerValue<AssignmentType> assignmentCVal : assignmentContainer.getValues()) {
                collectAssignment(assignmentCVal, mode, false, null);
            }
        }
    }

    private void collectVirtualAssignments(Collection<AssignmentType> forcedAssignments) throws SchemaException {
        for (AssignmentType assignment : emptyIfNull(forcedAssignments)) {
            //noinspection unchecked
            collectAssignment(assignment.asPrismContainerValue(), Mode.CURRENT, true, null);
        }
    }

    private void collectAssignments(ContainerDelta<AssignmentType> assignmentDelta) throws SchemaException {
        if (assignmentDelta != null) {
            collectAssignmentsFromDeltaSet(assignmentDelta.getValuesToAdd(), AddDeleteReplace.ADD);
            collectAssignmentsFromDeltaSet(assignmentDelta.getValuesToDelete(), AddDeleteReplace.DELETE);
            collectAssignmentsFromDeltaSet(assignmentDelta.getValuesToReplace(), AddDeleteReplace.REPLACE);
        }
    }

    private void collectAssignmentsFromDeltaSet(Collection<PrismContainerValue<AssignmentType>> assignments,
            AddDeleteReplace deltaSet) throws SchemaException {
        for (PrismContainerValue<AssignmentType> assignmentCVal: emptyIfNull(assignments)) {
            collectAssignment(assignmentCVal, Mode.CHANGED, false, deltaSet);
        }
    }

    private void collectAssignment(PrismContainerValue<AssignmentType> assignmentCVal, Mode mode, boolean virtual,
            AddDeleteReplace deltaSet) throws SchemaException {

        @NotNull SmartAssignmentElement element;

        if (assignmentCVal.isEmpty()) {
            // Special lookup for empty elements.
            // Changed assignments may be "light", i.e. they may contain just the identifier.
            // Make sure that we always have the full assignment data.
            if (assignmentCVal.getId() != null) {
                element = idMap.get(assignmentCVal.getId());
                if (element == null) {
                    // deleting non-existing assignment. Safe to ignore?
                    return;
                }
            } else {
                throw new SchemaException("Attempt to change empty assignment without ID");
            }
        } else {
            SmartAssignmentElement existingElement = lookup(assignmentCVal);
            if (existingElement != null) {
                element = existingElement;
            } else {
                element = put(assignmentCVal, virtual);
            }
        }

        element.updateFlags(mode, deltaSet);
    }

    private SmartAssignmentElement put(PrismContainerValue<AssignmentType> assignmentCVal, boolean virtual) {
        SmartAssignmentElement element = new SmartAssignmentElement(assignmentCVal, virtual);
        aMap.put(element.getKey(), element);
        if (assignmentCVal.getId() != null) {
            idMap.put(assignmentCVal.getId(), element);
        }
        return element;
    }

    private SmartAssignmentElement lookup(PrismContainerValue<AssignmentType> assignmentCVal) {
        if (assignmentCVal.getId() != null) {
            // A shortcut. But also important for deltas that specify correct id, but the value
            // does not match exactly (e.g. small differences in relation, e.g. null vs default)
            return idMap.get(assignmentCVal.getId());
        } else {
            return aMap.get(new SmartAssignmentKey(assignmentCVal));
        }
    }

    private int computeInitialCapacity(PrismContainer<AssignmentType> assignmentContainerCurrent,
            ContainerDelta<AssignmentType> assignmentDelta, Collection<AssignmentType> forcedAssignments) {
        return (assignmentContainerCurrent != null ? assignmentContainerCurrent.size() : 0)
                + (assignmentDelta != null ? assignmentDelta.size() : 0)
                + (forcedAssignments != null ? forcedAssignments.size() : 0);
    }

    @NotNull
    @Override
    public Iterator<SmartAssignmentElement> iterator() {
        if (InternalsConfig.getTestingPaths() == TestingPaths.REVERSED) {
            Collection<SmartAssignmentElement> values = aMap.values();
            List<SmartAssignmentElement> valuesList = new ArrayList<>(values.size());
            valuesList.addAll(values);
            Collections.reverse(valuesList);
            return valuesList.iterator();
        } else {
            return aMap.values().iterator();
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("SmartAssignmentCollection: ");
        if (aMap == null) {
            sb.append("uninitialized");
        } else {
            sb.append(aMap.size()).append(" items");
            for (SmartAssignmentElement element: aMap.values()) {
                sb.append("\n");
                sb.append(element.debugDump(indent + 1));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "SmartAssignmentCollection(" + aMap.values() + ")";
    }

    enum Mode {
        CURRENT, OLD, CHANGED
    }

}
