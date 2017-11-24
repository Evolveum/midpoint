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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemsPageType;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.isAuthorized;
import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.safeLongToInteger;
import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.prism.query.OrderDirection.DESCENDING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_CREATE_TIMESTAMP;

/**
 * @author lazyman
 */
public class WorkItemDtoProvider extends BaseSortableDataProvider<WorkItemDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemDtoProvider.class);

    private static final String DOT_CLASS = WorkItemDtoProvider.class.getName() + ".";

    private static final String OPERATION_LIST_ITEMS = DOT_CLASS + "listItems";
    private static final String OPERATION_COUNT_ITEMS = DOT_CLASS + "countItems";

    private WorkItemsPageType workItemsPageType;
    private IModel<PrismObject<UserType>> donorModel;

    public WorkItemDtoProvider(Component component, WorkItemsPageType workItemsPageType,
                               IModel<PrismObject<UserType>> donorModel) {
        super(component);
        this.workItemsPageType = workItemsPageType;
        this.donorModel = donorModel;
    }

    private String currentUserOid() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return "Unknown";
        }

        return principal.getOid();
    }

    @Override
    public Iterator<? extends WorkItemDto> iterator(long first, long count) {
        assumePowerOfAttorneyIfRequested();

        try {
            return super.iterator(first, count);
        } finally {
            dropPowerOfAttorneyIfRequested();
        }
    }

    @Override
    public long size() {
        assumePowerOfAttorneyIfRequested();

        try {
            return super.size();
        } finally {
            dropPowerOfAttorneyIfRequested();
        }
    }

    private void assumePowerOfAttorneyIfRequested() {
        if (workItemsPageType == WorkItemsPageType.ATTORNEY) {
            WebModelServiceUtils.assumePowerOfAttorney(donorModel.getObject(), getModelInteractionService(), getTaskManager(), null);
        }
    }

    private void dropPowerOfAttorneyIfRequested() {
        if (workItemsPageType == WorkItemsPageType.ATTORNEY) {
            WebModelServiceUtils.dropPowerOfAttorney(getModelInteractionService(), getTaskManager(), null);
        }
    }

    @Override
    public Iterator<? extends WorkItemDto> internalIterator(long first, long count) {
        getAvailableData().clear();

        Task task = getTaskManager().createTaskInstance();
        OperationResult result = new OperationResult(OPERATION_LIST_ITEMS);

        try {
            ObjectQuery query = createQuery(first, count, result);
            Collection<SelectorOptions<GetOperationOptions>> options =
                    GetOperationOptions.resolveItemsNamed(
                            new ItemPath(F_ASSIGNEE_REF),
                            new ItemPath(T_PARENT, WfContextType.F_OBJECT_REF),
                            new ItemPath(T_PARENT, WfContextType.F_TARGET_REF));
            List<WorkItemType> items = getModel().searchContainers(WorkItemType.class, query, options, task, result);

            for (WorkItemType item : items) {
                try {
                    getAvailableData().add(new WorkItemDto(item));
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing work item {}", e, item);
                    result.recordFatalError("Couldn't list work item.", e);
                }
            }

        } catch (CommonException | RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing work items", ex);
            result.recordFatalError("Couldn't list work items.", ex);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            handleError(result);
        }

        return getAvailableData().iterator();
    }

    private void handleError(OperationResult result) {
        getPage().showResult(result);
        throw new RestartResponseException(PageError.class);
    }

    private ObjectQuery createQuery(long first, long count, OperationResult result) throws SchemaException {
        ObjectQuery query = createQuery(result);
        query.setPaging(ObjectPaging.createPaging(safeLongToInteger(first), safeLongToInteger(count), F_CREATE_TIMESTAMP, DESCENDING));
        return query;
    }

    private ObjectQuery createQuery(OperationResult result) throws SchemaException {
        boolean authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
        S_FilterEntryOrEmpty q = QueryBuilder.queryFor(WorkItemType.class, getPrismContext());
        if (WorkItemsPageType.ALL.equals(workItemsPageType) && authorizedToSeeAll) {
            return q.build();
        } else if (WorkItemsPageType.CLAIMABLE.equals(workItemsPageType)) {
            return QueryUtils.filterForGroups(q, currentUserOid(), getRepositoryService(), result).build();
        } else {
            // not authorized to see all => sees only allocated to him (not quite what is expected, but sufficient for the time being)
            return QueryUtils.filterForAssignees(q, SecurityUtils.getPrincipalUser(),
                    OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS).build();
        }
    }

    @Override
    protected int internalSize() {
        int count;
        Task task = getTaskManager().createTaskInstance();
        OperationResult result = new OperationResult(OPERATION_COUNT_ITEMS);
        try {
            ObjectQuery query = createQuery(result);
            count = getModel().countContainers(WorkItemType.class, query, null, task, result);
        } catch (SchemaException | SecurityViolationException | ObjectNotFoundException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            throw new SystemException("Couldn't count work items: " + e.getMessage(), e);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            handleError(result);
        }

        return count;
    }
}
