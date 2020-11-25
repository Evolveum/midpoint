/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.reports.component.ImportReportPopupPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.RunReportPopupPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
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
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_ALL_URL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_REPORTS_URL,
                label = "PageReports.auth.reports.label",
                description = "PageReports.auth.reports.description")})
public class PageReports extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    private static final String DOT_CLASS = PageReports.class.getName() + ".";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runReport";

    public PageReports() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ReportType> table = new MainObjectListPanel<ReportType>(ID_TABLE, ReportType.class) {
            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ReportType reportType) {
                PageParameters pageParameters = new PageParameters();
                pageParameters.add(OnePageParameterEncoder.PARAMETER, reportType.getOid());
                if (reportType.getJasper() != null) {
                    navigateToNext(PageJasperReport.class, pageParameters);
                } else {
                    navigateToNext(PageReport.class, pageParameters);
                }
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_REPORTS;
            }

            @Override
            protected List<IColumn<SelectableBean<ReportType>, String>> createDefaultColumns() {
                return ColumnUtils.getDefaultObjectColumns();
            }

            @Override
            protected IColumn<SelectableBean<ReportType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return PageReports.this.createInlineMenu();
            }

            @Override
            protected boolean isCreateNewObjectEnabled() {
                return false;
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

    }

    private List<InlineMenuItem> createInlineMenu(){
        List<InlineMenuItem> menu = new ArrayList<>();
        ButtonInlineMenuItem runButton = new ButtonInlineMenuItem(createStringResource("PageReports.button.run")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ReportType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ReportType report = getRowModel().getObject().getValue();
                        runReportPerformed(target, report);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_START_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };

        runButton.setVisibilityChecker((rowModel, isHeader) -> !isImportReport((IModel<SelectableBean<ReportType>>)rowModel));

        menu.add(runButton);

        ButtonInlineMenuItem importButton = new ButtonInlineMenuItem(createStringResource("PageReports.button.import")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ReportType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ReportType report = getRowModel().getObject().getValue();
                        importReportPerformed(target, report);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_UPLOAD);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };

        importButton.setVisibilityChecker((rowModel, isHeader) -> isImportReport((IModel<SelectableBean<ReportType>>)rowModel));

        menu.add(importButton);
        menu.add(new ButtonInlineMenuItem(createStringResource("PageReports.button.configure")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ReportType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ReportType reportObject = getRowModel().getObject().getValue();
                        configurePerformed(target, reportObject);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }
        });
        menu.add(new ButtonInlineMenuItem(createStringResource("PageReports.button.showOutput")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ReportType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ReportType reportObject = getRowModel().getObject().getValue();
                        PageParameters pageParameters = new PageParameters();
                        pageParameters.add(OnePageParameterEncoder.PARAMETER, reportObject.getOid());
                        navigateToNext(PageCreatedReports.class, pageParameters);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder("fa fa-files-o");
            }

            @Override
            public boolean isHeaderMenuItem(){
                return false;
            }
        });
        return menu;
    }

    private boolean isImportReport(IModel<SelectableBean<ReportType>> rowModel) {
        ReportBehaviorType behavior = rowModel.getObject().getValue().getBehavior();
        return behavior != null && DirectionTypeType.IMPORT.equals(behavior.getDirection());
    }

    private void importReportPerformed(AjaxRequestTarget target, ReportType report) {
        ImportReportPopupPanel importReportPopupPanel = new ImportReportPopupPanel(getMainPopupBodyId(), report) {

            private static final long serialVersionUID = 1L;

            protected void importConfirmPerformed(AjaxRequestTarget target, ReportDataType reportImportData) {
                PageReports.this.importConfirmPerformed(target, report, reportImportData);
                hideMainPopup(target);

            }
        };
        showMainPopup(importReportPopupPanel, target);
    }

    private void importConfirmPerformed(AjaxRequestTarget target, ReportType reportType, ReportDataType reportImportData) {
        OperationResult result = new OperationResult(OPERATION_RUN_REPORT);
        Task task = createSimpleTask(OPERATION_RUN_REPORT);

        try {
            getReportManager().importReport(reportType.asPrismObject(), reportImportData.asPrismObject(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType, PrismContainer<ReportParameterType> reportParam) {
        OperationResult result = new OperationResult(OPERATION_RUN_REPORT);
        Task task = createSimpleTask(OPERATION_RUN_REPORT);

        try {
            getReportManager().runReport(reportType.asPrismObject(), reportParam, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }

    protected void runReportPerformed(AjaxRequestTarget target, ReportType report) {

        if(report.getJasper() == null) {
            runConfirmPerformed(target, report, null);
            return;
        }

        RunReportPopupPanel runReportPopupPanel = new RunReportPopupPanel(getMainPopupBodyId(), report) {

            private static final long serialVersionUID = 1L;

            protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType, PrismContainer<ReportParameterType> reportParam) {
                PageReports.this.runConfirmPerformed(target, reportType, reportParam);
                hideMainPopup(target);

            }
        };
        showMainPopup(runReportPopupPanel, target);

    }

    private void configurePerformed(AjaxRequestTarget target, ReportType report) {
        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, report.getOid());
        navigateToNext(PageJasperReport.class, params);
    }
}
