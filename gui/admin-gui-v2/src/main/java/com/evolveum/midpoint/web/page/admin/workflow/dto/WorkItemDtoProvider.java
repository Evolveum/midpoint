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

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.wf.WorkItem;
import com.evolveum.midpoint.wf.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.springframework.security.core.context.SecurityContextHolder;

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

    public static String currentUser() {            // TODO move to SecurityUtils
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            return "Unknown";
        }

        if (principal instanceof PrincipalUser) {
            PrincipalUser user = (PrincipalUser) principal;
            return user.getName();
        }

        return principal.toString();
    }

    public WorkItemDtoProvider(PageBase page, boolean assigned) {
        super(page);
        this.assigned = assigned;
    }

    @Override
    public Iterator<? extends WorkItemDto> iterator(int first, int count) {
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

            WorkflowManager wfm = getWorkflowManager();
            List<WorkItem> items = assigned ?
                    wfm.listWorkItemsAssignedToUser(currentUser(), first, count, result) :
                    wfm.listWorkItemsAssignableToUser(currentUser(), first, count, result);

            for (WorkItem item : items) {
                getAvailableData().add(new WorkItemDto(item));
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
    public int size() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_ITEMS);
        try {
            WorkflowManager wfm = getWorkflowManager();
            count = assigned ?
                    wfm.countWorkItemsAssignedToUser(currentUser(), result) :
                    wfm.countWorkItemsAssignableToUser(currentUser(), result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count work items assigned to user.", ex);
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
