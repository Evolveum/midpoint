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

import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DropDownMultiChoice;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDeleteDialogDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportOutputDto;
import com.evolveum.midpoint.web.session.ReportsStorage;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportOutputType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/reports/created", action = {
        PageAdminReports.AUTHORIZATION_REPORTS_ALL,
        AuthorizationConstants.NS_AUTHORIZATION + "#createdReports"})
public class PageCreatedReports extends PageAdminReports {

    private static final Trace LOGGER = TraceManager.getTrace(PageCreatedReports.class);

    private static final String DOT_CLASS = PageCreatedReports.class.getName() + ".";
    private static final String OPERATION_DELETE = DOT_CLASS + "deleteReportOutput";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CREATED_REPORTS_TABLE = "table";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_FILTER_FILE_TYPE = "filetype";
    private static final String ID_CONFIRM_DELETE = "confirmDeletePopup";

    private final String BUTTON_CAPTION_DOWNLOAD = createStringResource("pageCreatedReports.button.download").getString();

    private IModel<ReportOutputDto> filterModel;
    private IModel<ReportDeleteDialogDto> deleteModel = new Model<ReportDeleteDialogDto>();

    public PageCreatedReports(){

        filterModel = new LoadableModel<ReportOutputDto>() {
            @Override
            protected ReportOutputDto load() {
                ReportsStorage storage = getSessionStorage().getReports();
                ReportOutputDto dto = storage.getReportsSearch();

                if(dto == null){
                    dto = new ReportOutputDto();
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

    private void initLayout(){
        initSearchForm();

        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior = new AjaxDownloadBehaviorFromStream() {

            @Override
            protected byte[] initStream() {
                return createReport();
            }
        };
        ajaxDownloadBehavior.setContentType("application/pdf; charset=UTF-8");
        mainForm.add(ajaxDownloadBehavior);

        TablePanel table = new TablePanel<ReportOutputDto>(ID_CREATED_REPORTS_TABLE,
                new ObjectDataProvider(PageCreatedReports.this, ReportOutputType.class), initColumns(ajaxDownloadBehavior));
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);

        add(new ConfirmationDialog(ID_CONFIRM_DELETE, createStringResource("pageCreatedReports.dialog.title.confirmDelete"),
                createDeleteConfirmString()){

            @Override
            public void yesPerformed(AjaxRequestTarget target){
                close(target);

                ReportDeleteDialogDto dto = deleteModel.getObject();
                switch(dto.getOperation()){
                    case DELETE_SINGLE:
                        deleteSingleConfirmedPerformed(target, dto.getObjects().get(0));
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

    private void initSearchForm(){
        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);

        TextField searchText = new TextField(ID_SEARCH_TEXT, new PropertyModel<String>(filterModel,
                ReportOutputDto.F_TEXT));
        searchForm.add(searchText);

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON,
                createStringResource("pageCreatedReports.button.searchButton")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form){
                searchPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form){
                target.add(getFeedbackPanel());
            }
        };
        searchForm.add(searchButton);

        DropDownChoice filetypeSelect = new DropDownChoice(ID_FILTER_FILE_TYPE,
                new PropertyModel(filterModel, ReportOutputDto.F_FILE_TYPE),
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
    }

    private List<ExportType> createFileTypeList(){
        List<ExportType> list = new ArrayList<ExportType>();
        Collections.addAll(list, ExportType.values());
        return list;
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
    private List<IColumn<ReportOutputDto, String>> initColumns(final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior){
        List<IColumn<ReportOutputDto, String>> columns = new ArrayList<IColumn<ReportOutputDto, String>>();

        IColumn column;

        column = new CheckBoxHeaderColumn();
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageCreatedReports.table.name"), "value.name");
        columns.add(column);

        column = new PropertyColumn(createStringResource("pageCreatedReports.table.description"), "value.description");
        columns.add(column);

        column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.time"), "value.metadata.createTimestamp", "value.metadata.createTimestamp");
        columns.add(column);

        //column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.filetype"), "value.reportRef");
        //columns.add(column);

        column = new DoubleButtonColumn<SelectableBean<ReportOutputType>>(new Model(), null){

            @Override
            public String getFirstCap(){
                return BUTTON_CAPTION_DOWNLOAD;
            }

            @Override
            public String getSecondCap(){
                return "";
            }

            @Override
            public String getFirstColorCssClass(){
                return BUTTON_COLOR_CLASS.PRIMARY.toString();
            }

            @Override
            public String getSecondColorCssClass(){
                return BUTTON_COLOR_CLASS.DANGER.toString() + " glyphicon glyphicon-trash";
            }

            @Override
            public void firstClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportOutputType>> model){
                downloadPerformed(target, model.getObject().getValue(), ajaxDownloadBehavior);
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<SelectableBean<ReportOutputType>> model){
                deleteSinglePerformed(target, ReportDeleteDialogDto.Operation.DELETE_SINGLE, model);
            }

            @Override
            public String getSecondSizeCssClass(){
                return BUTTON_SIZE_CLASS.SMALL.toString();
            }
        };
        columns.add(column);

        column = new InlineMenuHeaderColumn<InlineMenuable>(initInlineMenu()){

            @Override
            public void populateItem(Item<ICellPopulator<InlineMenuable>> cellItem, String componentId,
                                     IModel<InlineMenuable> rowModel) {
                cellItem.add(new Label(componentId));
            }
        };
        columns.add(column);

        return columns;
    }

    private List<InlineMenuItem> initInlineMenu(){
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageCreatedReports.inlineMenu.deleteAll"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        deleteAllPerformed(target, ReportDeleteDialogDto.Operation.DELETE_ALL);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageCreatedReports.inlineMenu.deleteSelected"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        deleteSelectedPerformed(target, ReportDeleteDialogDto.Operation.DELETE_SELECTED);
                    }
                }));

        return headerMenuItems;
    }

    private IModel<String> createDeleteConfirmString(){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ReportDeleteDialogDto dto = deleteModel.getObject();

                switch (dto.getOperation()){
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

    private List<ReportOutputType> getSelectedData(){
        ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType> provider = getReportDataProvider();

        List<SelectableBean<ReportOutputType>> rows = provider.getAvailableData();
        List<ReportOutputType> selected = new ArrayList<ReportOutputType>();

        for(SelectableBean<ReportOutputType> row: rows){
            if(row.isSelected()){
                selected.add(row.getValue());
            }
        }

        return selected;
    }

    private ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType> getReportDataProvider(){
        DataTable table = getReportOutputTable().getDataTable();
        return (ObjectDataProvider<SelectableBean<ReportOutputType>, ReportOutputType>) table.getDataProvider();
    }

    private TablePanel getReportOutputTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_CREATED_REPORTS_TABLE));
    }

    private ObjectDataProvider getTableDataProvider(){
        TablePanel tablePanel = getReportOutputTable();
        DataTable table = tablePanel.getDataTable();
        return (ObjectDataProvider) table.getDataProvider();
    }

    private void deleteSinglePerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op,
                                       IModel<SelectableBean<ReportOutputType>> output){

        List<ReportOutputType> selected = new ArrayList<ReportOutputType>();
        selected.add(output.getObject().getValue());

        ReportDeleteDialogDto dto = new ReportDeleteDialogDto(op, selected);
        deleteModel.setObject(dto);

        ModalWindow dialog = (ModalWindow) get(ID_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteAllPerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op){
        ReportDeleteDialogDto dto = new ReportDeleteDialogDto(op, null);
        deleteModel.setObject(dto);

        ModalWindow dialog = (ModalWindow) get(ID_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteSelectedPerformed(AjaxRequestTarget target, ReportDeleteDialogDto.Operation op){
        List<ReportOutputType> selected = getSelectedData();

        if(selected.isEmpty()){
            return;
        }

        ReportDeleteDialogDto dto = new ReportDeleteDialogDto(op, selected);
        deleteModel.setObject(dto);

        ModalWindow dialog = (ModalWindow) get(ID_CONFIRM_DELETE);
        dialog.show(target);
    }

    private void deleteSingleConfirmedPerformed(AjaxRequestTarget target, ReportOutputType object){
        OperationResult result = new OperationResult(OPERATION_DELETE);

        WebModelUtils.deleteObject(ReportOutputType.class, object.getOid(), result, this);

        result.computeStatusIfUnknown();

        ObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getReportOutputTable());
        target.add(getFeedbackPanel());
    }

    private void deleteSelectedConfirmedPerformed(AjaxRequestTarget target, List<ReportOutputType> objects){
        OperationResult result = new OperationResult(OPERATION_DELETE);

        for(ReportOutputType output: objects){
            WebModelUtils.deleteObject(ReportOutputType.class, output.getOid(), result, this);
        }
        result.computeStatusIfUnknown();

        ObjectDataProvider provider = getTableDataProvider();
        provider.clearCache();

        showResult(result);
        target.add(getReportOutputTable());
        target.add(getFeedbackPanel());
    }

    private void deleteAllConfirmedPerformed(AjaxRequestTarget target){
        //TODO - implement as background task
        warn("Not implemented yet, will be implemented as background task.");
        target.add(getFeedbackPanel());
    }

    private ObjectQuery createQuery(){
        ReportOutputDto dto = filterModel.getObject();
        ObjectQuery query = null;

        if(StringUtils.isEmpty(dto.getText())){
            return null;
        }

        try{
            PolyStringNormalizer normalizer = getPrismContext().getDefaultPolyStringNormalizer();
            String normalizedString = normalizer.normalize(dto.getText());

            SubstringFilter substring = SubstringFilter.createSubstring(ReportOutputType.F_NAME, ReportOutputType.class,
                    getPrismContext(), PolyStringNormMatchingRule.NAME, normalizedString);

            query = ObjectQuery.createObjectQuery(substring);

        } catch(Exception e){
            error(getString("pageCreatedReports.message.queryError") + " " + e.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", e);
        }

        return query;
    }

    private byte[] createReport(){
        //TODO - create report from ReportType
        return null;
    }

    private void fileTypeFilterPerformed(AjaxRequestTarget target){
        //TODO - perform filtering based on file type - need to wait for schema update (ReportOutputType)
    }

    private void searchPerformed(AjaxRequestTarget target){
        ObjectQuery query = createQuery();
        target.add(getFeedbackPanel());

        TablePanel panel = getReportOutputTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        ReportsStorage storage = getSessionStorage().getReports();
        storage.setReportsSearch(filterModel.getObject());
        panel.setCurrentPage(storage.getReportsPaging());

        target.add(panel);
    }

    private void downloadPerformed(AjaxRequestTarget target, ReportOutputType report,
                                   AjaxDownloadBehaviorFromStream ajaxDownloadBehavior){

        //TODO - run download from file
    }



}
