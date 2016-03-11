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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
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
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;

import java.util.*;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.*;
import static com.evolveum.midpoint.prism.query.OrderDirection.DESCENDING;
import static com.evolveum.midpoint.schema.GetOperationOptions.createResolve;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.*;

/**
 * @author lazyman
 */
public class WorkItemDtoNewProvider extends BaseSortableDataProvider<WorkItemNewDto> {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemDtoNewProvider.class);
    private static final String DOT_CLASS = WorkItemDtoNewProvider.class.getName() + ".";
    private static final String OPERATION_LIST_ITEMS = DOT_CLASS + "listItems";
    private static final String OPERATION_COUNT_ITEMS = DOT_CLASS + "countItems";

    boolean assigned;

    public String currentUser() {
        MidPointPrincipal principal = SecurityUtils.getPrincipalUser();
        if (principal == null) {
            return "Unknown";
        }

        return principal.getOid();
    }

    public WorkItemDtoNewProvider(Component component, boolean assigned) {
        super(component);
        this.assigned = assigned;
    }

    @Override
    public Iterator<? extends WorkItemNewDto> internalIterator(long first, long count) {
        getAvailableData().clear();

        Task task = getTaskManager().createTaskInstance();
        OperationResult result = new OperationResult(OPERATION_LIST_ITEMS);

        try {
            ObjectQuery query = createQuery(first, count);
            Collection<SelectorOptions<GetOperationOptions>> options =
                    Arrays.asList(
                            SelectorOptions.create(new ItemPath(F_ASSIGNEE_REF), createResolve()));
            List<WorkItemType> items = getModel().searchContainers(WorkItemType.class, query, options, task, result);

            for (WorkItemType item : items) {
                try {
                    getAvailableData().add(new WorkItemNewDto(item));
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing work item {}", e, item);
                    result.recordFatalError("Couldn't list work item.", e);
                }
            }

        } catch (SchemaException|ObjectNotFoundException|SecurityViolationException|ConfigurationException|RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception when listing work items", ex);
            result.recordFatalError("Couldn't list work items.", ex);
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }

        return getAvailableData().iterator();
    }

    private ObjectQuery createQuery(long first, long count) throws SchemaException {
        ObjectQuery query = createQuery();
        query.setPaging(ObjectPaging.createPaging(safeLongToInteger(first), safeLongToInteger(count), F_WORK_ITEM_CREATED_TIMESTAMP, DESCENDING));
        return query;
    }

    private ObjectQuery createQuery() throws SchemaException {
        if (assigned) {
            return QueryBuilder.queryFor(WorkItemType.class, getPrismContext())
                    .item(WorkItemType.F_ASSIGNEE_REF).ref(currentUser())
                    .build();
        } else {
            throw new UnsupportedOperationException("search by more than one ref is not supported");
        }
    }

    @Override
    protected int internalSize() {
        int count = 0;
        Task task = getTaskManager().createTaskInstance();
        OperationResult result = new OperationResult(OPERATION_COUNT_ITEMS);
        try {
            ObjectQuery query = createQuery();
            count = getModel().countContainers(WorkItemType.class, query, null, task, result);
        } catch (SchemaException|RuntimeException e) {
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

    // TODO - fix this temporary implementation (perhaps by storing 'groups' in user context on logon)
    public List<String> getGroupsForUser(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<String> retval = new ArrayList<>();
        UserType userType = getRepositoryService().getObject(UserType.class, oid, null, result).asObjectable();
        for (AssignmentType assignmentType : userType.getAssignment()) {
            ObjectReferenceType ref = assignmentType.getTargetRef();
            if (ref != null) {
                String groupName = objectReferenceToGroupName(ref);
                if (groupName != null) {        // if the reference represents a group name (i.e. it is not e.g. an account ref)
                    retval.add(groupName);
                }
            }
        }
        return retval;
    }

    private String objectReferenceToGroupName(ObjectReferenceType ref) {
        if (RoleType.COMPLEX_TYPE.equals(ref.getType())) {
            return "role:" + ref.getOid();
        } else if (OrgType.COMPLEX_TYPE.equals(ref.getType())) {
            return "org:" + ref.getOid();
        } else {
            return null;
        }
    }


}
