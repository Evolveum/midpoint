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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
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
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDeleteDialogDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportOutputSearchDto;
import com.evolveum.midpoint.web.session.ReportsStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/reports/created", action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#createdReports",
                label = "PageCreatedReports.auth.createdReports.label",
                description = "PageCreatedReports.auth.createdReports.description")})
public class PageCreatedReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageCreatedReports.class);

    private static final String DOT_CLASS = PageCreatedReports.class.getName() + ".";
    private static final String OPERATION_DELETE = DOT_CLASS + "deleteReportOutput";
    private static final String OPERATION_DOWNLOAD_REPORT = DOT_CLASS + "downloadReport";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CREATED_REPORTS_TABLE = "table";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_FILTER_FILE_TYPE = "filetype";
    private static final String ID_REPORT_TYPE_SELECT = "reportType";
    private static final String ID_CONFIRM_DELETE = "confirmDeletePopup";

    private IModel<ReportOutputSearchDto> searchModel;
    private IModel<ReportDeleteDialogDto> deleteModel = new Model<>();
    private ReportOutputType currentReport;

    private static Map<ExportType, String> reportExportTypeMap = new HashMap<ExportType, String>();
    
    static{
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
    public PageCreatedReports() {
        this(new PageParameters(), null);
    }

    public PageCreatedReports(PageParameters pageParameters, PageBase previousPage) {
        super(pageParameters);

        setPreviousPage(previousPage);

        searchModel = new LoadableModel<ReportOutputSearchDto>() {
            @Override
            protected ReportOutputSearchDto load() {
                ReportsStorage storage = getSessionStorage().getReports();
                ReportOutputSearchDto dto = storage.getReportOutputSearch();

                if (dto == null) {
                    dto = new ReportOutputSearchDto();
                }

                return dto;
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageSubTitleModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("page.subTitle").getString();
            }
        };
    }

    private void initLayout() {
        initSearchForm();

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {

            @Override
            protected InputStream initStream() {
            	if (currentReport != null){
            		 String contentType = reportExportTypeMap.get(currentReport.getExportType());
            	        if (StringUtils.isEmpty(contentType)){
            	        	contentType = "multipart/mixed; charset=UTF-8";
            	        }
            	        setContentType(contentType);
            	}
            	
                return createReport();
            }
        };
       
//        ajaxDownloadBehavior.setContentType(contentType);
        mainForm.add(ajaxDownloadBehavior);

        ObjectDataProvider provider = new ObjectDataProvider(PageCreatedReports.this, ReportOutputType.class) {

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                ReportsStorage storage = getSessionStorage().getReports();
                storage.setReportOutputsPaging(paging);
            }
        };
        ObjectQuery query;

        String oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER).toString();
        if (oidValue != null && !StringUtils.isEmpty(oidValue)) {
            query = createReportTypeRefQuery(oidValue);
        } else {
            query = createQuery();
        }

        provider.setQuery(query);

        TablePanel table = new TablePanel(ID_CREATED_REPORTS_TABLE, provider, initColumns(ajaxDownloadBehavior),
                UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL, getItemsPerPage(UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL));
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        add(new ConfirmationDialog(ID_CONFIRM_DELETE, createStringResource("pageCreatedReports.dialog.title.confirmDelete"),
                createDeleteConfirmString()) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                close(target);

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
        });
    }

    private void initSearchForm() {
        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);

        //TODO - commented until FileType property will be available in ReportOutputType
        /*
        DropDownChoice filetypeSelect = new DropDownChoice(ID_FILTER_FILE_TYPE,
                new PropertyModel(searchModel, ReportOutputDto.F_FILE_TYPE),
                new AbstractReadOnlyModel<List<ExportType>>() {

                    @Override
                    public List<ExportType> getObject() {
                        return createFileTypeList();
                    }
                },
                new EnumChoiceRenderer(PageCreatedReports.this));
        filetypeSelect.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                fileTypeFilterPerformed(target);
            }
        });
        filetypeSelect.setOutputMarkupId(true);

        if(filetypeSelect.getModel().getObject() == null){
            filetypeSelect.getModel().setObject(null);
        }
        searchForm.add(filetypeSelect);
        */

        DropDownChoice reportTypeSelect = new DropDownChoice(ID_REPORT_TYPE_SELECT,
                new PropertyModel(searchModel, ReportOutputSearchDto.F_REPORT_TYPE),
                new AbstractReadOnlyModel<List<String>>() {

                    @Override
                    public List getObject() {
                        return createReportTypeList();
                    }
                },
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
                reportTypeFilterPerformed(target);
            }
        });
        reportTypeSelect.setOutputMarkupId(true);
        reportTypeSelect.setNullValid(true);

        if (getPageParameters().get(OnePageParameterEncoder.PARAMETER) != null) {
            createReportTypeList();

            for (String key : searchModel.getObject().getReportTypeMap().keySet()) {
                if (searchModel.getObject().getReportTypeMap().get(key).equals(getPageParameters().get(OnePageParameterEncoder.PARAMETER).toString())) {
                    reportTypeSelect.getModel().setObject(key);
                }
            }
        }
        searchForm.add(reportTypeSelect);

        BasicSearchPanel<ReportOutputSearchDto> basicSearch = new BasicSearchPanel<ReportOutputSearchDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, ReportOutputSearchDto.F_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                PageCreatedReports.this.searchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                PageCreatedReports.this.clearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);
    }

    //TODO - commented until FileType property will be available in ReportOutputType
    /*
    private List<ExportType> createFileTypeList(){
        List<ExportType> list = new ArrayList<ExportType>();
        Collections.addAll(list, ExportType.values());
        return list;
    }
    */

    private List<String> createReportTypeList() {
        searchModel.getObject().getReportTypeMap().clear();
        List<String> reportTypeNames = new ArrayList<String>();

        List<PrismObject<ReportType>> reportTypes = WebModelUtils.searchObjects(ReportType.class, null, null, getPageBase());

        for (PrismObject o : reportTypes) {
            ReportType reportType = (ReportType) o.asObjectable();

            if (reportType.isParent()) {
                String name = WebMiscUtil.getName(o);

                searchModel.getObject().getReportTypeMap().put(name, reportType.getOid());

                reportTypeNames.add(name);
            }
        }

        return reportTypeNames;
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    /*
    private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour(){
        return new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                filterPerformed(target);
            }
        };
    }
    */

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
                cellItem.add(new Label(componentId, new AbstractReadOnlyModel() {

                    @Override
                    public Object getObject() {
                        ReportOutputType object = rowModel.getObject().getValue();
                        MetadataType metadata = object.getMetadata();
                        if (metadata == null) {
                            return null;
                        }

                        return WebMiscUtil.formatDate(metadata.getCreateTimestamp());
                    }
                }));
            }
        };
        columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<ReportOutputType>>(new Model(), null) {

            @Override
            public String getFirstCap() {
                return createStringResource("pageCreatedReports.button.download").getString();
            }

            @Override
            public String getSecondCap() {
                return "";
            }

            @Override
            public String getFirstColorCssClass() {
                return BUTTON_COLOR_CLASS.PRIMARY.toString();
            }

            @Override
            public String getSecondColorCssClass() {
                return BUTTON_COLOR_CLASS.DANGER.toString() + " glyphicon glyphicon-trash";
            }

            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportOutputType>> model) {
                currentReport = model.getObject().getValue();
                downloadPerformed(target, model.getObject().getValue(), ajaxDownloadBehavior);
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportOutputType>> model) {
                deleteSelectedPerformed(target, ReportDeleteDialogDto.Operation.DELETE_SINGLE, model.getObject().getValue());
            }

            @Override
            public String getSecondSizeCssClass() {
                return BUTTON_SIZE_CLASS.SMALL.toString();
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
            if (row.isSelected()) {
                selected.add(row.getValue());
            }
        }

        return selected;
    }

    private ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType> getReportDataProvider() {
        DataTable table = getReportOutputTable().getDataTable();
        return (ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType>) table.getDataProvider();
    }

    private TablePanel getReportOutputTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_CREATED_REPORTS_TABLE));
    }

    private ObjectDataProvider getTableDataProvider() {
        TablePanel tablePanel = getReportOutputTable();
        DataTable table = tablePanel.getDataTable();
        return (ObjectDataProvider) table.getDataProvider();
    }

    private void deleteAllPerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op) {
        ReportDeleteDialogDto dto = new ReportDeleteDialogDto(op, null);
        deleteModel.setObject(dto);

        ModalWindow dialog = (ModalWindow) get(ID_CONFIRM_DELETE);
        dialog.show(target);
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

        ModalWindow dialog = (ModalWindow) get(ID_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteSelectedConfirmedPerformed(AjaxRequestTarget target, List<ReportOutputType> objects) {
        OperationResult result = new OperationResult(OPERATION_DELETE);

        for (ReportOutputType output : objects) {
            WebModelUtils.deleteObject(ReportOutputType.class, output.getOid(), result, this);
        }
        result.computeStatusIfUnknown();

        ObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getReportOutputTable());
        target.add(getFeedbackPanel());
    }

    private void deleteAllConfirmedPerformed(AjaxRequestTarget target) {
        //TODO - implement as background task
        warn("Not implemented yet, will be implemented as background task.");
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createReportTypeRefQuery(String oid) {

        ObjectQuery query = new ObjectQuery();

        try {
            RefFilter reportRef = RefFilter.createReferenceEqual(ReportOutputType.F_REPORT_REF, ReportOutputType.class,
                    getPrismContext(), oid);

            query.setFilter(reportRef);
            return query;
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't create query", e);
            error("Couldn't create query, reason: " + e.getMessage());
        }

        return null;
    }

    private ObjectQuery createQuery() {
        ReportOutputSearchDto dto = searchModel.getObject();
        ObjectQuery query = null;

        if (StringUtils.isEmpty(dto.getText())) {
            return null;
        }

        try {
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedString = normalizer.normalize(dto.getText());

            SubstringFilter substring = SubstringFilter.createSubstring(ReportOutputType.F_NAME, ReportOutputType.class,
                    getPrismContext(), PolyStringNormMatchingRule.NAME, normalizedString);

            query = ObjectQuery.createObjectQuery(substring);

        } catch (Exception e) {
            error(getString("pageCreatedReports.message.queryError") + " " + e.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", e);
        }

        return query;
    }

    private InputStream createReport() {
        OperationResult result = new OperationResult(OPERATION_DOWNLOAD_REPORT);
        ReportManager reportManager = getReportManager();

        if (currentReport == null) {
            return null;
        }

        InputStream input = null;
        try {
            input = reportManager.getReportOutputData(currentReport.getOid(), result);
        } catch (Exception e) {
            error(getString("pageCreatedReports.message.downloadError") + " " + e.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't download report.", e);
            LOGGER.trace(result.debugDump());
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResultInSession(result);
        }

        return input;
    }

    private void fileTypeFilterPerformed(AjaxRequestTarget target) {
        //TODO - perform filtering based on file type - need to wait for schema update (ReportOutputType)
    }

    private void reportTypeFilterPerformed(AjaxRequestTarget target) {
        ReportOutputSearchDto dto = searchModel.getObject();
        String oid = dto.getReportTypeMap().get(dto.getReportType());
        ObjectQuery query;

        if (oid == null || oid.isEmpty()) {
            query = createQuery();
        } else {
            query = createReportTypeRefQuery(oid);
        }

        TablePanel panel = getReportOutputTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportOutputSearch(searchModel.getObject());
        storage.setReportsPaging(null);
        panel.setCurrentPage(null);

        target.add(panel);
        target.add(getFeedbackPanel());
    }

    private void searchPerformed(AjaxRequestTarget target) {
        ObjectQuery query = createQuery();
        target.add(getFeedbackPanel());

        TablePanel panel = getReportOutputTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportOutputSearch(searchModel.getObject());
        storage.setReportOutputsPaging(null);
        panel.setCurrentPage(null);

        target.add(panel);
    }

    private void downloadPerformed(AjaxRequestTarget target, ReportOutputType report,
                                   AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {

        ajaxDownloadBehavior.initiate(target);
    }

    private void clearSearchPerformed(AjaxRequestTarget target) {
        searchModel.setObject(new ReportOutputSearchDto());

        TablePanel panel = getReportOutputTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(null);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportOutputSearch(searchModel.getObject());
        storage.setReportOutputsPaging(null);
        panel.setCurrentPage(null);

        target.add(get(ID_SEARCH_FORM));
        target.add(panel);
    }
}
