/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.*;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.DelegatingActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.DistributingActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Binds together all the information about an activity and its run (if present).
 *
 * The {@link Activity} itself is a binder for (shortly speaking):
 *
 * . ({@link #definition}) {@link ActivityDefinition}
 * . ({@link #run}) {@link AbstractActivityRun} (optional - only if we are dealing with the currently executing activity)
 *   * {@link ActivityState};
 * . ({@link #tree}) {@link ActivityTree}
 *   * {@link ActivityTreeStateOverview}
 * . {@link ActivityHandler}
 *
 * @param <WD> Type of the work definition object
 * @param <AH> Type of the activity handler object
 */
public abstract class Activity<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        implements DebugDumpable {

    private static final @NotNull ItemPath TASK_ROLE_PATH =
            ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_TASK_ROLE);

    /**
     * Identifier of the activity. It is unique at the current level in the activity tree.
     * May be defined in {@link ActivityDefinition#explicitlyDefinedIdentifier}, but usually it is generated
     * when the list of sub-activities is finalized.
     *
     * See {@link #setupIdentifiers(List)}.
     */
    private String identifier;

    /** Definition of the activity. Parsed from XML of the activity and from the legacy task extension. */
    @NotNull private final ActivityDefinition<WD> definition;

    /** Run of the activity. May be null. The current activity state is here. */
    private AbstractActivityRun<WD, AH, ?> run;

    /** Reference to the tree object. The tree state is here. */
    @NotNull private final ActivityTree tree;

    /**
     * Is this activity the local root i.e. top-level activity running in the current task?
     * If the task is a root task, this implies the activity is the global root of the activity tree.
     * But if the task is a subtask, the activity may be a sub-activity.
     */
    private boolean localRoot;

    /**
     * References to the children, indexed by their identifier.
     *
     * Thread safety: Must be synchronized because of external access in {@link TaskHandler#heartbeat(Task)} method.
     */
    @NotNull private final Map<String, Activity<?, ?>> childrenMap = Collections.synchronizedMap(new LinkedHashMap<>());

    /** Was {@link #childrenMap} already initialized? TODO move to a special class with the map. */
    private boolean childrenMapInitialized;

    /** (Global) path of this activity in the activity tree. */
    @NotNull private final Lazy<ActivityPath> pathLazy = Lazy.from(this::computePath);

    /** (Local) path of this activity in the current task. */
    @NotNull private final Lazy<ActivityPath> localPathLazy = Lazy.from(this::computeLocalPath);

    Activity(@NotNull ActivityDefinition<WD> definition, @NotNull ActivityTree tree) {
        this.definition = definition;
        this.tree = tree;
    }

    public String getIdentifier() {
        return identifier;
    }

    public @NotNull ActivityDefinition<WD> getDefinition() {
        return definition;
    }

    public @NotNull WD getWorkDefinition() {
        return definition.getWorkDefinition();
    }

    public @NotNull ActivityDistributionDefinition getDistributionDefinition() {
        return definition.getDistributionDefinition();
    }

    public @NotNull ActivityReportingDefinition getReportingDefinition() {
        return definition.getReportingDefinition();
    }

    public @NotNull ActivityControlFlowDefinition getControlFlowDefinition() {
        return definition.getControlFlowDefinition();
    }

    public abstract @NotNull AH getHandler();

    /**
     * Returns objects that create {@link AbstractActivityRun} objects for this activity.
     * It is used in cases where the activity runs locally i.e. is not delegated nor distributed.
     *
     * See {@link ActivityRunSupplier}.
     */
    protected abstract @NotNull ActivityRunSupplier<WD, AH> getLocalRunSupplier();

    /** Returns objects that suggest child activity identifiers. */
    protected abstract @NotNull CandidateIdentifierFormatter getCandidateIdentifierFormatter();

    public abstract @NotNull ActivityStateDefinition<?> getActivityStateDefinition();

    public AbstractActivityRun<WD, AH, ?> getRun() {
        return run;
    }

    public @NotNull ActivityTree getTree() {
        return tree;
    }

    public void setLocalRoot() {
        this.localRoot = true;
    }

    public abstract Activity<?, ?> getParent();

    public @NotNull List<Activity<?, ?>> getChildrenCopy() {
        synchronized (childrenMap) {
            return new ArrayList<>(childrenMap.values());
        }
    }

    private @NotNull Map<String, Activity<?, ?>> getChildrenMapCopy() {
        synchronized (childrenMap) {
            return new HashMap<>(childrenMap);
        }
    }

    public @NotNull List<Activity<?, ?>> getChildrenCopyExceptSkipped() {
        synchronized (childrenMap) {
            return childrenMap.values().stream()
                    .filter(child -> !child.isSkipped())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getDebugDumpLabel(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "definition", definition, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "run", run, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "parent", String.valueOf(getParent()), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "path", String.valueOf(getPath()), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "local path", String.valueOf(getLocalPath()), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "children (initialized=" + childrenMapInitialized + ")",
                getChildrenMapCopy(), indent + 1);
        return sb.toString();
    }

    private @NotNull String getDebugDumpLabel() {
        return getClass().getSimpleName() + " [identifier '" + identifier + "']" +
                (isRoot() ? " (root)" : "") +
                (isLocalRoot() ? " (local root)" : "");
    }

    /** Creates and sets the activity run ({@link #run}). */
    public @NotNull AbstractActivityRun<?, ?, ?> createRun(ActivityBasedTaskRun taskRun, OperationResult result) {
        stateCheck(run == null, "Run is already created in %s", this);
        ActivityRunInstantiationContext<WD, AH> context = new ActivityRunInstantiationContext<>(this, taskRun);
        RunType runType = determineRunType(taskRun.getRunningTask());
        switch (runType) {
            case LOCAL:
                run = getLocalRunSupplier()
                        .createActivityRun(context, result);
                break;
            case DELEGATING:
                run = new DelegatingActivityRun<>(context);
                break;
            case DISTRIBUTING:
                run = new DistributingActivityRun<>(context);
                break;
            default:
                throw new AssertionError(runType);
        }
        return run;
    }

    private @NotNull RunType determineRunType(Task activityTask) {
        if (doesTaskExecuteThisActivityAsWorker(activityTask)) {
            // This is a worker. It must be local execution then.
            return RunType.LOCAL;
        }

        if (definition.getDistributionDefinition().isSubtask() && !doesTaskExecuteThisActivityAsDelegate(activityTask)) {
            return RunType.DELEGATING;
        } else if (definition.getDistributionDefinition().hasWorkers()) {
            return RunType.DISTRIBUTING;
        } else {
            return RunType.LOCAL;
        }
    }

    private boolean doesTaskExecuteThisActivityAsDelegate(Task activityTask) {
        return isLocalRoot() && isTaskRoleDelegate(activityTask);
    }

    private boolean isTaskRoleDelegate(Task activityTask) {
        return getRoleOfTask(activityTask) == TaskRoleType.DELEGATE;
    }

    private boolean doesTaskExecuteThisActivityAsWorker(Task activityTask) {
        // Actually, the worker tasks cannot have a local child activity, so isLocalRoot should always be true.
        return isLocalRoot() && isTaskRoleWorker(activityTask);
    }

    private boolean isTaskRoleWorker(Task activityTask) {
        return getRoleOfTask(activityTask) == TaskRoleType.WORKER;
    }

    public boolean doesTaskExecuteTreeRootActivity(Task activityTask) {
        return isRoot() && getRoleOfTask(activityTask) == null;
    }

    private TaskRoleType getRoleOfTask(Task activityTask) {
        return activityTask.getPropertyRealValue(TASK_ROLE_PATH, TaskRoleType.class);
    }

    public @NotNull Activity<?, ?> getChild(String identifier) throws SchemaException {
        initializeChildrenMapIfNeeded();
        Activity<?, ?> child = childrenMap.get(identifier);
        if (child != null) {
            return child;
        } else {
            throw new IllegalArgumentException(
                    String.format("Child with identifier %s was not found among children of %s. Known children are: %s",
                            identifier, this, getChildrenMapCopy().keySet()));
        }
    }

    public void initializeChildrenMapIfNeeded() throws SchemaException {
        synchronized (childrenMap) { // just for sure
            if (!childrenMapInitialized) {
                assert childrenMap.isEmpty();
                createChildren();
                childrenMapInitialized = true;
            }
        }
    }

    /** Creates children in {@link #childrenMap}. The caller must hold the lock on {@link #childrenMap}. */
    private void createChildren() throws SchemaException {
        ArrayList<Activity<?, ?>> childrenList = getHandler().createChildActivities(this);
        setupIdentifiers(childrenList);
        tailorChildren(childrenList);
        setupIdentifiers(childrenList);
        childrenList.forEach(this::addChild);
    }

    private void addChild(@NotNull Activity<?, ?> child) {
        var previous = childrenMap.put(child.getIdentifier(), child);
        stateCheck(previous == null,
                "Multiple child activities with the same identifier: %s (%s, %s)",
                child.getIdentifier(), child, previous);
    }

    private void setupIdentifiers(List<Activity<?, ?>> childrenList) {
        for (Activity<?, ?> child : childrenList) {
            child.setupIdentifier(childrenList);
        }
    }

    private void setupIdentifier(List<Activity<?, ?>> siblingsList) {
        if (identifier != null) {
            return; // can occur on repeated runs
        }

        String explicitlyDefined = definition.getExplicitlyDefinedIdentifier();
        if (explicitlyDefined != null) {
            identifier = explicitlyDefined;
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

    /** Is this activity the (global) root of the activity tree? */
    public boolean isRoot() {
        return getParent() == null;
    }

    /** Is this activity the local root i.e. root of run in the current task? */
    public boolean isLocalRoot() {
        return localRoot;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "path='" + getPath() + '\'' +
                '}';
    }

    public @NotNull ActivityPath getPath() {
        return pathLazy.get();
    }

    private @NotNull ActivityPath computePath() {
        LinkedList<String> identifiers = new LinkedList<>();
        Activity<?, ?> current = this;
        while (!current.isRoot()) {
            identifiers.add(0, current.getIdentifier());
            current = current.getParent();
        }
        return ActivityPath.fromList(identifiers);
    }

    public @Nullable ActivityPath getLocalPath() {
        return localPathLazy.get();
    }

    private @Nullable ActivityPath computeLocalPath() {
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

    public ActivityErrorHandlingStrategyType getErrorHandlingStrategy() {
        // TODO implement inheritance of the error handling strategy among activities
        return definition.getControlFlowDefinition().getErrorHandlingStrategy();
    }

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        definition.applyChangeTailoring(tailoring);
    }

    void applySubtaskTailoring(@NotNull ActivitySubtaskDefinitionType subtaskSpecification) {
        definition.applySubtaskTailoring(subtaskSpecification);
    }

    public void accept(@NotNull ActivityVisitor visitor) {
        visitor.visit(this);
        getChildrenCopy()
                .forEach(child -> child.accept(visitor));
    }

    public boolean isSkipped() {
        return definition.getControlFlowDefinition().isSkip();
    }

    public @NotNull ExecutionModeType getExecutionMode() {
        return definition.getExecutionMode();
    }

    private enum RunType {
        LOCAL, DELEGATING, DISTRIBUTING
    }
}
