/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.cases.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import com.evolveum.midpoint.model.api.ModelService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.createResolveNames;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;

/**
 * @author bpowers
 */
public class CaseWorkItemDtoProvider extends BaseSortableDataProvider<CaseWorkItemDto> {

    private static final Trace LOGGER = TraceManager.getTrace(CaseWorkItemDtoProvider.class);
    private static final String DOT_CLASS = CaseWorkItemDtoProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private boolean notDecidedOnly;

    public CaseWorkItemDtoProvider(Component component) {
        super(component, false);        // TODO make this cache-able
    }

    @Override
    public Iterator<CaseWorkItemDto> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", first, count);
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
            LOGGER.trace("ITERATOR PAGING: {}.",paging);
            Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);

            ObjectQuery caseQuery = getQuery();
            caseQuery = caseQuery != null ? caseQuery.clone() : new ObjectQuery();
            caseQuery.setPaging(paging);

            Collection<SelectorOptions<GetOperationOptions>> resolveNames = createCollection(createResolveNames());
            ModelService modelService = getPage().getModelService();
            List<CaseWorkItemType> workItems = modelService.searchContainers(CaseWorkItemType.class, caseQuery, resolveNames, task, result);
            for (CaseWorkItemType workItem : workItems) {
                getAvailableData().add(new CaseWorkItemDto(workItem));
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list decisions.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list decisions", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            handleNotSuccessOrHandledErrorInIterator(result);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    private void handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
        getPage().showResult(result);
        throw new RestartResponseException(PageError.class);
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_OBJECTS);
            ObjectQuery query = getQuery().clone();

            ModelService modelService = getPage().getModelService();
            Collection<SelectorOptions<GetOperationOptions>> resolveNames = createCollection(createResolveNames());
            count = modelService.countContainers(CaseWorkItemType.class, query, resolveNames, task, result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count objects.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPage().showResult(result);
            throw new RestartResponseException(PageError.class);
        }
        LOGGER.trace("end::internalSize()");
        return count;
    }

    @SuppressWarnings("unused")
    public boolean isNotDecidedOnly() {
        return notDecidedOnly;
    }

    public void setNotDecidedOnly(boolean notDecidedOnly) {
        this.notDecidedOnly = notDecidedOnly;
    }

    @NotNull
    @Override
    protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
        return SearchingUtils.createObjectOrderings(sortParam);
    }

}
