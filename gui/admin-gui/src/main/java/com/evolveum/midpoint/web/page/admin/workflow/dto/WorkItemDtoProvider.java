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
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;

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
            List<WorkItemType> items = wfm.listWorkItemsRelatedToUser(currentUser(), assigned,
                    WebMiscUtil.safeLongToInteger(first), WebMiscUtil.safeLongToInteger(count), result);

            for (WorkItemType item : items) {
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
        WorkflowService workflowService = getWorkflowService();
        try {
            count = workflowService.countWorkItemsRelatedToUser(currentUser(), assigned, result);
        } catch (SchemaException|ObjectNotFoundException e) {
            throw new SystemException("Couldn't count work items: " + e.getMessage(), e);
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
