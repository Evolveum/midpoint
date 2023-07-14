/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.server.CasesTablePanel;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/casesAll", matchUrlForSecurity = "/admin/casesAll")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                        label = PageAdminCases.AUTH_CASES_ALL_LABEL,
                        description = PageAdminCases.AUTH_CASES_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_URL,
                        label = "PageCases.auth.cases.label",
                        description = "PageCases.auth.cases.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_VIEW_URL,
                        label = "PageCases.auth.cases.view.label",
                        description = "PageCases.auth.cases.view.description") })
@CollectionInstance(identifier = "allCases", applicableForType = CaseType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.cases.list", singularLabel = "ObjectType.case", icon = GuiStyleConstants.EVO_CASE_OBJECT_ICON))
public class PageCases extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CasesTablePanel.class);

    private static final String ID_TABLE = "table";

    private static final String DOT_CLASS = PageCases.class.getName() + ".";
    private static final String OPERATION_DELETE_CASE_OBJECT = DOT_CLASS + "deleteCaseObject";
    private static final String OPERATION_STOP_CASE_PROCESS = DOT_CLASS + "stopCaseProcess";

    public PageCases() {
        this(null);
    }

    public PageCases(PageParameters params) {
        super(params);
    }

    public PageCases(ObjectQuery predefinedQuery, PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CasesTablePanel tablePanel = new CasesTablePanel(ID_TABLE) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter getCasesFilter() {
                PageParameters pageParameters = getPageBase().getPageParameters();
                return pageParameters != null && pageParameters.isEmpty() ? PrismContext.get().queryFor(CaseType.class)
                        .item(CaseType.F_PARENT_REF).isNull()
                        .buildFilter() : super.getCasesFilter();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_CASES;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createActions();
            }

            @Override
            protected Collection<SelectorOptions<GetOperationOptions>> createOperationOptions() {
                return getPageBase().getOperationOptionsBuilder()
                        .item(CaseType.F_OBJECT_REF).resolve()
                        .item(CaseType.F_TARGET_REF).resolve()
                        .build();
            }
        };
        add(tablePanel);
    }

    private boolean isCaseInRowClosed(IModel<SelectableBeanImpl<CaseType>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null || rowModel.getObject().getValue() == null) {
            return false;
        }

        CaseType c = rowModel.getObject().getValue();
        return !CaseTypeUtil.isClosed(c);
    }

    private List<InlineMenuItem> createActions() {
        List<InlineMenuItem> menu = new ArrayList<>();

        menu.add(new ButtonInlineMenuItem(createStringResource("pageCases.button.stopProcess")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<CaseType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null && warnIfNoCaseSelected(target)) {
                            return;
                        }
                        if (getRowModel() == null) {
                            stopCaseProcessConfirmed(target);
                        } else {
                            stopCaseProcessConfirmed(target,
                                    Collections.singletonList(getRowModel().getObject().getValue()));
                        }
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return () -> isCaseInRowClosed(((ColumnMenuAction<SelectableBeanImpl<CaseType>>) getAction()).getRowModel());
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-fw " + GuiStyleConstants.CLASS_STOP_MENU_ITEM);
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return getTablePanel().getSelectedObjectsCount() > 0 ?
                        createStringResource("pageCases.button.stopProcess.multiple.confirmationMessage", getTablePanel().getSelectedObjectsCount()) :
                        createStringResource("pageCases.button.stopProcess.confirmationMessage");
            }

        });
        menu.add(new ButtonInlineMenuItem(createStringResource("pageCases.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<CaseType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null && warnIfNoCaseSelected(target)) {
                            return;
                        }
                        if (getRowModel() == null) {
                            deleteCaseObjectsConfirmed(target);
                        } else {
                            deleteCaseObjectsConfirmed(target,
                                    Collections.singletonList(getRowModel().getObject().getValue()));
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa-fw " + GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return getTablePanel().getSelectedObjectsCount() > 0 ?
                        createStringResource("pageCases.button.delete.multiple.confirmationMessage", getTablePanel().getSelectedObjectsCount()) :
                        createStringResource("pageCases.button.delete.confirmationMessage");
            }
        });

        return menu;
    }

    private void deleteCaseObjectsConfirmed(AjaxRequestTarget target) {
        deleteCaseObjectsConfirmed(target, getTablePanel().getSelectedRealObjects());
    }

    private void deleteCaseObjectsConfirmed(AjaxRequestTarget target, List<CaseType> casesToDelete) {
        if (casesToDelete == null) {
            return;
        }
        OperationResult result = new OperationResult(OPERATION_DELETE_CASE_OBJECT);
        casesToDelete.forEach(caseObject -> {
            WebModelServiceUtils.deleteObject(CaseType.class, caseObject.getOid(),
                    ExecuteChangeOptionsDto.createFromSystemConfiguration().createOptions(getPrismContext()),
                    result, PageCases.this);
        });
        result.computeStatusComposite();

        showResult(result);
        target.add(getFeedbackPanel());
        getTablePanel().refreshTable(target);
        getTablePanel().clearCache();
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target) {
        stopCaseProcessConfirmed(target, getTablePanel().getSelectedRealObjects());
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target, List<CaseType> casesToStop) {
        if (casesToStop == null) {
            return;
        }
        OperationResult result = new OperationResult(OPERATION_STOP_CASE_PROCESS);
        casesToStop.forEach(caseObject -> {
            Task task = createSimpleTask(OPERATION_STOP_CASE_PROCESS);
            try {
                getCaseService().cancelCase(caseObject.getOid(), task, result);
            } catch (Exception ex) {
                LOGGER.error("Couldn't stop case process: {}", ex.getLocalizedMessage());
                result.recordFatalError(createStringResource("PageCases.message.stopCaseProcessConfirmed.fatalError").getString(), ex);
            }
        });
        result.computeStatusComposite();

        showResult(result);
        target.add(getFeedbackPanel());
        getTablePanel().refreshTable(target);
        getTablePanel().clearCache();
    }

    private boolean warnIfNoCaseSelected(AjaxRequestTarget target) {
        if (CollectionUtils.isEmpty(getTablePanel().getSelectedRealObjects())) {
            warn(getString("PageCases.noCaseSelected"));
            target.add(getFeedbackPanel());
        }
        return CollectionUtils.isEmpty(getTablePanel().getSelectedRealObjects());
    }

    private CasesTablePanel getTablePanel() {
        return (CasesTablePanel) get(ID_TABLE);
    }

}
