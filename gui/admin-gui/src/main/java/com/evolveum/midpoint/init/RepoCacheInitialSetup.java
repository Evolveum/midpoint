/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;

public class RepoCacheInitialSetup {

    private static final Trace LOGGER = TraceManager.getTrace(RepoCacheInitialSetup.class);

    @NotNull private RepositoryService cacheRepositoryService;

    public RepoCacheInitialSetup(@NotNull RepositoryService cacheRepositoryService) {
        this.cacheRepositoryService = cacheRepositoryService;
    }

    public void init() {
        LOGGER.info("Repository cache post initialization.");

        OperationResult result = new OperationResult(RepoCacheInitialSetup.class.getName() + ".init");
        try {
            cacheRepositoryService.postInit(result);
            LOGGER.info("Repository cache post initialization finished successfully.");
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Repository cache post initialization failed", ex);
            result.recordFatalError("RepoCacheInitialSetup.message.init.fatalError", ex);
        } finally {
            result.computeStatus("RepoCacheInitialSetup.message.init.fatalError");
        }
    }
}
