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

package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportSearchDto;
import com.evolveum.midpoint.web.session.ReportsStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/reports", action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#reports",
                label = "PageReports.auth.reports.label",
                description = "PageReports.auth.reports.description")})
public class PageReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageReports.class);

    private static final String DOT_CLASS = PageReports.class.getName() + ".";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runReport";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_REPORTS_TABLE = "reportsTable";

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_SUBREPORTS = "subReportCheckbox";

    private IModel<ReportSearchDto> searchModel;

    public PageReports() {
        searchModel = new LoadableModel<ReportSearchDto>() {

            @Override
            protected ReportSearchDto load() {
                ReportsStorage storage = getSessionStorage().getReports();
                ReportSearchDto dto = storage.getReportSearch();

                if(dto == null){
                    dto = new ReportSearchDto();
                }

                return dto;
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageSubTitleModel(){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("page.subTitle").getString();
            }
        };
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        initSearchForm(searchForm);

        ObjectDataProvider provider = new ObjectDataProvider(PageReports.this, ReportType.class);
        provider.setQuery(createQuery());
        TablePanel table = new TablePanel<>(ID_REPORTS_TABLE, provider, initColumns());
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private void initSearchForm(Form<?> searchForm){

        CheckBox showSubreports = new CheckBox(ID_SUBREPORTS,
                new PropertyModel(searchModel, ReportSearchDto.F_PARENT));
        showSubreports.add(createFilterAjaxBehaviour());
        searchForm.add(showSubreports);

        BasicSearchPanel<ReportSearchDto> basicSearch = new BasicSearchPanel<ReportSearchDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, ReportSearchDto.F_SEARCH_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                PageReports.this.searchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                PageReports.this.clearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);
    }

    private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
        return new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        };
    }

    private List<IColumn<ReportType, String>> initColumns() {
        List<IColumn<ReportType, String>> columns = new ArrayList<IColumn<ReportType, String>>();

        IColumn column;
        column = new LinkColumn<SelectableBean<ReportType>>(createStringResource("PageReports.table.name"),
                ReportType.F_NAME.getLocalPart(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> rowModel){
                ReportType report = rowModel.getObject().getValue();
                reportTypeFilterPerformed(target, report.getOid());
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<ReportType>> rowModel){
                if(rowModel.getObject().getValue().isParent()){
                    return true;
                } else {
                    return false;
                }
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageReports.table.description"), "value.description");
        columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<ReportType>>(new Model(), null){

            @Override
            public String getFirstCap(){
                return PageReports.this.createStringResource("PageReports.button.run").getString();
            }

            @Override
            public String getSecondCap(){
                return PageReports.this.createStringResource("PageReports.button.configure").getString();
            }

            @Override
            public String getFirstColorCssClass(){
                if(getRowModel().getObject().getValue().isParent()){
                    return BUTTON_COLOR_CLASS.PRIMARY.toString();
                } else {
                    return BUTTON_COLOR_CLASS.PRIMARY.toString() + " " + BUTTON_DISABLED;
                }
            }

            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model){
                runReportPerformed(target, model.getObject().getValue());
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model){
                configurePerformed(target, model.getObject().getValue());
            }

            @Override
            public boolean isFirstButtonEnabled(IModel<SelectableBean<ReportType>> rowModel){
                return rowModel.getObject().getValue().isParent();
            }
        };
        columns.add(column);

        return columns;
    }

    private void reportTypeFilterPerformed(AjaxRequestTarget target, String oid){
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, oid);
        setResponsePage(new PageCreatedReports(params, PageReports.this));
    }

    private void runReportPerformed(AjaxRequestTarget target, ReportType report){
        LOGGER.debug("Run report performed for {}", new Object[]{report.asPrismObject()});

        OperationResult result = new OperationResult(OPERATION_RUN_REPORT);
        try {
            Task task = createSimpleTask(OPERATION_RUN_REPORT);
            getReportManager().runReport(report.asPrismObject(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void configurePerformed(AjaxRequestTarget target, ReportType report){
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, report.getOid());
        setResponsePage(PageReport.class, params);
    }

    private ObjectDataProvider getDataProvider(){
        DataTable table = getReportTable().getDataTable();
        return (ObjectDataProvider) table.getDataProvider();
    }

    private TablePanel getReportTable(){
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_REPORTS_TABLE));
    }

    private void searchPerformed(AjaxRequestTarget target){
        ObjectQuery query = createQuery();
        ObjectDataProvider provider = getDataProvider();
        provider.setQuery(query);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportSearch(searchModel.getObject());
        storage.setReportsPaging(null);

        TablePanel table = getReportTable();
        table.setCurrentPage(null);
        target.add(table);
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createQuery(){
        ReportSearchDto dto = searchModel.getObject();
        String text = dto.getText();
        Boolean parent = !dto.isParent();
        ObjectQuery query = new ObjectQuery();
        List<ObjectFilter> filters = new ArrayList<>();

        if(StringUtils.isNotEmpty(text)){
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedText = normalizer.normalize(text);

            ObjectFilter substring = SubstringFilter.createSubstring(ReportType.F_NAME, ReportType.class,
                    getPrismContext(), PolyStringNormMatchingRule.NAME, normalizedText);

            filters.add(substring);
        }

        if(parent == true){
            EqualFilter parentFilter = EqualFilter.createEqual(ReportType.F_PARENT, ReportType.class,
                    getPrismContext(), null, parent);
            filters.add(parentFilter);
        }

        if(!filters.isEmpty()){
            query.setFilter(AndFilter.createAnd(filters));
        } else {
            query = null;
        }

        return query;
    }

    private void clearSearchPerformed(AjaxRequestTarget target){
        searchModel.setObject(new ReportSearchDto());

        TablePanel panel = getReportTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(createQuery());

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportSearch(searchModel.getObject());
        storage.setReportsPaging(null);
        panel.setCurrentPage(null);

        target.add(get(ID_SEARCH_FORM));
        target.add(panel);
    }
}
