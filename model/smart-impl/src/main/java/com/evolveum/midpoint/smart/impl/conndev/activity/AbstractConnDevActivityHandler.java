package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
     * Suspends all non-closed sibling tasks of the given activity types for the same connector development.
     * Should be called on failure to implement fail-fast behavior across parallel tasks.
     */
    protected static void suspendSiblings(
            String connectorDevelopmentOid,
            Set<ItemName> siblingActivityTypes,
            LocalActivityRun<?, ?, ?> activityRun,
            OperationResult result) throws SchemaException {
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
