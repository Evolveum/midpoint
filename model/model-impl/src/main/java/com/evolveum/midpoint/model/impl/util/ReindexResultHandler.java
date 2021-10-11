/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.ArrayList;

/**
 *
 */
public class ReindexResultHandler extends AbstractSearchIterativeResultHandler<ObjectType> {

    @SuppressWarnings("unused")
    static final Trace LOGGER = TraceManager.getTrace(ReindexResultHandler.class);

    private static final String CLASS_DOT = ReindexResultHandler.class.getName() + ".";
    private static final String OP_HANDLE_OBJECT = CLASS_DOT + "handleObject";

    private RepositoryService repositoryService;

    ReindexResultHandler(RunningTask coordinatorTask, String taskOperationPrefix, String processShortName,
            String contextDesc, TaskManager taskManager,
            RepositoryService repositoryService) {
        super(coordinatorTask, taskOperationPrefix, processShortName, contextDesc, taskManager);
        this.repositoryService = repositoryService;
        setStopOnError(false);
    }

    @Override
    protected boolean handleObject(PrismObject<ObjectType> object, RunningTask workerTask, OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createMinorSubresult(OP_HANDLE_OBJECT);
        repositoryService.modifyObject(object.asObjectable().getClass(), object.getOid(), new ArrayList<>(),
                RepoModifyOptions.createExecuteIfNoChanges(), result);
        result.computeStatusIfUnknown();
        return true;
    }
}
