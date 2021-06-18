/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.execution.DelegatingActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.DistributingActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Binds together all the information about an activity and its execution (if present).
 *
 * @param <WD> Type of the work definition object
 * @param <AH> Type of the activity handler object
 */
public abstract class Activity<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        implements DebugDumpable {

    /**
     * Identifier of the activity. It is unique at the current level in the activity tree.
     */
    private String identifier;

    /**
     * Definition of the activity.
     */
    @NotNull private final ActivityDefinition<WD> definition;

    /**
     * Execution of the activity. May be null.
     */
    private AbstractActivityExecution<WD, AH, ?> execution;

    /**
     * Reference to the tree object.
     */
    @NotNull private final ActivityTree tree;

    /** TODO */
    private boolean localRoot;

    /**
     * References to the children, indexed by their identifier.
     */
    @NotNull private final LinkedHashMap<String, Activity<?, ?>> childrenMap = new LinkedHashMap<>();

    private boolean childrenMapInitialized;

    @NotNull private final Lazy<ActivityPath> pathLazy = Lazy.from(this::computePath);

    @NotNull private final Lazy<ActivityPath> localPathLazy = Lazy.from(this::computeLocalPath);

    Activity(@NotNull ActivityDefinition<WD> definition, @NotNull ActivityTree tree) {
        this.definition = definition;
        this.tree = tree;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @NotNull
    public ActivityDefinition<WD> getDefinition() {
        return definition;
    }

    @NotNull
    public WD getWorkDefinition() {
        return definition.getWorkDefinition();
    }

    @NotNull
    public abstract AH getHandler();

    @NotNull
    protected abstract ExecutionSupplier<WD, AH> getLocalExecutionSupplier();

    @NotNull
    protected abstract CandidateIdentifierFormatter getCandidateIdentifierFormatter();

    public AbstractActivityExecution<WD, AH, ?> getExecution() {
        return execution;
    }

    @NotNull
    public ActivityTree getTree() {
        return tree;
    }

    public void setLocalRoot(boolean localRoot) {
        this.localRoot = localRoot;
    }

    public abstract Activity<?, ?> getParent();

    @NotNull
    public LinkedHashMap<String, Activity<?, ?>> getChildrenMap() {
        return childrenMap;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getDebugDumpLabel(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "definition", definition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "execution", execution, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "parent", String.valueOf(getParent()), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "path", String.valueOf(getPath()), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "local path", String.valueOf(getLocalPath()), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "children (initialized=" + childrenMapInitialized + ")", childrenMap, indent + 1);
        return sb.toString();
    }

    @NotNull
    private String getDebugDumpLabel() {
        return getClass().getSimpleName() + " [identifier '" + identifier + "']" +
                (isRoot() ? " (root)" : "") +
                (isLocalRoot() ? " (local root)" : "");
    }

    public AbstractActivityExecution<?, ?, ?> createExecution(TaskExecution taskExecution, OperationResult result) {
        stateCheck(execution == null, "Execution is already created in %s", this);
        ExecutionInstantiationContext<WD, AH> context = new ExecutionInstantiationContext<>(this, taskExecution);
        ExecutionType executionType = determineExecutionType(taskExecution);
        switch (executionType) {
            case LOCAL:
                execution = getLocalExecutionSupplier()
                        .createExecution(context, result);
                break;
            case DELEGATING:
                execution = new DelegatingActivityExecution<>(context);
                break;
            case DISTRIBUTING:
                execution = new DistributingActivityExecution<>(context);
                break;
            default:
                throw new AssertionError(executionType);
        }
        return execution;
    }

    private @NotNull ExecutionType determineExecutionType(TaskExecution taskExecution) {
        if (definition.getDistributionDefinition().isSubtask()) {
            return isInDelegatedExecution(taskExecution) ? ExecutionType.LOCAL : ExecutionType.DELEGATING;
        } else if (definition.getDistributionDefinition().hasWorkers()) {
            return isInDistributedExecution(taskExecution) ? ExecutionType.LOCAL : ExecutionType.DISTRIBUTING;
        } else {
            return ExecutionType.LOCAL;
        }
    }

    private boolean isInDelegatedExecution(TaskExecution taskExecution) {
        return isLocalRoot() && isRoleDelegate(taskExecution);
    }

    private boolean isRoleDelegate(TaskExecution taskExecution) {
        return getRole(taskExecution) == ActivityExecutionRoleType.DELEGATE;
    }

    private boolean isInDistributedExecution(TaskExecution taskExecution) {
        // Actually, the worker tasks cannot have a local child activity, so isLocalRoot should always be true.
        return isLocalRoot() && isRoleWorker(taskExecution);
    }

    private boolean isRoleWorker(TaskExecution taskExecution) {
        return getRole(taskExecution) == ActivityExecutionRoleType.WORKER;
    }

    private ActivityExecutionRoleType getRole(TaskExecution taskExecution) {
        return taskExecution.getRunningTask()
                .getPropertyRealValue(
                        ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ROLE),
                        ActivityExecutionRoleType.class);
    }

    @NotNull
    public Activity<?, ?> getChild(String identifier) throws SchemaException {
        initializeChildrenMapIfNeeded();
        Activity<?, ?> child = childrenMap.get(identifier);
        if (child != null) {
            return child;
        } else {
            throw new IllegalArgumentException("Child with identifier " + identifier + " was not found among children of "
                    + this + ". Known children are: " + childrenMap.keySet());
        }
    }

    public void initializeChildrenMapIfNeeded() throws SchemaException {
        if (!childrenMapInitialized) {
            assert childrenMap.isEmpty();
            createChildren();
            childrenMapInitialized = true;
        }
    }

    private void createChildren() throws SchemaException {
        ArrayList<Activity<?, ?>> childrenList = getHandler().createChildActivities(this);
        setupIdentifiers(childrenList);
        tailorChildren(childrenList);
        setupIdentifiers(childrenList);
        childrenList.forEach(child -> childrenMap.put(child.getIdentifier(), child));
    }

    private void setupIdentifiers(List<Activity<?, ?>> childrenList) {
        for (Activity<?, ?> child : childrenList) {
            child.setupIdentifier(childrenList);
        }
    }

    private void setupIdentifier(List<Activity<?, ?>> siblingsList) {
        if (identifier != null) {
            return; // can occur on repeated executions
        }

        String defined = definition.getIdentifier();
        if (defined != null) {
            identifier = defined;
            return;
        }

        identifier = generateNextIdentifier(siblingsList);
    }

    // TODO implement seriously
    private String generateNextIdentifier(List<Activity<?, ?>> siblingsList) {
        Set<String> existing = siblingsList.stream()
                .map(Activity::getIdentifier)
                .collect(Collectors.toSet());

        int limit = 10000;

        String previousCandidate = null;
        for (int i = 1; i < limit; i++) {
            String candidate = getCandidateIdentifierFormatter().formatCandidateIdentifier(i);
            if (!existing.contains(candidate)) {
                return candidate;
            }
            if (previousCandidate != null && previousCandidate.equals(candidate)) {
                throw new IllegalStateException("Couldn't generate unique identifier: previous candidate and the current "
                        + "one are equal ('" + candidate + "') and in conflict with one of existing identifiers: " + existing);
            }
            previousCandidate = candidate;
        }
        throw new IllegalStateException("Unique identifier couldn't be generated even after " + limit + " attempts");
    }

    /**
     * Executes tailoring instructions, i.e. inserts new activities before/after specified ones,
     * or changes the configuration of specified activities.
     */
    private void tailorChildren(ArrayList<Activity<?, ?>> childrenList) throws SchemaException {
        new ActivityTailor(this, childrenList)
                .execute();
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    /**
     * Is this activity the local root i.e. root of execution in the current task?
     */
    public boolean isLocalRoot() {
        return localRoot;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "path='" + getPath() + '\'' +
                '}';
    }

    @NotNull
    public ActivityPath getPath() {
        return pathLazy.get();
    }

    @NotNull
    private ActivityPath computePath() {
        LinkedList<String> identifiers = new LinkedList<>();
        Activity<?, ?> current = this;
        while (!current.isRoot()) {
            identifiers.add(0, current.getIdentifier());
            current = current.getParent();
        }
        return ActivityPath.fromList(identifiers);
    }

    @Nullable
    public ActivityPath getLocalPath() {
        return localPathLazy.get();
    }

    @Nullable
    private ActivityPath computeLocalPath() {
        LinkedList<String> identifiers = new LinkedList<>();
        Activity<?, ?> current = this;
        while (!current.isLocalRoot()) {
            identifiers.add(0, current.getIdentifier());
            current = current.getParent();
            if (current == null) {
                // This means we are outside local root
                return null;
            }
        }
        return ActivityPath.fromList(identifiers);
    }

    public TaskErrorHandlingStrategyType getErrorHandlingStrategy() {
        // TODO implement inheritance of the error handling strategy among activities
        return definition.getControlFlowDefinition().getErrorHandlingStrategy();
    }

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        definition.applyChangeTailoring(tailoring);
    }

    private enum ExecutionType {
        LOCAL, DELEGATING, DISTRIBUTING
    }
}
