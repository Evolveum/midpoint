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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.mining.algorithm.detection.DetectionActionExecutorNew;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO
 */
@Component
public class RoleAnalysisPatternDetectionActivityHandler
        extends ModelActivityHandler<
        RoleAnalysisPatternDetectionActivityHandler.MyWorkDefinition,
        RoleAnalysisPatternDetectionActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisPatternDetectionActivityHandler.class);

    private static final String OP_EXECUTE = RoleAnalysisPatternDetectionActivityHandler.class.getName() + ".execute";

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                RoleAnalysisPatternDetectionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_ROLE_ANALYSIS_PATTERN_DETECTION,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                RoleAnalysisPatternDetectionWorkDefinitionType.COMPLEX_TYPE, MyWorkDefinition.class);
    }

    @Override
    public @NotNull ModelBeans getModelBeans() {
        return super.getModelBeans();
    }

    @Override
    public String getIdentifierPrefix() {
        return "role-analysis-pattern-detection";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, RoleAnalysisPatternDetectionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, RoleAnalysisPatternDetectionActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    class MyActivityRun
            extends LocalActivityRun<MyWorkDefinition, RoleAnalysisPatternDetectionActivityHandler, AbstractActivityWorkStateType> {

        MyActivityRun(
                @NotNull ActivityRunInstantiationContext<MyWorkDefinition, RoleAnalysisPatternDetectionActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult parentResult) {
            RunningTask runningTask = getRunningTask();
            runningTask.setExecutionSupport(this);

            // We need to create a subresult in order to be able to determine its status - we have to close it to get the status.
            OperationResult result = parentResult.createSubresult(OP_EXECUTE);
            try {
                LOGGER.info(
                        "Running role analysis pattern detection activity - FIXME add the implementation; cluster OID: {}",
                        getWorkDefinition().clusterOid);

                // FIXME add the implementation here
                TaskManager taskManager = getModelBeans().taskManager;
                ModelService modelService = getModelBeans().modelService;
                Task task = taskManager.createTaskInstance("DetectionActionActivity");

//                RepositoryService repositoryService = getBeans().repositoryService;
                new DetectionActionExecutorNew(getWorkDefinition().clusterOid, modelService, result, task)
                        .executeDetectionProcess();

            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                runningTask.setExecutionSupport(null);
                result.close();
            }

            OperationResultStatus status = result.getStatus();

            // We increase the progress only from 0 to 1.
            incrementProgress(new QualifiedItemProcessingOutcomeType()
                    .outcome(status.isError() ? ItemProcessingOutcomeType.FAILURE : ItemProcessingOutcomeType.SUCCESS));

            return standardRunResult(status);
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition {

        @NotNull private final String clusterOid;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (RoleAnalysisPatternDetectionWorkDefinitionType) info.getBean();
            clusterOid = configNonNull(
                    Referencable.getOid(typedDefinition.getClusterRef()),
                    "No cluster OID in work definition in %s", info.origin());
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabel(sb, "clusterOid", clusterOid, indent + 1);
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation() {
            return AffectedObjectsInformation.ObjectSet.notSupported();
        }
    }
}
