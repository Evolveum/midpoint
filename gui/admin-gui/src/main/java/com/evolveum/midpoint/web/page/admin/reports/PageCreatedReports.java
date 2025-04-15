/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import com.evolveum.midpoint.web.page.admin.reports.component.CreatedReportFragment;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.configuration.PageTraceView;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDeleteDialogDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/reports/created")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_CREATED_REPORTS_URL,
                label = "PageCreatedReports.auth.createdReports.label",
                description = "PageCreatedReports.auth.createdReports.description") })
public class PageCreatedReports extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageCreatedReports.class);

    private static final String DOT_CLASS = PageCreatedReports.class.getName() + ".";
    private static final String OPERATION_DELETE = DOT_CLASS + "deleteReportOutput";
    private static final String OPERATION_DOWNLOAD_REPORT = DOT_CLASS + "downloadReport";
    private static final String OPERATION_GET_REPORT_FILENAME = DOT_CLASS + "getReportFilename";
    private static final String OPERATION_LOAD_REPORTS = DOT_CLASS + "loadReports";
    private static final String OPERATION_LOAD_REPORT_TYPE_NAME = DOT_CLASS + "loadReportTypeName";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_HEADER = "tableHeader";

    private static final Map<FileFormatTypeType, String> REPORT_EXPORT_TYPE_MAP = new HashMap<>();

    private final IModel<ReportDeleteDialogDto> deleteModel = new Model<>();

    private ReportDataType currentReport;

    private Map<String, String> reportTypeMal = new HashMap<>();

    private AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = null;

    private IModel<String> reportType;

    static {
        REPORT_EXPORT_TYPE_MAP.put(FileFormatTypeType.CSV, "text/csv; charset=UTF-8");
        REPORT_EXPORT_TYPE_MAP.put(FileFormatTypeType.HTML, "text/html; charset=UTF-8");

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

    private String getReportType() {
        StringValue param = getPage().getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        if (param != null) {
            return param.toString();
        }
        return "undefined";
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ReportDataType> table = new MainObjectListPanel<>(ID_TABLE, ReportDataType.class) {

            @Override
            protected Component createHeader(String headerId) {
                String reportName = reportTypeMal.get(getReportType());
                reportType = new Model<>();
                reportType.setObject(reportName);

                List<String> values = new ArrayList<>(reportTypeMal.values());
                return new CreatedReportFragment(headerId, ID_TABLE_HEADER, PageCreatedReports.this, getSearchModel(), reportType, values) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void searchPerformed(AjaxRequestTarget target) {
                        refreshTable(target);
                    }

                    @Override
                    protected void onReportTypeUpdate(AjaxRequestTarget target) {
                        refreshTable(target);
                    }
                };
            }

            @Override
            protected void objectDetailsPerformed(ReportDataType object) {
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_CREATED_REPORTS_PANEL;
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ReportDataType>> createProvider() {
                return createSelectableBeanObjectDataProvider(() -> appendTypeFilter(),
                        (sortParam) -> PageCreatedReports.this.createCustomOrdering(sortParam));
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return initInlineMenu();
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<ReportDataType>> rowModel) {
                return false;
            }

            @Override
            protected List<IColumn<SelectableBean<ReportDataType>, String>> createDefaultColumns() {
                return PageCreatedReports.this.initColumns();
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

        ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {

            private static final long serialVersionUID = 1L;

            @Override
            protected InputStream getInputStream() {
                return createReport();
            }

            @Override
            public String getFileName() {
                return getReportFileName();
            }
        };

        mainForm.add(ajaxDownloadBehavior);
    }

    private MainObjectListPanel<ReportDataType> getObjectListPanel() {
        return (MainObjectListPanel<ReportDataType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {

        if (sortParam != null && sortParam.getProperty() != null) {
            OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
            if (sortParam.getProperty().equals("metadata/createTimestamp")) {
                return Collections.singletonList(
                        getPrismContext().queryFactory().createOrdering(
                                ItemPath.create(ReportDataType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), order));
            }
            return Collections.singletonList(
                    getPrismContext().queryFactory().createOrdering(
                            ItemPath.create(new QName(SchemaConstantsGenerated.NS_COMMON, sortParam.getProperty())), order));

        } else {
            return Collections.emptyList();
        }
    }

    //TODO - commented until FileType property will be available in ReportDataType

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    private List<IColumn<SelectableBean<ReportDataType>, String>> initColumns() {
        List<IColumn<SelectableBean<ReportDataType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<ReportDataType>, String> column = new AbstractColumn<SelectableBean<ReportDataType>, String>(
                createStringResource("pageCreatedReports.table.time"),
                "metadata/createTimestamp") {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ReportDataType>>> cellItem,
                    String componentId, final IModel<SelectableBean<ReportDataType>> rowModel) {
                cellItem.add(new DateLabelComponent(componentId, new IModel<Date>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public Date getObject() {
                        ReportDataType object = rowModel.getObject().getValue();
                        MetadataType metadata = object != null ? object.getMetadata() : null;
                        if (metadata == null) {
                            return null;
                        }

                        return XmlTypeConverter.toDate(metadata.getCreateTimestamp());
                    }
                }, WebComponentUtil.getShortDateTimeFormat(PageCreatedReports.this)));
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("pageCreatedReports.table.description"),
                SelectableBeanImpl.F_VALUE + "." + ReportDataType.F_DESCRIPTION.getLocalPart());
        columns.add(column);

        column = new EnumPropertyColumn(createStringResource("pageCreatedReports.table.fileFormat"),
                SelectableBeanImpl.F_VALUE + "." + ReportDataType.F_FILE_FORMAT.getLocalPart()){
            @Override
            protected String translate(Enum en) {
                return createStringResource(en).getString();
            }
        };
        columns.add(column);

        column = new ObjectReferenceColumn<>(createStringResource("pageCreatedReports.table.type"),
                SelectableBeanImpl.F_VALUE + "." + ReportDataType.F_REPORT_REF.getLocalPart()) {
            @Override
            public IModel<List<ObjectReferenceType>> extractDataModel(IModel<SelectableBean<ReportDataType>> rowModel) {
                if (isNullValue(rowModel)) {
                    return Model.ofList(Collections.emptyList());
                }
                SelectableBean<ReportDataType> bean = rowModel.getObject();
                ObjectReferenceType reportRef = bean.getValue().getReportRef();
                resolveReportTypeName(reportRef);
                return Model.ofList(Collections.singletonList(reportRef));
            }
        };
        columns.add(column);

        return columns;
    }

    private boolean isNullValue(IModel<SelectableBean<ReportDataType>> rowModel) {
        return rowModel == null || rowModel.getObject() == null || rowModel.getObject().getValue() == null;
    }

    private void resolveReportTypeName(ObjectReferenceType reportRef) {
        if (reportRef == null) {
            return;
        }
        if (reportRef.getTargetName() != null && StringUtils.isNotEmpty(reportRef.getTargetName().getOrig())) {
            return;
        }
        Task task = createSimpleTask(OPERATION_LOAD_REPORT_TYPE_NAME);
        OperationResult result = task.getResult();
        WebModelServiceUtils.resolveReferenceName(reportRef, PageCreatedReports.this, task, result);
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
                return new ColumnMenuAction<SelectableBeanImpl<ReportDataType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ReportDataType report = null;
                        if (getRowModel() != null) {
                            SelectableBeanImpl<ReportDataType> rowDto = getRowModel().getObject();
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
                return new ColumnMenuAction<SelectableBeanImpl<ReportDataType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ReportDataType> rowDto = getRowModel().getObject();
                        currentReport = rowDto.getValue();
                        downloadPerformed(target, ajaxDownloadBehavior);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-download");
            }

            @Override
            public boolean isHeaderMenuItem() {
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
                return new ColumnMenuAction<SelectableBeanImpl<ReportDataType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBeanImpl<ReportDataType> rowDto = getRowModel().getObject();
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
        if (isNullValue((IModel<SelectableBean<ReportDataType>>) rowModel)) {
            return false;
        }
        SelectableBean<ReportDataType> row = (SelectableBean<ReportDataType>) rowModel.getObject();
        return ObjectTypeUtil.hasArchetypeRef(row.getValue(), SystemObjectsType.ARCHETYPE_TRACE.value());
    }

    private IModel<String> createDeleteConfirmString() {
        return new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                ReportDeleteDialogDto dto = deleteModel.getObject();

                switch (dto.getOperation()) {
                    case DELETE_SINGLE:
                        ReportDataType report = dto.getObjects().get(0);
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

    private List<ReportDataType> getSelectedData() {
        return getObjectListPanel().getSelectedRealObjects();
    }

    private ConfirmationPanel getDeleteDialogPanel() {
        ConfirmationPanel dialog = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(), createDeleteConfirmString()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                ReportDeleteDialogDto dto = deleteModel.getObject();
                switch (dto.getOperation()) {
                    case DELETE_SINGLE:
                        deleteSelectedConfirmedPerformed(target,
                                Collections.singletonList(dto.getObjects().get(0)));
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

    private void deleteSelectedPerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op, ReportDataType single) {
        List<ReportDataType> selected = getSelectedData();

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

    private void deleteSelectedConfirmedPerformed(AjaxRequestTarget target, List<ReportDataType> objects) {
        OperationResult result = new OperationResult(OPERATION_DELETE);
        Task task = getPageBase().createSimpleTask(OPERATION_DELETE);

        for (ReportDataType data : objects) {
            OperationResult subresult = result.createSubresult(OPERATION_DELETE);
            subresult.addParam("Report", WebComponentUtil.getName(data));

            try {
                getReportManager().deleteReportData(data, task, subresult);
                subresult.recordSuccess();
            } catch (Exception e) {
                subresult.recordFatalError(
                        getString("PageCreatedReports.message.deleteSelectedConfirmedPerformed.fatalError", WebComponentUtil.getName(data), e.getMessage()), e);
                LOGGER.error("Cannot delete report {}. Reason: {}", WebComponentUtil.getName(data), e.getMessage(), e);
            }
        }
        result.computeStatusIfUnknown();

        getObjectListPanel().clearCache();
        getObjectListPanel().refreshTable(target);

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void deleteAllConfirmedPerformed(AjaxRequestTarget target) {
        //TODO - implement as background task
        warn("Not implemented yet, will be implemented as background task.");
        target.add(getFeedbackPanel());
    }

    private ObjectQuery appendTypeFilter() {
        String typeRef = reportType == null ? reportTypeMal.get(getReportType()) : reportType.getObject();
        S_FilterEntry q = getPrismContext().queryFor(ReportDataType.class);

        S_FilterExit refF;
        if (StringUtils.isNotBlank(typeRef)) {
            Entry<String, String> typeRefFilter = reportTypeMal.entrySet().stream().filter(e -> e.getValue().equals(typeRef)).findFirst().get();
            if (typeRefFilter != null) {
                refF = q.item(ReportDataType.F_REPORT_REF).ref(typeRefFilter.getKey());
                    return refF.build();
            }
        }
        return null;
    }

    private MidpointForm getMainForm() {
        return (MidpointForm) get(ID_MAIN_FORM);
    }

    private InputStream createReport() {
        return createReport(currentReport, ajaxDownloadBehavior, this);
    }

    public static InputStream createReport(ReportDataType report, AjaxDownloadBehaviorFromStream ajaxDownloadBehaviorFromStream, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_DOWNLOAD_REPORT);
        ReportManager reportManager = pageBase.getReportManager();

        if (report == null) {
            return null;
        }

        String contentType = REPORT_EXPORT_TYPE_MAP.get(report.getFileFormat());
        if (StringUtils.isEmpty(contentType)) {
            contentType = "multipart/mixed; charset=UTF-8";
        }
        ajaxDownloadBehaviorFromStream.setContentType(contentType);

        InputStream input = null;
        try {
            input = reportManager.getReportDataStream(report.getOid(), result);
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

    private void downloadPerformed(
            AjaxRequestTarget target, AjaxDownloadBehaviorFromStream ajaxDownloadBehavior) {
        ajaxDownloadBehavior.initiate(target);
    }

    private String getReportFileName() {
        return getReportFileName(currentReport);
    }

    public static String getReportFileName(ReportDataType currentReport) {
        try {
            String filePath = currentReport.getFilePath();
            if (filePath != null) {
                var fileName = new File(filePath).getName();
                if (StringUtils.isNotEmpty(fileName)) {
                    return fileName;
                }
            }
        } catch (RuntimeException ex) {
            // ignored
        }
        return "report"; // A fallback - this should not really occur
    }
}
