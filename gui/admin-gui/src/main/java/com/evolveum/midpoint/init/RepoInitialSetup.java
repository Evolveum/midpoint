/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
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
            result.recordFatalError("Repository post initialization failed.", ex);
        } finally {
            result.computeStatus("Repository post initialization failed.");
        }
    }
}
