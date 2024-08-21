/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.*;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.AssignmentIdStore;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.AddDeleteReplace;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.schema.config.AssignmentConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.internals.TestingPaths;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class SmartAssignmentCollection<F extends AssignmentHolderType>
        implements Iterable<SmartAssignmentElement>, DebugDumpable {

    /**
     * Collection of SmartAssignmentElements (assignment value + origin), indexed by assignment value.
     */
    private Map<SmartAssignmentKey, SmartAssignmentElement> aMap;

    /**
     * Map from assignment ID to SmartAssignmentElements. Currently relevant only when creating the collection.
     */
    private Map<Long, SmartAssignmentElement> idMap;

    /**
     * Fills-in this collection from given sources and freezes origins afterwards.
     *
     * Origin freeze is needed if "replace" delta is present, because we have to set final values for "isNew" origin flags.
     *
     * `objectCurrent` and `objectOld` and `currentAssignmentDelta` must be EP-compliant, i.e. must come from trusted
     * sources (e.g. objects must come from repo, and delta must be checked for authorizations).
     *
     * [EP:APSO] DONE: it is so (I believe)
     */
    public void collectAndFreeze(
            PrismObject<F> objectCurrent, PrismObject<F> objectOld,
            @NotNull OriginProvider<? super AssignmentType> deltaItemOriginProvider,
            ContainerDelta<AssignmentType> currentAssignmentDelta,
            Collection<AssignmentConfigItem> virtualAssignments) throws SchemaException {

        PrismContainer<AssignmentType> assignmentContainerOld = getAssignmentContainer(objectOld);
        PrismContainer<AssignmentType> assignmentContainerCurrent = getAssignmentContainer(objectCurrent);

        if (aMap == null) {
            int initialCapacity = computeInitialCapacity(assignmentContainerCurrent, currentAssignmentDelta, virtualAssignments);
            aMap = new HashMap<>(initialCapacity);
            idMap = new HashMap<>(initialCapacity);
        }

        if (objectCurrent != null) {
            // [EP:APSO] DONE These assignments reside physically in their object. Hence "embedded" is OK.
            collectAssignments(assignmentContainerCurrent, Mode.CURRENT, OriginProvider.embedded());
        }
        if (objectOld != null) {
            // [EP:APSO] DONE These assignments reside physically in their object. Hence "embedded" is OK.
            collectAssignments(assignmentContainerOld, Mode.OLD, OriginProvider.embedded());
        }

        // TODO what if assignment is both virtual and in delta? It will have virtual flag set to true... MID-6404
        collectVirtualAssignments(virtualAssignments);

        collectDeltaValues(assignmentContainerCurrent, currentAssignmentDelta, deltaItemOriginProvider);

        freezeOrigins();
    }

    private void collectDeltaValues(
            PrismContainer<AssignmentType> assignmentContainerCurrent,
            ContainerDelta<AssignmentType> currentAssignmentDelta,
            @NotNull OriginProvider<? super AssignmentType> deltaItemOriginProvider)
            throws SchemaException {
        if (currentAssignmentDelta != null) {
            if (currentAssignmentDelta.isReplace()) {
                allValues().forEach(v -> v.getOrigin().setNew(false));
                PrismContainer<AssignmentType> assignmentContainerNew =
                        computeAssignmentContainerNew(assignmentContainerCurrent, currentAssignmentDelta);
                // [EP:APSO] DONE, the delta item origin provider is correct
                collectAssignments(assignmentContainerNew, Mode.NEW, deltaItemOriginProvider);
            } else {
                // For performance reasons it is better to process only changes than to process
                // the whole new assignment set (that can have hundreds of assignments)
                // [EP:APSO] DONE, the delta item origin provider is correct
                collectAssignmentsFromAddDeleteDelta(currentAssignmentDelta, deltaItemOriginProvider);
            }
        }
    }

    @Nullable
    private PrismContainer<AssignmentType> getAssignmentContainer(PrismObject<F> object) {
        return object != null ? object.findContainer(FocusType.F_ASSIGNMENT) : null;
    }

    @NotNull
    private PrismContainer<AssignmentType> computeAssignmentContainerNew(
            PrismContainer<AssignmentType> currentContainer,
            ContainerDelta<AssignmentType> delta) throws SchemaException {
        PrismContainer<AssignmentType> newContainer;
        if (currentContainer == null) {
            newContainer = PrismContext.get().getSchemaRegistry()
                    .findContainerDefinitionByCompileTimeClass(AssignmentType.class).instantiate();
        } else {
            newContainer = currentContainer.clone();
        }
        delta.applyToMatchingPath(newContainer);
        return newContainer;
    }

    private void collectAssignments(
            @Nullable PrismContainer<AssignmentType> assignmentContainer,
            @NotNull Mode mode,
            @NotNull OriginProvider<? super AssignmentType> originProvider) // [EP:APSO] DONE 3/3
            throws SchemaException {
        if (assignmentContainer != null) {
            for (PrismContainerValue<AssignmentType> assignmentCVal : assignmentContainer.getValues()) {
                // [EP:APSO] DONE
                collectAssignment(
                        assignmentCVal, mode, false, null, false,
                        originProvider.origin(assignmentCVal.asContainerable()));
            }
        }
    }

    private void collectVirtualAssignments(Collection<AssignmentConfigItem> virtualAssignments)
            throws SchemaException {
        for (ConfigurationItem<AssignmentType> assignment : emptyIfNull(virtualAssignments)) {
            // [EP:APSO] DONE, virtual assignments are already correct (ensured by the caller)
            //noinspection unchecked
            collectAssignment(
                    assignment.value().asPrismContainerValue(),
                    Mode.CURRENT, true, null, false, assignment.origin());
        }
    }

    private void collectAssignmentsFromAddDeleteDelta(
            @NotNull ContainerDelta<AssignmentType> assignmentDelta,
            @NotNull OriginProvider<? super AssignmentType> deltaItemOriginProvider)
            throws SchemaException {
        collectAssignmentsFromDeltaSet(assignmentDelta.getValuesToAdd(), AddDeleteReplace.ADD, deltaItemOriginProvider);
        collectAssignmentsFromDeltaSet(assignmentDelta.getValuesToDelete(), AddDeleteReplace.DELETE, deltaItemOriginProvider);
    }

    private void collectAssignmentsFromDeltaSet(
            Collection<PrismContainerValue<AssignmentType>> assignments,
            AddDeleteReplace deltaSet,
            @NotNull OriginProvider<? super AssignmentType> deltaItemOriginProvider) throws SchemaException {
        for (PrismContainerValue<AssignmentType> assignmentCVal: emptyIfNull(assignments)) {
            boolean doNotCreateNew = deltaSet == AddDeleteReplace.DELETE;
            // [EP:APSO] DONE, delta item origin provider is correct
            collectAssignment(
                    assignmentCVal, Mode.IN_ADD_OR_DELETE_DELTA, false, deltaSet, doNotCreateNew,
                    deltaItemOriginProvider.origin(assignmentCVal.asContainerable()));
        }
    }

    /**
     * `doNotCreateNew` If an assignment does not exist, please DO NOT create it.
     * This flag is used for "delta delete" assignments that do not exist in object current.
     *
     * This also means that delete delta assignments must be processed last.
     *
     * [EP:APSO] DONE 3/3
     */
    private void collectAssignment(
            PrismContainerValue<AssignmentType> assignmentCVal,
            Mode mode, boolean virtual, AddDeleteReplace deltaSet, boolean doNotCreateNew,
            @NotNull ConfigurationItemOrigin origin) throws SchemaException {

        @NotNull SmartAssignmentElement element;

        if (assignmentCVal.isEmpty()) {
            // Special lookup for empty elements.
            // Changed assignments may be "light", i.e. they may contain just the identifier.
            // Make sure that we always have the full assignment data.
            if (assignmentCVal.getId() != null) {
                element = idMap.get(assignmentCVal.getId());
                if (element == null) {
                    // deleting non-existing assignment. Safe to ignore? Yes.
                    return;
                }
            } else {
                throw new SchemaException("Attempt to change empty assignment without ID");
            }
        } else {
            SmartAssignmentElement existingElement = lookup(assignmentCVal);
            if (existingElement != null) {
                element = existingElement;
            } else if (doNotCreateNew) {
                // Deleting non-existing assignment.
                return;
            } else {
                element = put(assignmentCVal, virtual, origin); // [EP:APSO] DONE, see signature
            }
        }

        element.updateOrigin(mode, deltaSet);
    }

    // [EP:APSO] DONE 1/1
    private SmartAssignmentElement put(
            PrismContainerValue<AssignmentType> assignmentCVal, boolean virtual, @NotNull ConfigurationItemOrigin origin) {
        SmartAssignmentElement element = new SmartAssignmentElement(assignmentCVal, virtual, origin);
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

    private int computeInitialCapacity(
            PrismContainer<?> assignmentContainerCurrent, ContainerDelta<?> assignmentDelta, Collection<?> forcedAssignments) {
        return (assignmentContainerCurrent != null ? assignmentContainerCurrent.size() : 0)
                + (assignmentDelta != null ? assignmentDelta.size() : 0)
                + (forcedAssignments != null ? forcedAssignments.size() : 0);
    }

    @NotNull
    @Override
    public Iterator<SmartAssignmentElement> iterator() {
        if (InternalsConfig.getTestingPaths() == TestingPaths.REVERSED) {
            Collection<SmartAssignmentElement> values = allValues();
            List<SmartAssignmentElement> valuesList = new ArrayList<>(values.size());
            valuesList.addAll(values);
            Collections.reverse(valuesList);
            return valuesList.iterator();
        } else {
            return allValues().iterator();
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
            for (SmartAssignmentElement element: allValues()) {
                sb.append("\n");
                sb.append(element.debugDump(indent + 1));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "SmartAssignmentCollection(" + allValues() + ")";
    }

    private void freezeOrigins() {
        allValues().forEach(v -> v.getOrigin().freeze());
    }

    private @NotNull Collection<SmartAssignmentElement> allValues() {
        return aMap.values();
    }

    /** See {@link AssignmentIdStore} for an explanation of how this works. */
    public void generateExternalIds(LensFocusContext<F> focusContext, OperationResult result)
            throws ObjectNotFoundException {
        AssignmentIdStore assignmentIdStore = focusContext.getAssignmentIdStore();

        List<SmartAssignmentElement> toGenerateList = new ArrayList<>();
        Collection<SmartAssignmentElement> allElements = allValues();
        for (SmartAssignmentElement element : allElements) {
            if (element.isNew() && !element.isVirtual() && element.getBuiltInAssignmentId() == null) {
                var id = assignmentIdStore.getKnownExternalId(element.getAssignment());
                if (id != null) {
                    element.setExternalId(id);
                } else {
                    toGenerateList.add(element);
                }
            }
        }
        if (toGenerateList.isEmpty()) {
            return;
        }
        Iterator<Long> allocated = null;
        String focusOid = focusContext.getOid();

        if (!isPrimaryAddNoOverwrite(focusContext) && focusOid != null) {
            // We need to try allocate containers idenitifiers from repository, because repository tries to
            // reuse preexisting containers IDs in add+overwrite for other elements in the objects.
            // Currently, there is no easy way to get options
            try {
                allocated =
                        ModelBeans.get().cacheRepositoryService.allocateContainerIdentifiers(
                                        focusContext.getObjectTypeClass(), focusOid, toGenerateList.size(), result)
                                .iterator();
            } catch (ObjectNotFoundException e) {
                if (!focusContext.isPrimaryAdd()) {
                    // Object should exists, but was not found.
                    throw e;
                }
                // We are doing add and object does not exists which is OK.
            }
        }
        if (allocated == null) {
            // Object is not in repo. Let us provide any IDs that are not used among assignments yet.
            var max = allElements.stream()
                    .filter(element -> !element.isVirtual())
                    .map(element -> {
                        if (element.getBuiltInAssignmentId() == null) {
                            return assignmentIdStore.getKnownExternalId(element.getAssignment());
                        }
                        return element.getBuiltInAssignmentId();
                    })
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
            List<Long> allocatedList = new ArrayList<>();
            for (int i = 0; i < toGenerateList.size(); i++) {
                allocatedList.add(++max);
            }
            allocated = allocatedList.iterator();
        }

        for (SmartAssignmentElement toGenerate : toGenerateList) {
            Long newId = allocated.next();
            toGenerate.setExternalId(newId);
            assignmentIdStore.put(toGenerate.getAssignment(), newId);
        }
    }

    enum Mode {
        CURRENT, OLD, NEW, IN_ADD_OR_DELETE_DELTA
    }

    boolean isPrimaryAddNoOverwrite(LensFocusContext<F> focusContext) {
        return focusContext.isPrimaryAdd() ? !ModelExecuteOptions.isOverwrite(focusContext.getLensContext().getOptions()) : false;
    }
}
