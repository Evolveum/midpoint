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

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.*;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;

/**
 * @author lazyman
 * @author mederly
 */
public class CertCaseDtoProvider extends BaseSortableDataProvider<CertCaseOrWorkItemDto> {

    private static final Trace LOGGER = TraceManager.getTrace(CertCaseDtoProvider.class);
    private static final String DOT_CLASS = CertCaseDtoProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private String campaignOid;
    // case query is stored in super.query

    public CertCaseDtoProvider(Component component) {
        super(component, false);        // TODO make this cacheable
    }

    @Override
    public Iterator<CertCaseOrWorkItemDto> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", first, count);
        getAvailableData().clear();

        Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);
        OperationResult result = task.getResult();
        try {
            ObjectPaging paging = createPaging(first, count);
            Collection<SelectorOptions<GetOperationOptions>> resolveNames = createCollection(createResolveNames());
            List<AccessCertificationCaseType> caseList = searchCases(campaignOid, paging, resolveNames, task, result);
            for (AccessCertificationCaseType _case : caseList) {
                getAvailableData().add(new CertCaseDto(_case, getPage(), task, result));
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

    private void handleNotSuccessOrHandledErrorInIterator(OperationResult result){
        getPage().showResult(result);
        throw new RestartResponseException(PageError.class);
    }

    // TODO replace searchCases with countCases (when it will be available)
    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_OBJECTS);
            List<AccessCertificationCaseType> caseList = searchCases(campaignOid, null, null, task, result);
            count = caseList.size();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count objects.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPage().showResult(result, false);
            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::internalSize()");
        return count;
    }

    @SuppressWarnings("unused")
    public String getCampaignOid() {
        return campaignOid;
    }

    public void setCampaignOid(String campaignOid) {
        this.campaignOid = campaignOid;
    }

    private List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectPaging paging, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException {
        final InOidFilter inOidFilter = InOidFilter.createOwnerHasOidIn(campaignOid);

        ObjectQuery query = getQuery();
        if (query != null) {
            query = query.clone();
            if (query.getFilter() == null) {
                query.setFilter(inOidFilter);
            } else {
                query.setFilter(AndFilter.createAnd(query.getFilter(), inOidFilter));
            }
        } else {
            query = new ObjectQuery();
            query.setFilter(inOidFilter);
        }

        query.setPaging(paging);
        SearchingUtils.hackPaging(query);

        return getModel().searchContainers(AccessCertificationCaseType.class, query, options, task, result);
    }


}
