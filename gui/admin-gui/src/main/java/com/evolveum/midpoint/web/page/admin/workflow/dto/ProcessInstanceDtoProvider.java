/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.api.WorkflowService;
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
    public Iterator<? extends ProcessInstanceDto> iterator(long first, long count) {
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
            List<ProcessInstance> items = wfm.listProcessInstancesRelatedToUser(currentUser(), requestedBy,
                    requestedFor, finished, WebMiscUtil.safeLongToInteger(first), WebMiscUtil.safeLongToInteger(count),
                    result);

            for (ProcessInstance item : items) {
                try {
                    getAvailableData().add(new ProcessInstanceDto(item));
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
