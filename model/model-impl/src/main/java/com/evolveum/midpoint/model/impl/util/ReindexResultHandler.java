/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.ArrayList;

/**
 * @author mederly
 */
public class ReindexResultHandler extends AbstractSearchIterativeResultHandler<ObjectType> {

    @SuppressWarnings("unused")
    static final Trace LOGGER = TraceManager.getTrace(ReindexResultHandler.class);

    private static final String CLASS_DOT = ReindexResultHandler.class.getName() + ".";

    private RepositoryService repositoryService;

    public ReindexResultHandler(Task coordinatorTask, String taskOperationPrefix, String processShortName,
                                String contextDesc, TaskManager taskManager,
                                RepositoryService repositoryService) {
        super(coordinatorTask, taskOperationPrefix, processShortName, contextDesc, taskManager);
        this.repositoryService = repositoryService;
        setStopOnError(false);
    }

    @Override
    protected boolean handleObject(PrismObject<ObjectType> object, Task workerTask, OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createMinorSubresult(CLASS_DOT + "handleObject");
        repositoryService.modifyObject(object.asObjectable().getClass(), object.getOid(), new ArrayList<>(),
                RepoModifyOptions.createExecuteIfNoChanges(), result);
        result.computeStatusIfUnknown();
        return true;
    }


    @Override
    public void completeProcessing(Task task, OperationResult result) {
        super.completeProcessing(task, result);
    }

}
