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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DropDownMultiChoice;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportSearchDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportOutputDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
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

    private static final String BUTTON_DOWNLOAD_TEXT = "Download";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_CREATED_REPORTS_TABLE = "table";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH_TYPE = "searchType";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_FILTER_FILE_TYPE = "filetype";
    private static final String ID_FILTER_REPORT_TYPE = "reportType";

    private final String BUTTON_CAPTION_DOWNLOAD = createStringResource("pageCreatedReports.button.download").getString();

    private IModel<List<ReportOutputDto>> model;
    private IModel<ReportSearchDto> filterModel;

    public PageCreatedReports(){

        filterModel = new LoadableModel<ReportSearchDto>() {
            @Override
            protected ReportSearchDto load() {
                return loadReportFilterDto();
            }
        };

        model = new LoadableModel<List<ReportOutputDto>>() {

            @Override
            protected List<ReportOutputDto> load() {
                return loadModel();
            }
        };
        initLayout();
    }

    private ReportSearchDto loadReportFilterDto(){
        ReportSearchDto dto = new ReportSearchDto();
        return dto;
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

    //TODO - load real ReportType objects from repository
    private List<ReportOutputDto> loadModel(){
        List<ReportOutputDto> reportList = new ArrayList<ReportOutputDto>();

        ReportOutputDto reportOutputDto;
        for(int i = 1; i <= 5; i++){
            reportOutputDto = new ReportOutputDto(ReportDto.Type.USERS, "ReportName" + i, "Report description " + i,
                    "Author" + i, "January " + i + "., 2014");
            reportOutputDto.setFileType("PDF");
            reportList.add(reportOutputDto);
        }

        return reportList;
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
                new ListDataProvider<ReportOutputDto>(this, model), initColumns(ajaxDownloadBehavior));
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    //TODO - consider repairing model.getObject().get(0) hack - it will cause problems
    private void initSearchForm(){
        Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);

        TextField searchText = new TextField(ID_SEARCH_TEXT, new PropertyModel<String>(model.getObject().get(0),
                ReportOutputDto.F_TEXT));
        searchForm.add(searchText);

        IModel<Map<String, String>> options = new Model(null);
        DropDownMultiChoice searchType = new DropDownMultiChoice<ReportOutputDto.SearchType>(ID_SEARCH_TYPE,
                new PropertyModel<List<ReportOutputDto.SearchType>>(model.getObject().get(0), ReportOutputDto.F_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(ReportOutputDto.SearchType.class),
                new IChoiceRenderer<ReportOutputDto.SearchType>() {

                    @Override
                    public Object getDisplayValue(ReportOutputDto.SearchType object) {
                        return WebMiscUtil.createLocalizedModelForEnum(object, PageCreatedReports.this).getObject();
                    }

                    @Override
                    public String getIdValue(ReportOutputDto.SearchType object, int index) {
                        return Integer.toString(index);
                    }
                }, options);
                searchForm.add(searchType);

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
                new PropertyModel(filterModel, ReportSearchDto.F_FILE_TYPE),
                new AbstractReadOnlyModel<List<ExportType>>() {

                    @Override
                    public List<ExportType> getObject() {
                        return createFileTypeList();
                    }
                },
                new EnumChoiceRenderer(PageCreatedReports.this));
        filetypeSelect.add(createFilterAjaxBehaviour());
        filetypeSelect.setOutputMarkupId(true);

        if(filetypeSelect.getModel().getObject() == null){
            filetypeSelect.getModel().setObject(null);
        }
        searchForm.add(filetypeSelect);

        DropDownChoice reportTypeSelect = new DropDownChoice(ID_FILTER_REPORT_TYPE,
                new PropertyModel(filterModel, ReportSearchDto.F_REPORT_TYPE),
                new AbstractReadOnlyModel<List<ReportDto.Type>>() {

                    @Override
                    public List<ReportDto.Type> getObject() {
                        return createReportTypeList();
                    }
                }, new EnumChoiceRenderer(PageCreatedReports.this));
        reportTypeSelect.add(createFilterAjaxBehaviour());
        reportTypeSelect.setOutputMarkupId(true);

        if(reportTypeSelect.getModel().getObject() == null){
            reportTypeSelect.getModel().setObject(null);
        }
        searchForm.add(reportTypeSelect);
    }

    private List<ReportDto.Type> createReportTypeList(){
        List<ReportDto.Type> list = new ArrayList<ReportDto.Type>();
        Collections.addAll(list, ReportDto.Type.values());
        return list;
    }

    private List<ExportType> createFileTypeList(){
        List<ExportType> list = new ArrayList<ExportType>();
        Collections.addAll(list, ExportType.values());
        return list;
    }

    private AjaxFormComponentUpdatingBehavior createFilterAjaxBehaviour(){
        return new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                filterPerformed(target);
            }
        };
    }

    private List<IColumn<ReportOutputDto, String>> initColumns(final AjaxDownloadBehaviorFromStream ajaxDownloadBehavior){
        List<IColumn<ReportOutputDto, String>> columns = new ArrayList<IColumn<ReportOutputDto, String>>();

        IColumn column;

        column = new CheckBoxHeaderColumn();
        columns.add(column);

        column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.name"), null){

            @Override
            public IModel<Object> getDataModel(IModel<ReportOutputDto> rowModel){
                ReportOutputDto dto = rowModel.getObject();

                return (IModel)createStringResource(dto.getName());
            }
        };
        columns.add(column);

        column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.description"), null){

            @Override
            public IModel<Object> getDataModel(IModel<ReportOutputDto> rowModel){
                ReportOutputDto dto = rowModel.getObject();

                return (IModel)createStringResource(dto.getDescription());
            }
        };
        columns.add(column);

        column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.type"), null){

            @Override
            public IModel<Object> getDataModel(IModel<ReportOutputDto> rowModel){
                ReportOutputDto dto = rowModel.getObject();

                return (IModel)createStringResource(dto.getReportType().toString());
            }

        };
        columns.add(column);

        column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.author"), null){

            @Override
            public IModel<Object> getDataModel(IModel<ReportOutputDto> rowModel){
                ReportOutputDto dto = rowModel.getObject();

                return (IModel)createStringResource(dto.getAuthor());
            }
        };
        columns.add(column);

        column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.time"), null){

            @Override
            public IModel<Object> getDataModel(IModel<ReportOutputDto> rowModel){
                ReportOutputDto dto = rowModel.getObject();

                return (IModel)createStringResource(dto.getTime());
            }
        };
        columns.add(column);

        column = new PropertyColumn<ReportOutputDto, String>(createStringResource("pageCreatedReports.table.filetype"), null){

            @Override
            public IModel<Object> getDataModel(IModel<ReportOutputDto> rowModel){
                ReportOutputDto dto = rowModel.getObject();

                return (IModel)createStringResource(dto.getFileType());
            }
        };
        columns.add(column);

        column = new DoubleButtonColumn<ReportOutputDto>(new Model(), null){

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
            public void firstClicked(AjaxRequestTarget target, IModel<ReportOutputDto> model){
                downloadPerformed(target, model.getObject(), ajaxDownloadBehavior);
            }

            @Override
            public void secondClicked(AjaxRequestTarget target, IModel<ReportOutputDto> model){
                deletePerformed(target, model.getObject());
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
                //We do not need row inline menu
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
                        deleteAllPerformed(target);
                    }
                }));

        headerMenuItems.add(new InlineMenuItem(createStringResource("pageCreatedReports.inlineMenu.deleteSelected"), true,
                new HeaderMenuAction(this){

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form){
                        deleteSelectedPerformed(target);
                    }
                }));

        return headerMenuItems;
    }

    private List<ReportOutputDto> getSelectedData(AjaxRequestTarget target, ReportOutputDto item){
        List<ReportOutputDto> items;

        if(item != null){
            items = new ArrayList<ReportOutputDto>();
            items.add(item);
            return items;
        }

        items = WebMiscUtil.getSelectedData(getListTable());
        if(items.isEmpty()){
            warn(getString("pageCreatedReports.message.nothingSelected"));
            target.add(getFeedbackPanel());
        }
        return items;
    }

    private TablePanel getListTable() {
        return (TablePanel) get(createComponentPath(ID_MAIN_FORM, ID_CREATED_REPORTS_TABLE));
    }

    private void downloadPerformed(AjaxRequestTarget target, ReportOutputDto report,
                                   AjaxDownloadBehaviorFromStream ajaxDownloadBehavior){
        //TODO - run download from file
    }

    private void deletePerformed(AjaxRequestTarget targert, ReportOutputDto report){
        //TODO - delete current report output
    }

    private void deleteAllPerformed(AjaxRequestTarget target){
        //TODO - delete all objects of ReportOutputType
    }

    private void deleteSelectedPerformed(AjaxRequestTarget target){
        //TODO - delete Selected objects of ReportOutputType
    }

    private byte[] createReport(){
        //TODO - create report from ReportType
        return null;
    }

    private void filterPerformed(AjaxRequestTarget target){
        //TODO - perform some fancy search here
    }

    private void searchPerformed(AjaxRequestTarget target){
        //TODO - perform search based on search model
    }



}
