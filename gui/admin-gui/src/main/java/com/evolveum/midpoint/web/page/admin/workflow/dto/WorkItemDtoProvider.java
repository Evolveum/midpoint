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
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.wf.api.WorkItem;
import com.evolveum.midpoint.wf.api.WorkflowException;
import org.apache.wicket.Component;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class WorkItemDtoProvider extends BaseSortableDataProvider<WorkItemDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemDtoProvider.class);
    private static final String DOT_CLASS = WorkItemDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_ITEMS = DOT_CLASS + "listItems";
    private static final String OPERATION_COUNT_ITEMS = DOT_CLASS + "countItems";

    boolean assigned;

    public static String currentUser() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return "Unknown";
        }

        return principal.getOid();
    }

    public WorkItemDtoProvider(Component component, boolean assigned) {
        super(component);
        this.assigned = assigned;
    }

    @Override
    public Iterator<? extends WorkItemDto> internalIterator(long first, long count) {
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
            List<WorkItem> items = wfm.listWorkItemsRelatedToUser(currentUser(), assigned,
                    WebMiscUtil.safeLongToInteger(first), WebMiscUtil.safeLongToInteger(count), result);

            for (WorkItem item : items) {
                try {
                    getAvailableData().add(new WorkItemDto(item));
                } catch (Exception e) {
                    LoggingUtils.logException(LOGGER, "Unhandled exception when listing work item {}", e, item);
                    result.recordFatalError("Couldn't list work item.", e);
                }
            }

        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when listing work items", ex);
            result.recordFatalError("Couldn't list work items.", ex);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }

        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_ITEMS);
        try {
            WorkflowService workflowService = getWorkflowService();
            count = workflowService.countWorkItemsRelatedToUser(currentUser(), assigned, result);
        } catch (WorkflowException ex) {
            result.recordFatalError("Couldn't count work items assigned/assignable to user.", ex);
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
