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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.wf.ProcessInstance;
import com.evolveum.midpoint.wf.WfDataAccessor;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
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

    public static String currentUser() {
        PrincipalUser principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return "Unknown";
        }

        return principal.getOid();
    }

    public ProcessInstanceDtoProvider(PageBase page, boolean requestedBy, boolean finished) {
        super(page);
        LOGGER.trace("requestedBy = " + requestedBy + ", finished = " + finished);
        this.requestedBy = requestedBy;
        this.finished = finished;
    }

    @Override
    public Iterator<? extends ProcessInstanceDto> iterator(int first, int count) {
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

            WfDataAccessor wfm = getWorkflowDataAccessor();
            List<ProcessInstance> items = wfm.listProcessInstances(requestedBy, finished, currentUser(), first, count, result);

            for (ProcessInstance item : items) {
                getAvailableData().add(new ProcessInstanceDto(item));
            }

        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Unhandled exception when listing process instances", ex);
            result.recordFatalError("Couldn't list process instances.", ex);
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
            WfDataAccessor wfDataAccessor = getWorkflowDataAccessor();
            count = wfDataAccessor.countProcessInstances(requestedBy, finished, currentUser(), result);
        } catch (Exception ex) {
            String msg = "Couldn't list process instances requested " + (requestedBy ? "by":"for") + " a user.";
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
