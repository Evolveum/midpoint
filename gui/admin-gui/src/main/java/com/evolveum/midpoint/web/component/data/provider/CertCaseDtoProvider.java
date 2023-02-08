/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.provider;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCaseOrWorkItemDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.SearchingUtils;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.*;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;

/**
 * @author lazyman
 */
public class CertCaseDtoProvider extends BaseSortableDataProvider<CertCaseOrWorkItemDto> {
    private static final long serialVersionUID = 1L;

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

        Task task = getPageBase().createSimpleTask(OPERATION_SEARCH_OBJECTS);
        OperationResult result = task.getResult();
        try {
            ObjectPaging paging = createPaging(first, count);
            Collection<SelectorOptions<GetOperationOptions>> resolveNames = createCollection(createResolveNames());
            List<AccessCertificationCaseType> caseList = searchCases(campaignOid, paging, resolveNames, getPageBase().getPrismContext(), task, result);
            for (AccessCertificationCaseType acase : caseList) {
                getAvailableData().add(new CertCaseDto(acase, getPageBase(), task, result));
            }
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("CertCaseDtoProvider.message.internalIterator.fatalError").getString(), ex);
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
        getPageBase().showResult(result);
        throw new RestartResponseException(PageError.class);
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_COUNT_OBJECTS);
            count = countCases(campaignOid, null, getPageBase().getPrismContext(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("CertCaseDtoProvider.message.internalSize.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPageBase().showResult(result, false);
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

    private List<AccessCertificationCaseType> searchCases(String campaignOid, ObjectPaging paging,
            Collection<SelectorOptions<GetOperationOptions>> options, PrismContext prismContext,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, CommunicationException {
        InOidFilter inOidFilter = prismContext.queryFactory().createOwnerHasOidIn(campaignOid);
        ObjectQuery query = createFinalQuery(inOidFilter, prismContext);
        query.setPaging(paging);
        return getModelService().searchContainers(AccessCertificationCaseType.class, query, options, task, result);
    }

    private int countCases(String campaignOid, Collection<SelectorOptions<GetOperationOptions>> options,
            PrismContext prismContext, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, CommunicationException {
        InOidFilter inOidFilter = prismContext.queryFactory().createOwnerHasOidIn(campaignOid);
        ObjectQuery query = createFinalQuery(inOidFilter, prismContext);
        return getModelService().countContainers(AccessCertificationCaseType.class, query, options, task, result);
    }

    @NotNull
    private ObjectQuery createFinalQuery(InOidFilter inOidFilter, PrismContext prismContext) {
        ObjectQuery query = getQuery();
        if (query != null) {
            query = query.clone();
            if (query.getFilter() == null) {
                query.setFilter(inOidFilter);
            } else {
                query.setFilter(prismContext.queryFactory().createAnd(query.getFilter(), inOidFilter));
            }
        } else {
            query = getPrismContext().queryFactory().createQuery(inOidFilter);
        }
        return query;
    }

    @NotNull
    @Override
    protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
        return SearchingUtils.createObjectOrderings(sortParam, false, getPrismContext());
    }

}
