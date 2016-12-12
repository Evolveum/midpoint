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
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.reports.component.DownloadButtonPanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDeleteDialogDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportOutputSearchDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import com.evolveum.midpoint.web.session.ReportsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.io.InputStream;
import java.util.*;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/reports/created", action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_CREATED_REPORTS_URL,
                label = "PageCreatedReports.auth.createdReports.label",
                description = "PageCreatedReports.auth.createdReports.description")})
public class PageCreatedReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageCreatedReports.class);

    private static final String DOT_CLASS = PageCreatedReports.class.getName() + ".";
    private static final String OPERATION_DELETE = DOT_CLASS + "deleteReportOutput";
    private static final String OPERATION_DOWNLOAD_REPORT = DOT_CLASS + "downloadReport";
    private static final String OPERATION_GET_REPORT_FILENAME = DOT_CLASS + "getReportFilename";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CREATED_REPORTS_TABLE = "table";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_REPORT_TYPE_SELECT = "reportType";
    private static final String ID_TABLE_HEADER = "tableHeader";

    private LoadableModel<ReportOutputSearchDto> searchModel;
    private IModel<ReportDeleteDialogDto> deleteModel = new Model<>();
    private ReportOutputType currentReport;

    private static Map<ExportType, String> reportExportTypeMap = new HashMap<>();

    static {
        reportExportTypeMap.put(ExportType.CSV, "text/csv; charset=UTF-8");
        reportExportTypeMap.put(ExportType.DOCX, "application/vnd.openxmlformats-officedocument.wordprocessingml.document; charset=UTF-8");
        reportExportTypeMap.put(ExportType.HTML, "text/html; charset=UTF-8");
        reportExportTypeMap.put(ExportType.ODS, "application/vnd.oasis.opendocument.spreadsheet; charset=UTF-8");
        reportExportTypeMap.put(ExportType.ODT, "application/vnd.oasis.opendocument.text; charset=UTF-8");
        reportExportTypeMap.put(ExportType.PDF, "application/pdf; charset=UTF-8");
        reportExportTypeMap.put(ExportType.PPTX, "application/vnd.openxmlformats-officedocument.presentationml.presentation; charset=UTF-8");
        reportExportTypeMap.put(ExportType.RTF, "application/rtf; charset=UTF-8");
        reportExportTypeMap.put(ExportType.XHTML, "application/xhtml+xml; charset=UTF-8");
        reportExportTypeMap.put(ExportType.XLS, "application/vnd.ms-excel; charset=UTF-8");
        reportExportTypeMap.put(ExportType.XLSX, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=UTF-8");
        reportExportTypeMap.put(ExportType.XML, "application/xml; charset=UTF-8");
        reportExportTypeMap.put(ExportType.XML_EMBED, "text/xml; charset=UTF-8");

    }

    public PageCreatedReports(PageParameters pageParameters) {
        super(pageParameters);

        searchModel = new LoadableModel<ReportOutputSearchDto>(false) {

            @Override
            protected ReportOutputSearchDto load() {
                ReportsStorage storage = getSessionStorage().getReports();
                ReportOutputSearchDto dto = storage.getReportOutputSearch();

                if (dto != null) {
                    return dto;
                }

                return createSearchDto();
            }
        };

        initLayout();
    }

    private ReportOutputSearchDto createSearchDto() {
        ReportOutputSearchDto dto = new ReportOutputSearchDto();
        Map<String, String> reportTypeMap = dto.getReportTypeMap();

        List<PrismObject<ReportType>> reportTypes = WebModelServiceUtils.searchObjects(ReportType.class, null, null, getPageBase());
        LOGGER.debug("Found {} report types.", reportTypes.size());

        for (PrismObject o : reportTypes) {
            ReportType reportType = (ReportType) o.asObjectable();

            if (reportType.isParent()) {
                String name = WebComponentUtil.getName(o);
                reportTypeMap.put(name, reportType.getOid());
            }
        }

        StringValue param = getPage().getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (param != null) {
            for (String key : dto.getReportTypeMap().keySet()) {
                if (reportTypeMap.get(key).equals(param.toString())) {
                    dto.setReportType(key);
                }
            }
        }

        return dto;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {
            @Override
            protected InputStream initStream() {
                return createReport(this);
            }

            @Override
           public String getFileName(){
              return getReportFileName();
            }
        };

        mainForm.add(ajaxDownloadBehavior);

        ObjectDataProvider provider = new ObjectDataProvider(PageCreatedReports.this, ReportOutputType.class) {

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                ReportsStorage storage = getSessionStorage().getReports();
                storage.setReportOutputsPaging(paging);
            }

            @Override
            public ObjectQuery getQuery() {
                return createQuery();
            }
        };

        BoxedTablePanel table = new BoxedTablePanel(ID_CREATED_REPORTS_TABLE, provider,
                initColumns(ajaxDownloadBehavior),
                UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL,
                (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL)) {

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageCreatedReports.this, searchModel);
            }
        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    //TODO - commented until FileType property will be available in ReportOutputType

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    //TODO - consider adding Author name, File Type and ReportType to columns
    private List<IColumn<SelectableBean<ReportOutputType>, String>> initColumns(
            final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {
        List<IColumn<SelectableBean<ReportOutputType>, String>> columns = new ArrayList<>();

        IColumn column;

        column = new CheckBoxHeaderColumn();
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageCreatedReports.table.name"), "name", "value.name");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageCreatedReports.table.description"), "value.description");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<ReportOutputType>, String>(
                createStringResource("pageCreatedReports.table.time"),
                "createTimestamp") {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ReportOutputType>>> cellItem,
                                     String componentId, final IModel<SelectableBean<ReportOutputType>> rowModel) {
                cellItem.add(new DateLabelComponent(componentId, new AbstractReadOnlyModel<Date>() {

                    @Override
                    public Date getObject() {
                        ReportOutputType object = rowModel.getObject().getValue();
                        MetadataType metadata = object != null ? object.getMetadata() : null;
                        if (metadata == null) {
                            return null;
                        }

                        return XmlTypeConverter.toDate(metadata.getCreateTimestamp());                   }
                }, DateLabelComponent.LONG_MEDIUM_STYLE));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<ReportOutputType>, String>(new Model(), null) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ReportOutputType>>> cellItem,
                                     String componentId, final IModel<SelectableBean<ReportOutputType>> model) {

                DownloadButtonPanel panel = new DownloadButtonPanel(componentId) {

                    @Override
                    protected void deletePerformed(AjaxRequestTarget target) {
                        deleteSelectedPerformed(target, ReportDeleteDialogDto.Operation.DELETE_SINGLE,
                                model.getObject().getValue());
                    }

                    @Override
                    protected void downloadPerformed(AjaxRequestTarget target) {
                        currentReport = model.getObject().getValue();
                        PageCreatedReports.this.
                                downloadPerformed(target, model.getObject().getValue(), ajaxDownloadBehavior);
                    }
                };
                cellItem.add(panel);
            }
        };
        columns.add(column);

        column = new InlineMenuHeaderColumn<InlineMenuable>(initInlineMenu()) {

            @Override
            public void populateItem(Item<ICellPopulator<InlineMenuable>> cellItem, String componentId,
                                     IModel<InlineMenuable> rowModel) {
                cellItem.add(new Label(componentId));
            }
        };
        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<>();

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageCreatedReports.inlineMenu.deleteAll"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deleteAllPerformed(target, ReportDeleteDialogDto.Operation.DELETE_ALL);
                    }
                }
        ));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageCreatedReports.inlineMenu.deleteSelected"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deleteSelectedPerformed(target, ReportDeleteDialogDto.Operation.DELETE_SELECTED, null);
                    }
                }
        ));

        return headerMenuItems;
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ReportDeleteDialogDto dto = deleteModel.getObject();

                switch (dto.getOperation()) {
                    case DELETE_SINGLE:
                        ReportOutputType report = dto.getObjects().get(0);
                        return createStringResource("pageCreatedReports.message.deleteOutputSingle",
                                report.getName().getOrig()).getString();
                    case DELETE_ALL:
                        return createStringResource("pageCreatedReports.message.deleteAll").getString();
                    default:
                        return createStringResource("pageCreatedReports.message.deleteOutputConfirmed",
                                getSelectedData().size()).getString();
                }
            }
        };
    }

    private List<ReportOutputType> getSelectedData() {
        ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType> provider = getReportDataProvider();

        List<SelectableBean<ReportOutputType>> rows = provider.getAvailableData();
        List<ReportOutputType> selected = new ArrayList<>();

        for (SelectableBean<ReportOutputType> row : rows) {
            if (row.isSelected() && row.getValue() != null) {
                selected.add(row.getValue());
            }
        }

        return selected;
    }

    private ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType> getReportDataProvider() {
        DataTable table = getReportOutputTable().getDataTable();
        return (ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType>) table.getDataProvider();
    }

    private Table getReportOutputTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_CREATED_REPORTS_TABLE));
    }

    private ObjectDataProvider getTableDataProvider() {
        Table tablePanel = getReportOutputTable();
        DataTable table = tablePanel.getDataTable();
        return (ObjectDataProvider) table.getDataProvider();
    }

    private void deleteAllPerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op) {
        final ReportDeleteDialogDto dto = new ReportDeleteDialogDto(op, null);
        deleteModel.setObject(dto);

        getPageBase().showMainPopup(getDeleteDialogPanel(), target);
    }

    private ConfirmationPanel getDeleteDialogPanel(){
        ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), createDeleteConfirmString()){
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);

                ReportDeleteDialogDto dto = deleteModel.getObject();
                switch (dto.getOperation()) {
                    case DELETE_SINGLE:
                        deleteSelectedConfirmedPerformed(target, Arrays.asList(dto.getObjects().get(0)));
                        break;
                    case DELETE_SELECTED:
                        deleteSelectedConfirmedPerformed(target, dto.getObjects());
                        break;
                    case DELETE_ALL:
                        deleteAllConfirmedPerformed(target);
                        break;
                }
            }
        };
        return dialog;
    }


    private void deleteSelectedPerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op, ReportOutputType single) {
        List<ReportOutputType> selected = getSelectedData();

        if (single != null) {
            selected.clear();
            selected.add(single);
        }

        if (selected.isEmpty()) {
            return;
        }

        ReportDeleteDialogDto dto = new ReportDeleteDialogDto(op, selected);
        deleteModel.setObject(dto);

        getPageBase().showMainPopup(getDeleteDialogPanel(), target);
    }

    private void deleteSelectedConfirmedPerformed(AjaxRequestTarget target, List<ReportOutputType> objects) {
        OperationResult result = new OperationResult(OPERATION_DELETE);

        for (ReportOutputType output : objects) {
            WebModelServiceUtils.deleteObject(ReportOutputType.class, output.getOid(), result, this);
        }
        result.computeStatusIfUnknown();

        ObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add((Component) getReportOutputTable());
        target.add(getFeedbackPanel());
    }

    private void deleteAllConfirmedPerformed(AjaxRequestTarget target) {
        //TODO - implement as background task
        warn("Not implemented yet, will be implemented as background task.");
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createQuery() {
        ReportOutputSearchDto dto = searchModel.getObject();
        S_AtomicFilterEntry q = QueryBuilder.queryFor(ReportOutputType.class, getPrismContext());

        if (StringUtils.isNotEmpty(dto.getText())) {
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedString = normalizer.normalize(dto.getText());
            q = q.item(ReportOutputType.F_NAME).containsPoly(normalizedString).matchingNorm().and();
        }

        String oid = dto.getReportTypeMap().get(dto.getReportType());
        if (StringUtils.isNotEmpty(oid)) {
            q = q.item(ReportOutputType.F_REPORT_REF).ref(oid).and();
        }

        return q.all().build();
    }

    private InputStream createReport(AjaxDownloadBehaviorFromStream ajaxDownloadBehaviorFromStream) {
		return createReport(currentReport, ajaxDownloadBehaviorFromStream, this);
	}

	public static InputStream createReport(ReportOutputType report, AjaxDownloadBehaviorFromStream ajaxDownloadBehaviorFromStream, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_DOWNLOAD_REPORT);
        ReportManager reportManager = pageBase.getReportManager();

		if (report == null) {
            return null;
        }

		String contentType = reportExportTypeMap.get(report.getExportType());
		if (StringUtils.isEmpty(contentType)) {
			contentType = "multipart/mixed; charset=UTF-8";
		}
		ajaxDownloadBehaviorFromStream.setContentType(contentType);

		InputStream input = null;
        try {
            input = reportManager.getReportOutputData(report.getOid(), result);
        } catch (Exception e) {
            pageBase.error(pageBase.getString("pageCreatedReports.message.downloadError") + " " + e.getMessage());
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't download report.", e);
            LOGGER.trace(result.debugDump());
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            pageBase.showResult(result);
        }

        return input;
    }

    private void fileTypeFilterPerformed(AjaxRequestTarget target) {
        //TODO - perform filtering based on file type - need to wait for schema update (ReportOutputType)
    }

    private void downloadPerformed(AjaxRequestTarget target, ReportOutputType report,
                                   AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {

        ajaxDownloadBehavior.initiate(target);
    }

    private void searchPerformed(AjaxRequestTarget target) {
        refreshTable(target);
    }

    private void clearSearchPerformed(AjaxRequestTarget target) {
        ReportOutputSearchDto dto = searchModel.getObject();
        dto.setReportType(null);
        dto.setText(null);

        refreshTable(target);
    }

    private void refreshTable(AjaxRequestTarget target) {
        Table panel = getReportOutputTable();

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportOutputSearch(searchModel.getObject());
        storage.setReportOutputsPaging(null);
        panel.setCurrentPage(0);

        target.add((Component) panel);
        target.add(getFeedbackPanel());
    }

    private static class SearchFragment extends Fragment {

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
                              IModel<ReportOutputSearchDto> model) {
            super(id, markupId, markupProvider, model);

            initLayout();
        }

        private void initLayout() {
            final Form searchForm = new Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            final IModel<ReportOutputSearchDto> model = (IModel) getDefaultModel();

            BasicSearchPanel<ReportOutputSearchDto> basicSearch =
                    new BasicSearchPanel<ReportOutputSearchDto>(ID_BASIC_SEARCH, model) {

                        @Override
                        protected IModel<String> createSearchTextModel() {
                            return new PropertyModel<String>(model, UsersDto.F_TEXT);
                        }

                        @Override
                        protected void searchPerformed(AjaxRequestTarget target) {
                            PageCreatedReports page = (PageCreatedReports) getPage();
                            page.searchPerformed(target);
                        }

                        @Override
                        protected void clearSearchPerformed(AjaxRequestTarget target) {
                            PageCreatedReports page = (PageCreatedReports) getPage();
                            page.clearSearchPerformed(target);
                        }
                    };
            searchForm.add(basicSearch);

            DropDownChoice reportTypeSelect = new DropDownChoice(ID_REPORT_TYPE_SELECT,
                    new PropertyModel(model, ReportOutputSearchDto.F_REPORT_TYPE),
                    new PropertyModel(model, ReportOutputSearchDto.F_REPORT_TYPES),
                    new ChoiceRenderer()
            ) {

                @Override
                protected String getNullValidDisplayValue() {
                    return getString("pageCreatedReports.filter.reportType");
                }

            };
            reportTypeSelect.add(new OnChangeAjaxBehavior() {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    PageCreatedReports page = (PageCreatedReports) getPage();
                    page.searchPerformed(target);
                }
            });
            reportTypeSelect.setOutputMarkupId(true);
            reportTypeSelect.setNullValid(true);
            searchForm.add(reportTypeSelect);
        }
    }

    private String getReportFileName(){
        try {
            OperationResult result = new OperationResult(OPERATION_GET_REPORT_FILENAME);
            ReportOutputType reportOutput = WebModelServiceUtils.loadObject(ReportOutputType.class, currentReport.getOid(), getPageBase(),
                    null, result).asObjectable();
            String fileName = reportOutput.getFilePath();
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            return fileName;
        } catch (Exception ex){
            //nothing to do
        }
        return null;
    }
}
