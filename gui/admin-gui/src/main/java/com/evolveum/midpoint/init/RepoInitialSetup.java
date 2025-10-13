/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;

public class RepoInitialSetup {

    private static final Trace LOGGER = TraceManager.getTrace(RepoInitialSetup.class);

    @NotNull private RepositoryService repositoryService;

    public RepoInitialSetup(@NotNull RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public void init() {
        LOGGER.info("Repository post initialization.");

        OperationResult result = new OperationResult(RepoInitialSetup.class.getName() + ".init");
        try {
            repositoryService.postInit(result);
            LOGGER.info("Repository post initialization finished successfully.");
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Repository post initialization failed", ex);
            result.recordFatalError("RepoInitialSetup.message.init.fatalError", ex);
        } finally {
            result.computeStatus("RepoInitialSetup.message.init.fatalError");
        }
    }
}
