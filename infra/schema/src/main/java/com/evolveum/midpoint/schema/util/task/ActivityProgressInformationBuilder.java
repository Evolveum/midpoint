/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation.RealizationState;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformation.unknown;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.isDelegated;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateOverviewProgressInformationVisibilityType.HIDDEN;

/**
 * Builds {@link ActivityProgressInformation} from task tree or from root task with tree overview.
 */
public class ActivityProgressInformationBuilder {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityProgressInformation.class);

    /** The whole activity tree. The same for all related builders. */
    @Nullable private final ActivityTreeStateType tree;

    /** Current task being processed. Changes among builders. */
    @NotNull private final TaskType task;

    /** Current activity identifier being processed. */
    @Nullable private final String activityIdentifier;

    /** Current activity path being processed. */
    @NotNull private final ActivityPath activityPath;

    /** Full state for the current activity. Null if not known or not used. */
    @Nullable private final ActivityStateType fullActivityState;

    /** State overview for the current activity. Null if not known or not used. */
    @Nullable private final ActivityStateOverviewType activityStateOverview;

    @NotNull private final TaskResolver resolver;
    @NotNull private final InformationSource informationSource;

    private ActivityProgressInformationBuilder(
            @Nullable ActivityTreeStateType tree,
            @NotNull TaskType task,
            @Nullable String activityIdentifier,
            @NotNull ActivityPath activityPath,
            @Nullable ActivityStateType fullActivityState,
            @NotNull TaskResolver resolver,
            @NotNull InformationSource informationSource) {
        this.tree = tree;
        this.task = task;
        this.activityIdentifier = activityIdentifier;
        this.activityPath = activityPath;
        this.fullActivityState = fullActivityState;
        this.activityStateOverview = getStateOverview();
        this.resolver = resolver;
        this.informationSource = informationSource;
    }

    public static @NotNull ActivityProgressInformation fromTask(@NotNull TaskType task, @NotNull ActivityPath activityPath,
            @NotNull TaskResolver resolver, @NotNull InformationSource informationSource) {
        TaskActivityStateType globalState = task.getActivityState();
        if (globalState == null) {
            return unknown(null, activityPath);
        }
        ActivityStateType rootActivityState = globalState.getActivity();
        if (rootActivityState == null) {
            return unknown(null, activityPath);
        }
        return new ActivityProgressInformationBuilder(
                globalState.getTree(),
                task,
                rootActivityState.getIdentifier(),
                activityPath,
                rootActivityState,
                resolver,
                informationSource)
                .build();
    }

    public ActivityProgressInformation build() {
        ChildrenContinuation continuation = buildCurrentWithContinuation();
        addChildren(continuation);
        return continuation.parent;
    }

    private void addChildren(@NotNull ChildrenContinuation continuation) {
        for (Child child : continuation.children) {
            continuation.parent.children.add(
                    new ActivityProgressInformationBuilder(
                            tree,
                            continuation.task,
                            child.identifier,
                            child.path,
                            child.fullActivityState,
                            resolver,
                            informationSource)
                            .build());
        }
    }

    private ChildrenContinuation buildCurrentWithContinuation() {
        switch (informationSource) {
            case TREE_OVERVIEW_ONLY:
                return continuationFromOverview();
            case TREE_OVERVIEW_PREFERRED:
                return hasProgressInOverview() ? continuationFromOverview() : continuationFromFullState();
            case FULL_STATE_PREFERRED:
                return hasProgressInFullState() ? continuationFromFullState() : continuationFromOverview();
            case FULL_STATE_ONLY:
                return continuationFromFullState();
            default:
                throw new AssertionError(informationSource);
        }
    }

    private boolean hasProgressInOverview() {
        return activityStateOverview != null && activityStateOverview.getProgressInformationVisibility() != HIDDEN;
    }

    /**
     * We assume that if (any) subtasks are present, then all subtasks are present, and we have nothing to load.
     */
    private boolean hasProgressInFullState() {
        return fullActivityState != null &&
                (subtasksPresent() || !isDelegated(fullActivityState) && !BucketingUtil.isCoordinator(fullActivityState));
    }

    private boolean subtasksPresent() {
        return !task.getSubtaskRef().isEmpty();
    }

    private ChildrenContinuation continuationFromFullState() {
        if (fullActivityState == null) {
            return continuation(
                    currentUnknown());
        } else if (isDelegated(fullActivityState)) {
            return continuationFromDelegatedActivityState(
                    getDelegatedTaskRef(fullActivityState));
        } else {
            return continuation(
                    currentFromNotDelegatedActivityState());
        }
    }

    private @NotNull ChildrenContinuation continuationFromDelegatedActivityState(ObjectReferenceType delegateTaskRef) {
        TaskType delegateTask = getSubtaskIfResolvable(delegateTaskRef);
        if (delegateTask == null) {
            return continuation(
                    currentUnknown());
        }

        TaskActivityStateType globalState = delegateTask.getActivityState();
        if (globalState == null) {
            return continuation(
                    currentUnknown());
        }
        ActivityStateType rootActivityState = globalState.getActivity();
        if (rootActivityState == null) {
            return continuation(
                    currentUnknown());
        }
        return new ActivityProgressInformationBuilder(tree, delegateTask,
                rootActivityState.getIdentifier(), activityPath, rootActivityState,
                resolver, informationSource)
                .buildCurrentWithContinuation();
    }

    @NotNull
    private ActivityProgressInformation currentUnknown() {
        return unknown(activityIdentifier, activityPath);
    }

    private TaskType getSubtaskIfResolvable(ObjectReferenceType subtaskRef) {
        String subTaskOid = subtaskRef != null ? subtaskRef.getOid() : null;
        if (subTaskOid == null) {
            return null;
        }
        TaskType inTask = TaskTreeUtil.findChildIfResolved(task, subTaskOid);
        if (inTask != null) {
            return inTask;
        }
        try {
            return resolver.resolve(subTaskOid);
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("The resolver does not support resolution of subtasks. Subtask OID: {}", subTaskOid);
            return null;
        } catch (ObjectNotFoundException | SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't retrieve subtask {} of {}", e, subTaskOid, task);
            return null;
        }
    }

    private static ObjectReferenceType getDelegatedTaskRef(ActivityStateType state) {
        AbstractActivityWorkStateType workState = state.getWorkState();
        return workState instanceof DelegationWorkStateType ?
                ((DelegationWorkStateType) workState).getTaskRef() : null;
    }

    @NotNull
    private ActivityProgressInformation currentFromNotDelegatedActivityState() {
        stateCheck(hasProgressInFullState(), "Full activity state is not present");
        assert fullActivityState != null;
        return new ActivityProgressInformation(
                fullActivityState.getIdentifier(),
                activityPath,
                RealizationState.fromFullState(fullActivityState.getRealizationState()),
                BucketsProgressInformation.fromFullState(fullActivityState),
                ItemsProgressInformation.fromFullState(fullActivityState, activityPath, task, resolver));
    }

    private @NotNull ChildrenContinuation continuationFromOverview() {
        ActivityProgressInformation current;
        if (activityStateOverview == null) {
            current = currentUnknown();
        } else {
            current = new ActivityProgressInformation(
                    activityStateOverview.getIdentifier(),
                    activityPath,
                    RealizationState.fromOverview(activityStateOverview.getRealizationState()),
                    BucketsProgressInformation.fromOverview(activityStateOverview),
                    ItemsProgressInformation.fromOverview(activityStateOverview));
        }
        return continuation(current);
    }

    private @NotNull ChildrenContinuation continuation(@NotNull ActivityProgressInformation current) {
        return new ChildrenContinuation(current, task, getChildren());
    }

    private @Nullable ActivityStateOverviewType getStateOverview() {
        if (tree == null || tree.getActivity() == null) {
            return null;
        } else {
            return ActivityStateOverviewUtil.findEntry(tree.getActivity(), activityPath);
        }
    }

    private List<Child> getChildren() {
        switch (informationSource) {
            case TREE_OVERVIEW_ONLY:
                return childrenFromOverview();
            case TREE_OVERVIEW_PREFERRED:
                return merge(
                        childrenFromOverview(),
                        childrenFromFullState());
            case FULL_STATE_PREFERRED:
                return merge(
                        childrenFromFullState(),
                        childrenFromOverview());
            case FULL_STATE_ONLY:
                return childrenFromFullState();
            default:
                throw new AssertionError(informationSource);
        }
    }

    private List<Child> childrenFromFullState() {
        if (fullActivityState == null) {
            return List.of();
        } else {
            return fullActivityState.getActivity().stream()
                    .filter(subState -> subState.getIdentifier() != null)
                    .map(subState -> Child.fromFullState(activityPath, subState))
                    .collect(Collectors.toList());
        }
    }

    private List<Child> childrenFromOverview() {
        if (activityStateOverview == null) {
            return List.of();
        } else {
            return activityStateOverview.getActivity().stream()
                    .filter(subOverview -> subOverview.getIdentifier() != null)
                    .map(subOverview -> Child.fromOverview(activityPath, subOverview))
                    .collect(Collectors.toList());
        }
    }

    private List<Child> merge(List<Child> base, List<Child> toMerge) {
        List<Child> allChildren = new ArrayList<>(base);
        for (Child childToMerge : toMerge) {
            boolean matched = false;
            for (Child ch : allChildren) {
                if (ch.matches(childToMerge)) {
                    ch.updateFrom(childToMerge);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                allChildren.add(childToMerge);
            }
        }
        return allChildren;
    }

    private static class Child {
        @NotNull private final ActivityPath path;
        @Nullable private final String identifier;
        @Nullable private ActivityStateType fullActivityState;

        private Child(@NotNull ActivityPath path, @Nullable String identifier, @Nullable ActivityStateType fullActivityState) {
            this.path = path;
            this.identifier = identifier;
            this.fullActivityState = fullActivityState;
        }

        static Child fromFullState(@NotNull ActivityPath parentPath, @NotNull ActivityStateType state) {
            return new Child(
                    parentPath.append(state.getIdentifier()),
                    state.getIdentifier(),
                    state);
        }

        static Child fromOverview(@NotNull ActivityPath parentPath, @NotNull ActivityStateOverviewType overview) {
            return new Child(
                    parentPath.append(overview.getIdentifier()),
                    overview.getIdentifier(),
                    null);
        }

        boolean matches(Child other) {
            return path.equals(other.path) && Objects.equals(identifier, other.identifier);
        }

        void updateFrom(Child other) {
            if (fullActivityState == null) {
                this.fullActivityState = other.fullActivityState;
            }
        }
    }

    /** Where we should continue when looking for children. TODO better name! */
    private static class ChildrenContinuation {

        /** Information about the parent activity. */
        @NotNull private final ActivityProgressInformation parent;

        /** Task where parent activity information resides (can be root task we use overview). */
        @NotNull private final TaskType task;

        /** List of children to visit. */
        @NotNull private final List<Child> children;

        private ChildrenContinuation(@NotNull ActivityProgressInformation parent,
                @NotNull TaskType task, @NotNull List<Child> children) {
            this.parent = parent;
            this.task = task;
            this.children = children;
        }
    }

    public enum InformationSource {
        /**
         * When constructing the progress information we look at tree overview only.
         * The full state is ignored.
         */
        TREE_OVERVIEW_ONLY,

        /**
         * When looking for current progress, we take tree overview first (if the progress is present there),
         * and only then we look at the full state.
         */
        TREE_OVERVIEW_PREFERRED,

        /**
         * When looking for current progress, we take full state first (if it exists). Only then we look at the
         * tree overview.
         */
        FULL_STATE_PREFERRED,

        /**
         * We look at full state only. For worker tasks this assumes that subtasks are loaded.
         * (Otherwise they couldn't be found.)
         */
        FULL_STATE_ONLY
    }
}
