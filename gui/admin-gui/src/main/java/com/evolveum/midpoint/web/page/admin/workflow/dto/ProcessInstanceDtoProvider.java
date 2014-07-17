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

import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;

import org.apache.wicket.Component;

import java.util.Iterator;
import java.util.List;

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

        OperationResult result = new OperationResult(OPERATION_LIST_ITEMS);

        try {
//            SortParam sortParam = getSort();
//            OrderDirectionType order;
//            if (sortParam.isAscending()) {
//                order = OrderDirectionType.ASCENDING;
//            } else {
//                order = OrderDirectionType.DESCENDING;
//            }

            WorkflowService wfm = getWorkflowService();
            List<WfProcessInstanceType> items = wfm.listProcessInstancesRelatedToUser(currentUser(), requestedBy,
                    requestedFor, finished, WebMiscUtil.safeLongToInteger(first), WebMiscUtil.safeLongToInteger(count),
                    result);

            for (WfProcessInstanceType item : items) {
                try {
                    getAvailableData().add(new ProcessInstanceDto(item, null));
                } catch (Exception e) {
                    LoggingUtils.logException(LOGGER, "Unhandled exception when listing process instance ", e, item);
                    result.recordPartialError("Couldn't list process instance.", e);
                }
            }

        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when listing process instances", ex);
            result.recordFatalError("Couldn't list process instances.", ex);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_ITEMS);
        try {
            WorkflowService workflowService = getWorkflowService();
            count = workflowService.countProcessInstancesRelatedToUser(currentUser(), requestedBy, requestedFor, finished, result);
        } catch (Exception ex) {
            String msg = "Couldn't list process instances";
            LoggingUtils.logException(LOGGER, msg, ex);
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
