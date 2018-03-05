/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.component.RunReportPopupPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;


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

    public PageReports() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel table = new MainObjectListPanel<ReportType>(ID_REPORTS_TABLE, ReportType.class, UserProfileStorage.TableId.PAGE_REPORTS,
                null, this) {

            @Override
            protected IColumn<SelectableBean<ReportType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            public void objectDetailsPerformed(AjaxRequestTarget target, ReportType report) {
                if (report != null) {
                    PageReports.this.reportDetailsPerformed(target, report.getOid());
                }
            }

            @Override
            protected PrismObject<ReportType> getNewObjectListObject(){
                return (new ReportType()).asPrismObject();
            }

            @Override
            protected List<IColumn<SelectableBean<ReportType>, String>> createColumns() {
                return PageReports.this.initColumns();
            }

            @Override
            protected IColumn<SelectableBean<ReportType>, String> createActionsColumn() {
                return PageReports.this.createActionsColumn();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return new ArrayList<>();
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target) {
                navigateToNext(PageNewReport.class);
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

    }

    private List<IColumn<SelectableBean<ReportType>, String>> initColumns() {
        List<IColumn<SelectableBean<ReportType>, String>> columns = new ArrayList<IColumn<SelectableBean<ReportType>, String>>();

        IColumn column = new PropertyColumn(createStringResource("PageReports.table.description"), "value.description");
        columns.add(column);

        return columns;
    }

    private void reportDetailsPerformed(AjaxRequestTarget target, String oid) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, oid);
        navigateToNext(PageCreatedReports.class, params);
    }

    private IColumn<SelectableBean<ReportType>, String> createActionsColumn(){
        return new InlineMenuButtonColumn<SelectableBean<ReportType>>(createInlineMenu(), 2, this){
            @Override
            protected List<InlineMenuItem> getHeaderMenuItems() {
                return new ArrayList<>();
            }

            @Override
            protected int getHeaderNumberOfButtons(){
                return 0;
            }
        };
    }

    private List<InlineMenuItem> createInlineMenu(){
        List<InlineMenuItem> menu = new ArrayList<>();
        menu.add(new InlineMenuItem(createStringResource("PageReports.button.run"),
                new Model<Boolean>(true), new Model<Boolean>(true), false,
                new ColumnMenuAction<SelectableBean<ReportType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ReportType report = getRowModel().getObject().getValue();
                        runReportPerformed(target, report);
                    }
                }, 0,
                GuiStyleConstants.CLASS_START_MENU_ITEM,
                DoubleButtonColumn.BUTTON_COLOR_CLASS.INFO.toString()));

        menu.add(new InlineMenuItem(createStringResource("PageReports.button.configure"),
                new Model<Boolean>(true), new Model<Boolean>(true),
                false,
                new ColumnMenuAction<SelectableBean<ReportType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                            ReportType reportObject = getRowModel().getObject().getValue();
                            configurePerformed(target, reportObject);
                    }
                }, 1,
                GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
                DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString()));

        return menu;

    }

    protected void runReportPerformed(AjaxRequestTarget target, ReportType report) {

    	RunReportPopupPanel runReportPopupPanel = new RunReportPopupPanel(getMainPopupBodyId(), report) {

    		private static final long serialVersionUID = 1L;

			@Override
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

    private Table getReportTable() {
        return (Table) get(createComponentPath(ID_MAIN_FORM, ID_REPORTS_TABLE));
    }
}
