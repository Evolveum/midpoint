/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.component.RunReportPopupPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportSearchDto;
import com.evolveum.midpoint.web.session.ReportsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

//import com.evolveum.midpoint.report.impl.ReportConstants;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
            @Url(mountUrl = "/admin/reports", matchUrlForSecurity = "/admin/reports")
        },
        action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_URL,
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
    private static final String ID_TABLE_HEADER = "tableHeader";

    private IModel<ReportSearchDto> searchModel;

    public PageReports() {
        searchModel = new LoadableModel<ReportSearchDto>() {

            @Override
            protected ReportSearchDto load() {
                ReportsStorage storage = getSessionStorage().getReports();
                ReportSearchDto dto = storage.getReportSearch();

                if (dto == null) {
                    dto = new ReportSearchDto();
                }

                return dto;
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        ObjectDataProvider provider = new ObjectDataProvider(PageReports.this, ReportType.class);
        provider.setQuery(createQuery());

        BoxedTablePanel table = new BoxedTablePanel(ID_REPORTS_TABLE, provider, initColumns(),
                UserProfileStorage.TableId.PAGE_REPORTS,
                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_REPORTS)) {

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageReports.this, searchModel);
            }
        };
        table.setShowPaging(false);
        table.setOutputMarkupId(true);
        mainForm.add(table);

    }

    private List<IColumn<ReportType, String>> initColumns() {
        List<IColumn<ReportType, String>> columns = new ArrayList<IColumn<ReportType, String>>();

        IColumn column;
        column = new LinkColumn<SelectableBean<ReportType>>(createStringResource("PageReports.table.name"),
                ReportType.F_NAME.getLocalPart(), "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> rowModel) {
                ReportType report = rowModel.getObject().getValue();
                if (report != null) {
                    reportTypeFilterPerformed(target, report.getOid());
                }
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<ReportType>> rowModel) {
				return rowModel.getObject().getValue() != null && rowModel.getObject().getValue().isParent();
            }
        };
        columns.add(column);

        column = new PropertyColumn(createStringResource("PageReports.table.description"), "value.description");
        columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<ReportType>>(new Model(), null) {

            @Override
            public String getFirstCap() {
                return PageReports.this.createStringResource("PageReports.button.run").getString();
            }

            @Override
            public String getSecondCap() {
                return PageReports.this.createStringResource("PageReports.button.configure").getString();
            }

            @Override
            public String getFirstColorCssClass() {
                if (getRowModel().getObject().getValue() != null && getRowModel().getObject().getValue().isParent()) {
                    return BUTTON_COLOR_CLASS.PRIMARY.toString();
                } else {
                    return BUTTON_COLOR_CLASS.PRIMARY.toString() + " " + BUTTON_DISABLED;
                }
            }

            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model) {
                runReportPerformed(target, model.getObject().getValue());
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportType>> model) {
                configurePerformed(target, model.getObject().getValue());
            }

            @Override
            public boolean isFirstButtonEnabled(IModel<SelectableBean<ReportType>> rowModel) {
                return getRowModel().getObject().getValue() != null && rowModel.getObject().getValue().isParent();
            }
        };
        columns.add(column);

        return columns;
    }

    private void reportTypeFilterPerformed(AjaxRequestTarget target, String oid) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageCreatedReports.class, params);
    }

    protected void runReportPerformed(AjaxRequestTarget target, ReportType report) {

    	RunReportPopupPanel runReportPopupPanel = new RunReportPopupPanel(getMainPopupBodyId(), report) {

    		private static final long serialVersionUID = 1L;

			protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType, PrismContainer<ReportParameterType> reportParam) {
    			OperationResult result = new OperationResult(OPERATION_RUN_REPORT);
    	        try {

    	            Task task = createSimpleTask(OPERATION_RUN_REPORT);

    	            getReportManager().runReport(reportType.asPrismObject(), reportParam, task, result);
    	        } catch (Exception ex) {
    	            result.recordFatalError(ex);
    	        } finally {
    	            result.computeStatusIfUnknown();
    	        }

    	        showResult(result);
    	        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM)));
    	        hideMainPopup(target);

    		};
    	};
    	showMainPopup(runReportPopupPanel, target);

    }

    private void configurePerformed(AjaxRequestTarget target, ReportType report) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, report.getOid());
        navigateToNext(PageReport.class, params);
    }

    private ObjectDataProvider getDataProvider() {
        DataTable table = getReportTable().getDataTable();
        return (ObjectDataProvider) table.getDataProvider();
    }

    private Table getReportTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_REPORTS_TABLE));
    }

    private void searchPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createQuery();
        ObjectDataProvider provider = getDataProvider();
        provider.setQuery(query);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportSearch(searchModel.getObject());
        storage.setPaging(null);

        Table table = getReportTable();
        table.setCurrentPage(null);
        target.add((Component) table);
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createQuery() {
        ReportSearchDto dto = searchModel.getObject();
        String text = dto.getText();
        Boolean parent = !dto.isParent();

        S_AtomicFilterEntry q = QueryBuilder.queryFor(ReportType.class, getPrismContext());
        if (StringUtils.isNotEmpty(text)) {
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedText = normalizer.normalize(text);
            q = q.item(ReportType.F_NAME).eqPoly(normalizedText).matchingNorm().and();
        }
        if (parent) {
            q = q.item(ReportType.F_PARENT).eq(true).and();
        }
        return q.all().build();
    }

    private void clearSearchPerformed(AjaxRequestTarget target) {
        searchModel.setObject(new ReportSearchDto());

        Table panel = getReportTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(createQuery());

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportSearch(searchModel.getObject());
        storage.setPaging(null);
        panel.setCurrentPage(null);

        target.add((Component) panel);
    }

//    private void initRunReportModal() {
//        ModalWindow window = createModalWindow(MODAL_ID_RUN_REPORT,
//                createStringResource("Run report"), 1100, 560);
//        window.setContent(new RunReportPopupPanel(window.getContentId()) {
//
//            @Override
//            protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType, PrismContainer<ReportParameterType> params) {
//                runReportPerformed(target, reportType, params);
//            }
//        });
//        add(window);
//        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
//
//            @Override
//            public void onClose(AjaxRequestTarget target) {
//                target.appendJavaScript("$('.wicket-aa-container').remove();");
//            }
//        });
//    }

//    private void showRunReportPopup(AjaxRequestTarget target, ReportType reportType) {
//        ModalWindow modal = (ModalWindow) get(MODAL_ID_RUN_REPORT);
//        RunReportPopupPanel content = (RunReportPopupPanel) modal.get(modal.getContentId());
////        ReportDto reportDto = new ReportDto()
//        content.setReportType(reportType);
////        ModalWindow window = (ModalWindow) get(MODAL_ID_RUN_REPORT);
//        modal.show(target);
//        target.add(getFeedbackPanel());
////        showModalWindow(MODAL_ID_RUN_REPORT, target);
////        target.add(getFeedbackPanel());
//    }


    private static class SearchFragment extends Fragment {

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
                              IModel<ReportSearchDto> model) {
            super(id, markupId, markupProvider, model);

            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            final IModel<ReportSearchDto> model = (IModel) getDefaultModel();

            BasicSearchPanel<ReportSearchDto> basicSearch = new BasicSearchPanel<ReportSearchDto>(ID_BASIC_SEARCH, model) {

                @Override
                protected IModel<String> createSearchTextModel() {
                    return new PropertyModel<>(model, ReportSearchDto.F_SEARCH_TEXT);
                }

                @Override
                protected void searchPerformed(AjaxRequestTarget target) {
                    PageReports page = (PageReports) getPage();
                    page.searchPerformed(target);
                }

                @Override
                protected void clearSearchPerformed(AjaxRequestTarget target) {
                    PageReports page = (PageReports) getPage();
                    page.clearSearchPerformed(target);
                }
            };
            searchForm.add(basicSearch);

            CheckBox showSubreports = new CheckBox(ID_SUBREPORTS,
                    new PropertyModel(model, ReportSearchDto.F_PARENT));
            showSubreports.add(createFilterAjaxBehaviour());
            searchForm.add(showSubreports);
        }

        private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour() {
            return new AjaxFormComponentUpdatingBehavior("change") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    PageReports page = (PageReports) getPage();
                    page.searchPerformed(target);
                }
            };
        }
    }
}
