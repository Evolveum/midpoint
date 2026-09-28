/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.ClusterwideCacheInvalidationDispatcher;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InitialDataImportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

/** Activity handler that restores the initial objects after a repository factory reset. */
public class InitialDataImportActivityHandler
        implements ActivityHandler<InitialDataImportActivityHandler.InitialDataImportWorkDefinition, InitialDataImportActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImportActivityHandler.class);

    @Autowired private ActivityHandlerRegistry handlerRegistry;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private InitialDataImport initialDataImport;
    @Autowired private ClusterwideCacheInvalidationDispatcher cacheDispatcher;
    @Autowired private ModelService modelService;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                InitialDataImportWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_INITIAL_DATA_IMPORT,
                InitialDataImportWorkDefinition.class,
                InitialDataImportWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                InitialDataImportWorkDefinitionType.COMPLEX_TYPE,
                InitialDataImportWorkDefinition.class);
    }

    @Override
    public String getIdentifierPrefix() {
        return "initial-data-import";
    }

    @Override
    public AbstractActivityRun<InitialDataImportWorkDefinition, InitialDataImportActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<InitialDataImportWorkDefinition, InitialDataImportActivityHandler> context,
            @NotNull OperationResult result) {
        return new InitialDataImportActivityRun(context);
    }

    private static final class InitialDataImportActivityRun
            extends LocalActivityRun<InitialDataImportWorkDefinition, InitialDataImportActivityHandler, AbstractActivityWorkStateType> {

        private InitialDataImportActivityRun(@NotNull ActivityRunInstantiationContext<InitialDataImportWorkDefinition,
                InitialDataImportActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            InitialDataImportActivityHandler handler = getActivityHandler();

            handler.securityEnforcer.authorizeAll(getRunningTask(), result);
            handler.initialDataImport.init(true);

            // TODO consider if we need to go clusterwide here
            handler.cacheDispatcher.dispatchInvalidation(null, null, true, null);

            handler.modelService.shutdown();
            handler.modelService.postInit(result);

            LOGGER.info("Repository factory reset finished");
            return standardRunResult(result.getComputeStatus());
        }
    }

    public static class InitialDataImportWorkDefinition extends AbstractWorkDefinition {

        private InitialDataImportWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
        }

        @Override
        public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation(
                @Nullable AbstractActivityWorkStateType state) {
            return AffectedObjectsInformation.ObjectSet.notSupported();
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
        }
    }
}
