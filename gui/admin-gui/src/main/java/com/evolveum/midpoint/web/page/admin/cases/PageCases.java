/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.*;

/**
 * @author acope on 9/14/17.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/casesAll", matchUrlForSecurity = "/admin/casesAll")
        }, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                label = PageAdminCases.AUTH_CASES_ALL_LABEL,
                description = PageAdminCases.AUTH_CASES_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_URL,
                label = "PageCases.auth.cases.label",
                description = "PageCases.auth.cases.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_VIEW_URL,
                label = "PageCases.auth.casesAll.view.label",
                description = "PageCases.auth.casesAll.view.description")
})
public class PageCases extends PageAdminObjectList<CaseType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageCases.class);

    private static final String DOT_CLASS = PageCases.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCE_DISPLAY_NAME = DOT_CLASS + "loadReferenceDisplayName";
    private static final String OPERATION_DELETE_CASE_OBJECT = DOT_CLASS + "deleteCaseObject";
    private static final String OPERATION_STOP_CASE_PROCESS = DOT_CLASS + "stopCaseProcess";

    private static final long serialVersionUID = 1L;

    public static final String ID_MAIN_FORM = "mainForm";
    public static final String ID_CASES_TABLE = "table";

    public PageCases() {
        super();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
        LOGGER.trace("caseDetailsPerformed()");

        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
        navigateToNext(PageCase.class, pageParameters);
    }

    @Override
    protected List<IColumn<SelectableBean<CaseType>, String>> initColumns() {
        LOGGER.trace("initColumns()");

        return ColumnUtils.getDefaultCaseColumns(PageCases.this, false);
    }

    @Override
    protected Class getType(){
        return CaseType.class;
    }

    @Override
    protected UserProfileStorage.TableId getTableId(){
        return UserProfileStorage.TableId.TABLE_CASES;
    }


    @Override
    protected boolean isCreateNewObjectEnabled(){
        return false;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getQueryOptions(){
        return getOperationOptionsBuilder()
                .item(CaseType.F_OBJECT_REF).resolve()
                .item(CaseType.F_TARGET_REF).resolve()
                .build();
    }

    @Override
    protected void setDefaultSorting(BaseSortableDataProvider<SelectableBean<CaseType>> provider){
        provider.setSort(MetadataType.F_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
    }

    @Override
    protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
        return WebComponentUtil.createMetadataOrdering(sortParam, "createTimestamp", getPrismContext());
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();

        menu.add(new ButtonInlineMenuItem(createStringResource("pageCases.button.stopProcess")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<CaseType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (getRowModel() == null && warnIfNoCaseSelected(target)){
                            return;
                        }
                        if (getRowModel() == null) {
                            stopCaseProcessConfirmed(target);
                        } else {
                            stopCaseProcessConfirmed(target, Arrays.asList(getRowModel().getObject().getValue()));
                        }
                    }
                };
            }

            @Override
            public IModel<Boolean> getEnabled() {
                IModel<SelectableBeanImpl<CaseType>> rowModel = ((ColumnMenuAction<SelectableBeanImpl<CaseType>>)getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getValue() != null){
                    return Model.of(!CaseTypeUtil.isClosed(rowModel.getObject().getValue()));
                } else {
                    return super.getEnabled();
                }
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_STOP_MENU_ITEM);
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                return getObjectListPanel().getSelectedObjectsCount() > 0 ?
                        createStringResource("pageCases.button.stopProcess.multiple.confirmationMessage", getObjectListPanel().getSelectedObjectsCount()) :
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
                        if (getRowModel() == null && warnIfNoCaseSelected(target)){
                            return;
                        }
                        if (getRowModel() == null) {
                            deleteCaseObjectsConfirmed(target);
                        } else {
                            deleteCaseObjectsConfirmed(target, Arrays.asList(getRowModel().getObject().getValue()));
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }

            @Override
            public IModel<Boolean> getEnabled() {
                IModel<SelectableBeanImpl<CaseType>> rowModel = ((ColumnMenuAction<SelectableBeanImpl<CaseType>>)getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getValue() != null){
                    return Model.of(!CaseTypeUtil.isClosed(rowModel.getObject().getValue()));
                } else {
                    return super.getEnabled();
                }
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                return getObjectListPanel().getSelectedObjectsCount() > 0 ?
                        createStringResource("pageCases.button.delete.multiple.confirmationMessage", getObjectListPanel().getSelectedObjectsCount()) :
                        createStringResource("pageCases.button.delete.confirmationMessage");
            }

        });

        return menu;
    }

    private <O extends ObjectType> String getObjectRef(IModel<SelectableBeanImpl<CaseType>> caseModel) {
        CaseType caseModelObject = caseModel.getObject().getValue();
        if (caseModelObject == null || caseModelObject.getObjectRef() == null) {
            return "";
        }
        return WebComponentUtil.getEffectiveName(caseModelObject.getObjectRef(), AbstractRoleType.F_DISPLAY_NAME, PageCases.this,
                OPERATION_LOAD_REFERENCE_DISPLAY_NAME);
    }

    private void deleteCaseObjectsConfirmed(AjaxRequestTarget target){
        deleteCaseObjectsConfirmed(target, getObjectListPanel().getSelectedObjects());
    }

    private void deleteCaseObjectsConfirmed(AjaxRequestTarget target, List<CaseType> casesToDelete){
        if (casesToDelete == null){
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
        getObjectListPanel().refreshTable(CaseType.class, target);
        getObjectListPanel().clearCache();
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target){
        stopCaseProcessConfirmed(target, getObjectListPanel().getSelectedObjects());
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target, List<CaseType> casesToStop){
        if (casesToStop == null){
            return;
        }
        OperationResult result = new OperationResult(OPERATION_STOP_CASE_PROCESS);
        casesToStop.forEach(caseObject -> {
            Task task = createSimpleTask(OPERATION_STOP_CASE_PROCESS);
            try {
                getWorkflowService().cancelCase(caseObject.getOid(), task, result);
            } catch (Exception ex){
                LOGGER.error("Couldn't stop case process: {}", ex.getLocalizedMessage());
                result.recordFatalError(createStringResource("PageCases.message.stopCaseProcessConfirmed.fatalError").getString(), ex);
            }
        });
        result.computeStatusComposite();

        showResult(result);
        target.add(getFeedbackPanel());
        getObjectListPanel().refreshTable(CaseType.class, target);
        getObjectListPanel().clearCache();
    }

    private boolean warnIfNoCaseSelected(AjaxRequestTarget target){
        if (CollectionUtils.isEmpty(getObjectListPanel().getSelectedObjects())){
            warn(getString("PageCases.noCaseSelected"));
            target.add(getFeedbackPanel());
        }
        return CollectionUtils.isEmpty(getObjectListPanel().getSelectedObjects());
    }
}
