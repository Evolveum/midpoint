/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.page.admin.configuration.PageTraceView;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.StringChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDeleteDialogDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/reports/created", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_CREATED_REPORTS_URL,
                label = "PageCreatedReports.auth.createdReports.label",
                description = "PageCreatedReports.auth.createdReports.description")})
public class PageCreatedReports extends PageAdminObjectList<ReportOutputType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageCreatedReports.class);

    private static final String DOT_CLASS = PageCreatedReports.class.getName() + ".";
    private static final String OPERATION_DELETE = DOT_CLASS + "deleteReportOutput";
    private static final String OPERATION_DOWNLOAD_REPORT = DOT_CLASS + "downloadReport";
    private static final String OPERATION_GET_REPORT_FILENAME = DOT_CLASS + "getReportFilename";
    private static final String OPERATION_LOAD_REPORTS = DOT_CLASS + "loadReports";

    private static final String ID_REPORT_TYPE_SELECT = "reportType";


    private IModel<ReportDeleteDialogDto> deleteModel = new Model<>();
    private ReportOutputType currentReport;

    private static Map<ExportType, String> reportExportTypeMap = new HashMap<>();
    private Map<String, String> reportTypeMal = new HashMap<>();

    private AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = null;

    static {
        reportExportTypeMap.put(ExportType.CSV, "text/csv; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.DOCX, "application/vnd.openxmlformats-officedocument.wordprocessingml.document; charset=UTF-8");
        reportExportTypeMap.put(ExportType.HTML, "text/html; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.ODS, "application/vnd.oasis.opendocument.spreadsheet; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.ODT, "application/vnd.oasis.opendocument.text; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.PDF, "application/pdf; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.PPTX, "application/vnd.openxmlformats-officedocument.presentationml.presentation; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.RTF, "application/rtf; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.XHTML, "application/xhtml+xml; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.XLS, "application/vnd.ms-excel; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.XLSX, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.XML, "application/xml; charset=UTF-8");
//        reportExportTypeMap.put(JasperExportType.XML_EMBED, "text/xml; charset=UTF-8");

    }

    public PageCreatedReports(PageParameters pageParameters) {
        super(pageParameters);

        initReportTypeMap();
    }

    private void initReportTypeMap() {
        OperationResult result = new OperationResult(OPERATION_LOAD_REPORTS);
        List<PrismObject<ReportType>> reports = WebModelServiceUtils.searchObjects(ReportType.class, null, result, this);
        reportTypeMal = new HashMap<>();
        for (PrismObject<ReportType> report : reports) {
            ReportType reportType = report.asObjectable();
            reportTypeMal.put(reportType.getOid(), WebComponentUtil.getName(reportType));
        }
    }

    private String getReportType(){
         StringValue param = getPage().getPageParameters().get(OnePageParameterEncoder.PARAMETER);
         if (param != null) {
             return param.toString();
         }
         return "undefined";
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        customInitLayout();
    }

    private void customInitLayout() {

        String reportName = reportTypeMal.get(getReportType());
        List<String> values = new ArrayList<>(reportTypeMal.values());
        DropDownChoicePanel<String> reportTypeSelect = new DropDownChoicePanel(ID_REPORT_TYPE_SELECT,
                Model.of(reportName),
                Model.ofList(values),
                StringChoiceRenderer.simple(), true);

        reportTypeSelect.getBaseFormComponent().add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getObjectListPanel().refreshTable(ReportOutputType.class, target);
            }
        });
        reportTypeSelect.setOutputMarkupId(true);
        getMainForm().add(reportTypeSelect);


        ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {

            private static final long serialVersionUID = 1L;

            @Override
            protected InputStream initStream() {
                return createReport();
            }

            @Override
           public String getFileName(){
              return getReportFileName();
            }
        };

        getMainForm().add(ajaxDownloadBehavior);
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        return PageCreatedReports.this.initInlineMenu();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, ReportOutputType object) {
        // TODO Auto-generated method stub
    }

    @Override
    protected boolean isCreateNewObjectEnabled(){
        return false;
    }

    @Override
    protected boolean isNameColumnClickable(IModel<SelectableBean<ReportOutputType>> rowModel) {
        return false;
    }

    @Override
    protected ObjectQuery addCustomFilterToContentQuery(ObjectQuery query) {
        return appendTypeFilter(query);
    }

    @Override
    protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {

        if (sortParam != null && sortParam.getProperty() != null) {
            OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
            if (sortParam.getProperty().equals("createTimestamp")) {
                return Collections.singletonList(
                        getPrismContext().queryFactory().createOrdering(
                                ItemPath.create(ReportOutputType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), order));
            }
            return Collections.singletonList(
                    getPrismContext().queryFactory().createOrdering(
                            ItemPath.create(new QName(SchemaConstantsGenerated.NS_COMMON, sortParam.getProperty())), order));


        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected UserProfileStorage.TableId getTableId(){
        return UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL;
    }

    //TODO - commented until FileType property will be available in ReportOutputType

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    //TODO - consider adding Author name, File Type and ReportType to columns
    protected List<IColumn<SelectableBean<ReportOutputType>, String>> initColumns() {
            List<IColumn<SelectableBean<ReportOutputType>, String>> columns = new ArrayList<>();

         IColumn<SelectableBean<ReportOutputType>, String> column = new PropertyColumn<>(createStringResource("pageCreatedReports.table.description"), "value.description");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<ReportOutputType>, String>(
                createStringResource("pageCreatedReports.table.time"),
                "createTimestamp") {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ReportOutputType>>> cellItem,
                                     String componentId, final IModel<SelectableBean<ReportOutputType>> rowModel) {
                cellItem.add(new DateLabelComponent(componentId, new IModel<Date>() {

                    private static final long serialVersionUID = 1L;
                    @Override
                    public Date getObject() {
                        ReportOutputType object = rowModel.getObject().getValue();
                        MetadataType metadata = object != null ? object.getMetadata() : null;
                        if (metadata == null) {
                            return null;
                        }

                        return XmlTypeConverter.toDate(metadata.getCreateTimestamp());                   }
                }, WebComponentUtil.getShortDateTimeFormat(PageCreatedReports.this)));
            }
        };
        columns.add(column);

//        column = new AbstractColumn<SelectableBean<ReportOutputType>, String>(new Model(), null) {
//
//            private static final long serialVersionUID = 1L;
//            @Override
//            public void populateItem(Item<ICellPopulator<SelectableBean<ReportOutputType>>> cellItem,
//                                     String componentId, final IModel<SelectableBean<ReportOutputType>> model) {
//
//                DownloadButtonPanel panel = new DownloadButtonPanel(componentId) {
//
//                    private static final long serialVersionUID = 1L;
//                    @Override
//                    protected void deletePerformed(AjaxRequestTarget target) {
//                        deleteSelectedPerformed(target, ReportDeleteDialogDto.Operation.DELETE_SINGLE,
//                                model.getObject().getValue());
//                    }
//
//                    @Override
//                    protected void downloadPerformed(AjaxRequestTarget target) {
//                        currentReport = model.getObject().getValue();
//                        PageCreatedReports.this.
//                                downloadPerformed(target, model.getObject().getValue(), ajaxDownloadBehavior);
//                    }
//                };
//                cellItem.add(panel);
//            }
//        };
//        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> menuItems = new ArrayList<>();

//        menuItems.add(new InlineMenuItem(createStringResource("pageCreatedReports.inlineMenu.deleteAll")) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public InlineMenuItemAction initAction() {
//                return new HeaderMenuAction(PageCreatedReports.this) {
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        deleteAllPerformed(target, ReportDeleteDialogDto.Operation.DELETE_ALL);
//                    }
//                };
//            }
//        });

        menuItems.add(new ButtonInlineMenuItem(createStringResource("pageCreatedReports.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ReportOutputType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ReportOutputType report = null;
                        if (getRowModel() != null) {
                            SelectableBeanImpl<ReportOutputType> rowDto = getRowModel().getObject();
                            report = rowDto.getValue();
                        }
                        deleteSelectedPerformed(target, ReportDeleteDialogDto.Operation.DELETE_SELECTED, report);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-minus");
            }
        });

        boolean canViewTraces;
        boolean canReadTraces;
        try {
            canReadTraces = isAuthorized(ModelAuthorizationAction.READ_TRACE.getUrl());
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't authorize reading traces", t);
            canReadTraces = false;
        }

        canViewTraces = canReadTraces && WebModelServiceUtils.isEnableExperimentalFeature(this);

        ButtonInlineMenuItem item = new ButtonInlineMenuItem(createStringResource("DownloadButtonPanel.download")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ReportOutputType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ReportOutputType> rowDto = getRowModel().getObject();
                        currentReport = rowDto.getValue();
                        downloadPerformed(target, rowDto.getValue(), ajaxDownloadBehavior);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-download");
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }
        };
        if (!canReadTraces) {
            item.setVisibilityChecker((rowModel, isHeader) -> !isTrace(rowModel));
        }
        menuItems.add(item);

        ButtonInlineMenuItem viewTraceItem = new ButtonInlineMenuItem(createStringResource("DownloadButtonPanel.viewTrace")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ReportOutputType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ReportOutputType> rowDto = getRowModel().getObject();
                        currentReport = rowDto.getValue();
                        PageParameters parameters = new PageParameters();
                        parameters.add(PageTraceView.PARAM_OBJECT_ID, currentReport.getOid());
                        navigateToNext(PageTraceView.class, parameters);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-eye");
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
        menuItems.add(viewTraceItem);
        viewTraceItem.setVisibilityChecker((rowModel, isHeader) -> canViewTraces && isTrace(rowModel));
        return menuItems;
    }

    private boolean isTrace(IModel<?> rowModel) {
        //noinspection unchecked
        SelectableBean<ReportOutputType> row = (SelectableBean<ReportOutputType>) rowModel.getObject();
        return ObjectTypeUtil.hasArchetype(row.getValue(), SystemObjectsType.ARCHETYPE_TRACE.value());
    }

    private IModel<String> createDeleteConfirmString() {
        return new IModel<String>() {

            private static final long serialVersionUID = 1L;

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
                                deleteModel.getObject().getObjects().size()).getString();
                }
            }
        };
    }

    private List<ReportOutputType> getSelectedData() {
        return getObjectListPanel().getSelectedObjects();
    }

    private void deleteAllPerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op) {
        final ReportDeleteDialogDto dto = new ReportDeleteDialogDto(op, null);
        deleteModel.setObject(dto);

        getPageBase().showMainPopup(getDeleteDialogPanel(), target);
    }

    private ConfirmationPanel getDeleteDialogPanel(){
        ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), createDeleteConfirmString()){

            private static final long serialVersionUID = 1L;
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
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
            OperationResult subresult = result.createSubresult(OPERATION_DELETE);
            subresult.addParam("Report", WebComponentUtil.getName(output));

            try {
                getReportManager().deleteReportOutput(output, subresult);
                subresult.recordSuccess();
            } catch (Exception e) {
                subresult.recordFatalError(
                        getString("PageCreatedReports.message.deleteSelectedConfirmedPerformed.fatalError", WebComponentUtil.getName(output), e.getMessage()), e);
                LOGGER.error("Cannot delete report {}. Reason: {}", WebComponentUtil.getName(output), e.getMessage(), e);
                continue;
            }
            //WebModelServiceUtils.deleteObject(ReportOutputType.class, output.getOid(), result, this);
        }
        result.computeStatusIfUnknown();

        getObjectListPanel().clearCache();
        getObjectListPanel().refreshTable(ReportOutputType.class, target);

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void deleteAllConfirmedPerformed(AjaxRequestTarget target) {
        //TODO - implement as background task
        warn("Not implemented yet, will be implemented as background task.");
        target.add(getFeedbackPanel());
    }

    private ObjectQuery appendTypeFilter(ObjectQuery query) {
        DropDownChoicePanel<String> typeSelect = (DropDownChoicePanel<String>) getMainForm().get(ID_REPORT_TYPE_SELECT);
        String typeRef = typeSelect == null ? reportTypeMal.get(getReportType()) : (String) typeSelect.getBaseFormComponent().getModelObject();
        S_AtomicFilterEntry q = getPrismContext().queryFor(ReportOutputType.class);

        S_AtomicFilterExit refF;
        if (StringUtils.isNotBlank(typeRef)) {
            Entry<String, String> typeRefFilter = reportTypeMal.entrySet().stream().filter(e -> e.getValue().equals(typeRef)).findFirst().get();
            if (typeRefFilter != null) {
                refF = q.item(ReportOutputType.F_REPORT_REF).ref(typeRefFilter.getKey());
            if (query == null) {
                query = refF.build();
            } else {
                query.addFilter(refF.buildFilter());
            }
            }
        }

        return query;
    }

    private InputStream createReport() {
        return createReport(currentReport, ajaxDownloadBehavior, this);
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
        } catch (IOException ex) {
            LOGGER.error("Report {} is not accessible.", WebComponentUtil.getName(report));
            result.recordPartialError("Report " + WebComponentUtil.getName(report) + " is not accessible.");
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

    private void downloadPerformed(AjaxRequestTarget target, ReportOutputType reports,
                                   AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {

        ajaxDownloadBehavior.initiate(target);
    }

    private String getReportFileName() {
        return getReportFileName(currentReport);
    }

    public static String getReportFileName(ReportOutputType currentReport){
        try {
            OperationResult result = new OperationResult(OPERATION_GET_REPORT_FILENAME);
//            ReportOutputType reportOutput = WebModelServiceUtils.loadObject(ReportOutputType.class, currentReport.getOid(), getPageBase(),
//                    null, result).asObjectable();
            String fileName = currentReport.getFilePath();
            if (fileName.contains("/")) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            return fileName;
        } catch (Exception ex){
            //nothing to do
        }
        return null;
    }

    @Override
    protected Class<ReportOutputType> getType(){
        return ReportOutputType.class;
    }
}
