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

package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseWorkItemDto;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseWorkItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.certification.dto.SearchingUtils;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.isAuthorized;

/**
 * @author bpowers
 */
public abstract class PageCaseWorkItems extends PageAdminCaseWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItems.class);

    private static final String DOT_CLASS = PageCaseWorkItems.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = DOT_CLASS + "recordActionSelected";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CASE_WORK_ITEMS_TABLE = "caseWorkItemsTable";

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_TABLE_HEADER = "tableHeader";

    private IModel<Boolean> showNotDecidedOnlyModel = new Model<>(false);

    private boolean all;

    public PageCaseWorkItems(boolean all) {
        this.all = all;
        initLayout();
    }

    //region Data
    private CaseWorkItemDtoProvider createProvider() {
        CaseWorkItemDtoProvider provider = new CaseWorkItemDtoProvider(PageCaseWorkItems.this);
        try {
            provider.setQuery(createQuery());
        } catch (SchemaException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't create case work item query", e);
        }
        provider.setSort(SearchingUtils.CURRENT_REVIEW_DEADLINE, SortOrder.ASCENDING);        // default sorting
        return provider;
    }

    private ObjectQuery createQuery() throws SchemaException {
        boolean authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
        S_FilterEntryOrEmpty q = QueryBuilder.queryFor(CaseWorkItemType.class, getPrismContext());
        if (all && authorizedToSeeAll) {
            return q.build();
        } else {
            // not authorized to see all => sees only allocated to him (not quite what is expected, but sufficient for the time being)
            return QueryUtils.filterForAssignees(q, SecurityUtils.getPrincipalUser(),
                    OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS).build();
        }
    }

    private String getCurrentUserOid() {
        try {
            return getSecurityEnforcer().getPrincipal().getOid();
        } catch (SecurityViolationException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't get currently logged user OID", e);
        }
    }
    //endregion

    //region Layout
    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);
        CaseWorkItemDtoProvider provider = createProvider();
        int itemsPerPage = (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL);
        BoxedTablePanel<CaseWorkItemDto> table = new BoxedTablePanel<CaseWorkItemDto>(ID_CASE_WORK_ITEMS_TABLE, provider, initColumns(),
                UserProfileStorage.TableId.PAGE_CERT_DECISIONS_PANEL, itemsPerPage) {

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageCaseWorkItems.this, showNotDecidedOnlyModel);
            }
        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        table.setItemsPerPage(itemsPerPage);        // really don't know why this is necessary, as e.g. in PageRoles the size setting works without it
        mainForm.add(table);

        // adding this on outer feedback panel prevents displaying the error messages
        //addVisibleOnWarningBehavior(getMainFeedbackPanel());
        //addVisibleOnWarningBehavior(getTempFeedbackPanel());
    }

    private List<IColumn<CaseWorkItemDto, String>> initColumns() {
        List<IColumn<CaseWorkItemDto, String>> columns = new ArrayList<>();

        IColumn<CaseWorkItemDto, String> column;

        column = new CheckBoxHeaderColumn<>();
        columns.add(column);

//        column = new PropertyColumn(createStringResource("PageCaseWorkItems.table.name"), CaseWorkItemDto.F_NAME);
        columns.add(createNameColumn());

        column = new PropertyColumn(createStringResource("PageCaseWorkItems.table.objectName"), CaseWorkItemDto.F_OBJECT_NAME);
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageCaseWorkItems.table.description"), CaseWorkItemDto.F_DESCRIPTION);
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageCaseWorkItems.table.actors"), CaseWorkItemDto.F_ASSIGNEES);
        columns.add(column);

        return columns;
    }

    private Table getCaseWorkItemsTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_CASE_WORK_ITEMS_TABLE));
    }

    @NotNull
    private AbstractColumn<CaseWorkItemDto, String> createNameColumn() {
        return new LinkColumn<CaseWorkItemDto>(createStringResource("PageCaseWorkItems.table.name"), CaseWorkItemDto.F_NAME,
                CaseWorkItemDto.F_NAME) {
            @Override
            public void onClick(AjaxRequestTarget target, IModel<CaseWorkItemDto> rowModel) {
                PageParameters parameters = new PageParameters();
//                parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getWorkItemId());
                parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getCase().getOid());
                navigateToNext(PageCase.class, parameters);
            }
        };
    }
    //endregion

    //region Actions

    private void caseDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
        LOGGER.trace("caseDetailsPerformed()");
        navigateToNext(PageCase.class, pageParameters);
    }

    private void recordActionOnSelected(AccessCertificationResponseType response, AjaxRequestTarget target) {
//        List<CaseWorkItemDto> workItemDtoList = WebComponentUtil.getSelectedData(getCaseWorkItemsTable());
//        if (workItemDtoList.isEmpty()) {
//            warn(getString("PageCaseWorkItems.message.noItemSelected"));
//            target.add(getFeedbackPanel());
//            return;
//        }
//
//        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION_SELECTED);
//        Task task = createSimpleTask(OPERATION_RECORD_ACTION_SELECTED);
//        for (CaseWorkItemDto workItemDto : workItemDtoList) {
//            OperationResult resultOne = result.createSubresult(OPERATION_RECORD_ACTION);
//            try {
//                getCertificationService().recordDecision(
//                        workItemDto.getCampaignRef().getOid(),
//                        workItemDto.getCaseId(), workItemDto.getWorkItemId(),
//                        response, workItemDto.getComment(), task, resultOne);
//            } catch (Exception ex) {
//                resultOne.recordFatalError(ex);
//            } finally {
//                resultOne.computeStatusIfUnknown();
//            }
//        }
//        result.computeStatus();
//
//        if (!result.isSuccess()) {
//            showResult(result);
//        }
//        target.add(getFeedbackPanel());
//        target.add((Component) getCaseWorkItemsTable());
    }

    // if response is null this means keep the current one in workItemDto
    private void recordActionPerformed(AjaxRequestTarget target, CaseWorkItemDto workItemDto, AccessCertificationResponseType response) {
//        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION);
//        try {
//            Task task = createSimpleTask(OPERATION_RECORD_ACTION);
//            if (response == null) {
//                response = workItemDto.getResponse();
//            }
//            // TODO work item ID
//            getCertificationService().recordDecision(
//                    workItemDto.getCampaignRef().getOid(),
//                    workItemDto.getCaseId(), workItemDto.getWorkItemId(),
//                    response, workItemDto.getComment(), task, result);
//        } catch (Exception ex) {
//            result.recordFatalError(ex);
//        } finally {
//            result.computeStatusIfUnknown();
//        }
//
//        if (!result.isSuccess()) {
//            showResult(result);
//        }
//        resetCertWorkItemCountModel();
//        target.add(this);
    }

    private void searchFilterPerformed(AjaxRequestTarget target) {
        ObjectQuery query;
        try {
            query = createQuery();
        } catch (SchemaException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't create case work item query", e);
        }

        Table panel = getCaseWorkItemsTable();
        DataTable table = panel.getDataTable();
        CaseWorkItemDtoProvider provider = (CaseWorkItemDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        provider.setNotDecidedOnly(Boolean.TRUE.equals(showNotDecidedOnlyModel.getObject()));
        table.setCurrentPage(0);

        target.add(getFeedbackPanel());
        target.add((Component) getCaseWorkItemsTable());
    }
    //endregion

    private static class SearchFragment extends Fragment {

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
                              IModel<Boolean> model) {
            super(id, markupId, markupProvider, model);

            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            final IModel<Boolean> model = (IModel<Boolean>) getDefaultModel();
        }

        private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
            return new AjaxFormComponentUpdatingBehavior("change") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    PageCaseWorkItems page = (PageCaseWorkItems) getPage();
                    page.searchFilterPerformed(target);
                }
            };
        }
    }
}
