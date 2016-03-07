/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;

import org.apache.wicket.Component;

import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_OBJECT_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESS_INSTANCE_ID;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_REQUESTER_REF;

/**
 * @author lazyman
 * @author mederly
 */
public class ProcessInstanceDtoProvider extends BaseSortableDataProvider<ProcessInstanceDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProcessInstanceDtoProvider.class);
    private static final String DOT_CLASS = ProcessInstanceDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_ITEMS = DOT_CLASS + "listItems";
    private static final String OPERATION_COUNT_ITEMS = DOT_CLASS + "countItems";

    /*
     * requestedBy:
     * - true = we are interested in process instances REQUESTED BY a user (e.g. the user has requested granting a role to another user)
     * - false = we are interested in process instances REQUESTED FOR a user (e.g. the user is to be granted a role)
     */

    boolean requestedBy;
    private boolean finished;
    private boolean requestedFor;

    public static String currentUser() {
    	MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return "Unknown";
        }

        return principal.getOid();
    }

    public ProcessInstanceDtoProvider(Component component, boolean requestedBy, boolean requestedFor, boolean finished) {
        super(component);
        LOGGER.trace("requestedBy = " + requestedBy + ", requestedFor = " + requestedFor + ", finished = " + finished);
        this.requestedBy = requestedBy;
        this.requestedFor = requestedFor;
        this.finished = finished;
    }

    @Override
    public Iterator<? extends ProcessInstanceDto> internalIterator(long first, long count) {
        getAvailableData().clear();

        Task opTask = getTaskManager().createTaskInstance(OPERATION_LIST_ITEMS);
        OperationResult result = opTask.getResult();

        try {
//            SortParam sortParam = getSort();
//            OrderDirectionType order;
//            if (sortParam.isAscending()) {
//                order = OrderDirectionType.ASCENDING;
//            } else {
//                order = OrderDirectionType.DESCENDING;
//            }

            ObjectQuery query = getObjectQuery();
            query.setPaging(ObjectPaging.createPaging(safeLongToInteger(first), safeLongToInteger(count)));

            List<PrismObject<TaskType>> tasks = getModel().searchObjects(TaskType.class, query, null, opTask, result);
            for (PrismObject<TaskType> task : tasks) {
                try {
                    getAvailableData().add(new ProcessInstanceDto(task.asObjectable()));
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing workflow task {}", e, task);
                    result.recordPartialError("Couldn't list process instance.", e);
                }
            }

        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing wf-related tasks", ex);
            result.recordFatalError("Couldn't list wf-related tasks.", ex);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return getAvailableData().iterator();
    }

    private ObjectQuery getObjectQuery() throws SchemaException {
        String currentUserOid = currentUser();
        S_FilterEntry q = QueryBuilder.queryFor(TaskType.class, getPrismContext());
        if (requestedBy) {
            q = q.item(F_WORKFLOW_CONTEXT, F_REQUESTER_REF).ref(currentUserOid).and();
        }
        if (requestedFor) {
            q = q.item(F_OBJECT_REF).ref(currentUserOid).and();
        }
        return q
                .not().item(F_WORKFLOW_CONTEXT, F_PROCESS_INSTANCE_ID).isNull()
                .build();
    }

    @Override
    protected int internalSize() {
        int count = 0;
        Task opTask = getTaskManager().createTaskInstance(OPERATION_COUNT_ITEMS);
        OperationResult result = opTask.getResult();
        try {
            ObjectQuery query = getObjectQuery();
            count = getModel().countObjects(TaskType.class, query, null, opTask, result);
        } catch (Exception ex) {
            String msg = "Couldn't count process instances";
            LoggingUtils.logUnexpectedException(LOGGER, msg, ex);
            result.recordFatalError(msg, ex);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return count;
    }
}
