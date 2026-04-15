package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Set;

public abstract class AbstractConnDevActivityHandler<T extends AbstractConnDevActivityHandler.AbstractWorkDefinition, S extends AbstractConnDevActivityHandler<T,S>>
        extends ModelActivityHandler<T, S> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractConnDevActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    private final QName definitionType;
    private final ItemName definitionName;
    private final QName stateType;
    private final Class<? extends AbstractWorkDefinition<?>> definitionClass;
    private final WorkDefinitionFactory.WorkDefinitionSupplier definitionFactory;

    public AbstractConnDevActivityHandler(QName definitionType, ItemName definitionName, QName stateType, Class<? extends AbstractWorkDefinition<?>> definitionClass, WorkDefinitionFactory.WorkDefinitionSupplier definitionFactory) {
        this.definitionType = definitionType;
        this.definitionName = definitionName;
        this.stateType = stateType;
        this.definitionClass = definitionClass;
        this.definitionFactory = definitionFactory;
    }

    @PostConstruct
    public void register() {
        handlerRegistry.register(definitionType,definitionName,definitionClass,definitionFactory,this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(definitionType, definitionClass);
    }

    @Override
    public @NotNull ActivityStateDefinition getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(stateType);
    }

    @Override
    public String getIdentifierPrefix() {
        return definitionName.getLocalPart();
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    /**
     * Returns the set of sibling activity types that this activity should coordinate with.
     * Used by {@link #suspendSiblings} and {@link #waitForSiblingByPolling}.
     */
    protected @NotNull Set<ItemName> getSiblingActivityTypes() {
        return Set.of();
    }

    /**
     * Suspends all non-closed sibling tasks of the given activity types for the same connector development.
     * Should be called on failure to implement fail-fast behavior across parallel tasks.
     */
    protected void suspendSiblings(
            String connectorDevelopmentOid,
            LocalActivityRun<?, ?, ?> activityRun,
            OperationResult result) throws SchemaException {
        var siblingActivityTypes = getSiblingActivityTypes();
        var runningTask = activityRun.getRunningTask();
        var query = PrismContext.get().queryFor(TaskType.class)
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_OBJECTS,
                        BasicObjectSetType.F_OBJECT_REF))
                .ref(connectorDevelopmentOid)
                .build();
        List<String> siblingOids = ConnDevBeans.get().repositoryService
                .searchObjects(TaskType.class, query, null, result)
                .stream()
                .map(o -> o.asObjectable())
                .filter(t -> t.getExecutionState() != TaskExecutionStateType.CLOSED)
                .filter(t -> hasSiblingActivityType(t, siblingActivityTypes))
                .map(TaskType::getOid)
                .filter(oid -> !oid.equals(runningTask.getOid()))
                .toList();
        if (!siblingOids.isEmpty()) {
            LOGGER.info("Suspending {} sibling task(s) due to failure in task {}",
                    siblingOids.size(), runningTask.getName());
            activityRun.getBeans().taskManager.suspendTasks(siblingOids, TaskManager.DO_NOT_WAIT, result);
        }
    }

    private static final long SIBLING_POLL_INTERVAL_MS = 2000L;

    /**
     * To be called after successful completion of the activity's own work.
     * Signals own completion by setting activity resultStatus to SUCCESS, then polls sibling tasks
     * until they also signal completion (via their activity resultStatus), then returns a combined result.
     *
     * If any sibling signals failure (FATAL_ERROR or PARTIAL_ERROR activity resultStatus), returns FATAL_ERROR.
     * If all siblings signal success (SUCCESS activity resultStatus), returns success.
     * If the task is asked to stop, throws {@link SystemException}.
     */
    protected ActivityRunResult waitForSiblingByPolling(
            String connectorDevelopmentOid,
            LocalActivityRun<?, ?, ?> activityRun,
            OperationResult result) throws SchemaException {
        var siblingActivityTypes = getSiblingActivityTypes();

        // Signal that this task's own work is done successfully, so siblings can detect it via polling.
        try {
            activityRun.getActivityState().setResultStatus(OperationResultStatus.SUCCESS);
            activityRun.getActivityState().flushPendingTaskModificationsChecked(result);
        } catch (ActivityRunException e) {
            throw new SystemException("Failed to signal work completion for sibling coordination", e);
        }

        var runningTask = activityRun.getRunningTask();

        var siblingOids = ConnDevBeans.get().repositoryService
                .searchObjects(TaskType.class, PrismContext.get().queryFor(TaskType.class)
                        .item(ItemPath.create(
                                TaskType.F_AFFECTED_OBJECTS,
                                TaskAffectedObjectsType.F_ACTIVITY,
                                ActivityAffectedObjectsType.F_OBJECTS,
                                BasicObjectSetType.F_OBJECT_REF))
                        .ref(connectorDevelopmentOid)
                        .build(), null, result)
                .stream()
                .map(o -> o.asObjectable())
                .filter(t -> !t.getOid().equals(runningTask.getOid()))
                .filter(t -> hasSiblingActivityType(t, siblingActivityTypes))
                .map(TaskType::getOid)
                .toList();

        if (siblingOids.isEmpty()) {
            return ActivityRunResult.success();
        }

        var siblingQuery = PrismContext.get().queryFor(TaskType.class)
                .id(siblingOids.toArray(String[]::new))
                .build();

        while (activityRun.canRun()) {
            var siblings = ConnDevBeans.get().repositoryService
                    .searchObjects(TaskType.class, siblingQuery, null, result)
                    .stream()
                    .map(o -> o.asObjectable())
                    .toList();

            for (TaskType sibling : siblings) {
                var siblingStatus = getSiblingActivityResultStatus(sibling);
                if (siblingStatus == OperationResultStatusType.FATAL_ERROR
                        || siblingStatus == OperationResultStatusType.PARTIAL_ERROR) {
                    result.recordFatalError("Sibling task did not finish successfully: " + sibling.getName());
                    return ActivityRunResult.finished(OperationResultStatus.FATAL_ERROR);
                }
            }

            boolean allDone = siblings.stream()
                    .allMatch(t -> getSiblingActivityResultStatus(t) == OperationResultStatusType.SUCCESS);

            if (allDone) {
                return ActivityRunResult.success();
            }

            try {
                Thread.sleep(SIBLING_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new SystemException("Task was interrupted while waiting for sibling tasks to complete.");
    }

    private static OperationResultStatusType getSiblingActivityResultStatus(TaskType task) {
        var activityState = task.getActivityState();
        if (activityState == null || activityState.getActivity() == null) {
            return null;
        }
        return activityState.getActivity().getResultStatus();
    }

    private static boolean hasSiblingActivityType(TaskType task, Set<ItemName> activityTypes) {
        if (task.getAffectedObjects() == null || task.getAffectedObjects().getActivity().isEmpty()) {
            return false;
        }
        return activityTypes.contains(task.getAffectedObjects().getActivity().get(0).getActivityType());
    }

    public static abstract class AbstractWorkDefinition<T extends ConnDevBaseWorkDefinitionType> extends com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition {

        final String connectorDevelopmentOid;
        final T typedDefinition;

        public AbstractWorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            this(info, true);
        }

        public AbstractWorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info, boolean requireConnectorDevelopment) throws ConfigurationException {
            super(info);
            this.typedDefinition = (T) info.getBean();
            connectorDevelopmentOid = Referencable.getOid(typedDefinition.getConnectorDevelopmentRef());
            if (requireConnectorDevelopment) {
                MiscUtil.configNonNull(connectorDevelopmentOid, "No Connector Development OID specified");
            }
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(@Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException {
            if (connectorDevelopmentOid == null) {
                return AffectedObjectsInformation.ObjectSet.notSupported();
            }
            return AffectedObjectsInformation.ObjectSet.repository(new BasicObjectSetType()
                    .objectRef(connectorDevelopmentOid, ConnectorDevelopmentType.COMPLEX_TYPE)
            );


        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {

        }
    }
}
