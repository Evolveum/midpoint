/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.ClusteringAction;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO
 */
@Component
public class RoleAnalysisClusteringActivityHandler
        extends ModelActivityHandler<
        RoleAnalysisClusteringActivityHandler.MyWorkDefinition,
        RoleAnalysisClusteringActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisClusteringActivityHandler.class);

    private static final String OP_EXECUTE = RoleAnalysisClusteringActivityHandler.class.getName() + ".execute";

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                RoleAnalysisClusteringWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_ROLE_ANALYSIS_CLUSTERING,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                RoleAnalysisClusteringWorkDefinitionType.COMPLEX_TYPE, MyWorkDefinition.class);
    }

    @Override
    public String getIdentifierPrefix() {
        return "role-analysis-clustering";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, RoleAnalysisClusteringActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, RoleAnalysisClusteringActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    static class MyActivityRun
            extends LocalActivityRun<MyWorkDefinition, RoleAnalysisClusteringActivityHandler, AbstractActivityWorkStateType> {

        MyActivityRun(
                @NotNull ActivityRunInstantiationContext<MyWorkDefinition, RoleAnalysisClusteringActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult parentResult) {
            RunningTask runningTask = getRunningTask();
            runningTask.setExecutionSupport(this);

            // There are 7 steps; currently, we simply increase the progress value by 1 on each step.
            // Later, we will provide more elaborate progress reporting.
            activityState.getLiveProgress().setExpectedTotal(7);

            // We need to create a subresult in order to be able to determine its status - we have to close it to get the status.
            OperationResult result = parentResult.createSubresult(OP_EXECUTE);

            try {
                String sessionOid = getWorkDefinition().sessionOid;
                LOGGER.debug("Running role analysis clustering activity; session OID = {}", sessionOid);

                ClusteringAction clusteringAction = new ClusteringAction(this);
                clusteringAction.execute(sessionOid, result);

            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                runningTask.setExecutionSupport(null);
                result.close();
            }

            return standardRunResult(result.getStatus());
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition {

        @NotNull private final String sessionOid;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (RoleAnalysisClusteringWorkDefinitionType) info.getBean();
            sessionOid = configNonNull(
                    Referencable.getOid(typedDefinition.getSessionRef()),
                    "No session OID in work definition in %s", info.origin());
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabel(sb, "sessionOid", sessionOid, indent + 1);
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation() {
            return AffectedObjectsInformation.ObjectSet.notSupported();
        }
    }
}
