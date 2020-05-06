/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.ContainerListDataProvider;
import com.evolveum.midpoint.web.page.admin.workflow.PageAttorneySelection;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by honchar
 */
public class CaseWorkItemsPanel extends BasePanel<CaseWorkItemType> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CaseWorkItemsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_POWER_DONOR_OBJECT = DOT_CLASS + "loadPowerDonorObject";
    private static final String OPERATION_COMPLETE_WORK_ITEM = DOT_CLASS + "completeWorkItem";

    private static final String ID_WORKITEMS_TABLE = "workitemsTable";

    public enum View {
        FULL_LIST,                // selectable, full information
        DASHBOARD,                 // not selectable, reduced info (on dashboard)
        ITEMS_FOR_PROCESS        // work items for a process
    }

    private View view;
    private  PageParameters pageParameters;

    public CaseWorkItemsPanel(String id, View view){
        super(id);
        this.view = view;
    }

    public CaseWorkItemsPanel(String id, View view, PageParameters pageParameters){
        super(id);
        this.view = view;
        this.pageParameters = pageParameters;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        ContainerableListPanel workItemsPanel = new ContainerableListPanel(ID_WORKITEMS_TABLE,
                UserProfileStorage.TableId.PAGE_CASE_WORK_ITEMS_PANEL) {
            @Override
            protected Class getType() {
                return CaseWorkItemType.class;
            }

            @Override
            protected PageStorage getPageStorage() {
                return CaseWorkItemsPanel.this.getPageBase().getSessionStorage().getWorkItemStorage();
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> initColumns() {
                return CaseWorkItemsPanel.this.initColumns();
            }

            @Override
            protected ObjectFilter getCustomFilter(){
                return getCaseWorkItemsFilter();
            }

            @Override
            protected Collection<SelectorOptions<GetOperationOptions>> getQueryOptions() {
                return CaseWorkItemsPanel.this.getPageBase().getOperationOptionsBuilder()
                        .item(AbstractWorkItemType.F_ASSIGNEE_REF).resolve()
                        .item(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF).resolve()
                        .item(PrismConstants.T_PARENT, CaseType.F_TARGET_REF).resolve()
                        .build();
            }

            @Override
            protected boolean hideFooterIfSinglePage(){
                return View.DASHBOARD.equals(view);
            }

            @Override
            protected boolean isSearchVisible(){
                return !View.DASHBOARD.equals(view);
            }

            @Override
            protected void setDefaultSorting(ContainerListDataProvider provider){
                provider.setSort(CaseWorkItemType.F_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
            }

        };
        workItemsPanel.setOutputMarkupId(true);
        add(workItemsPanel);
    }

    private List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> initColumns(){
        List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> columns = new ArrayList<>();

        if (View.FULL_LIST.equals(view)) {
            columns.add(new CheckBoxHeaderColumn<>());
        }
        columns.add(new IconColumn<PrismContainerValueWrapper<CaseWorkItemType>>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(CaseWorkItemType.COMPLEX_TYPE));
            }

        });
        columns.add(new LinkColumn<PrismContainerValueWrapper<CaseWorkItemType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                PolyStringType workitemName = ColumnUtils.unwrapRowModel(rowModel).getName();
                return Model.of(WebComponentUtil.getTranslatedPolyString(workitemName));
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                if (rowModel.getObject() == null || rowModel.getObject().getRealValue() == null){
                    return false;
                }
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                PageCaseWorkItem pageCaseWorkItem = new PageCaseWorkItem(rowModel.getObject() != null ? rowModel.getObject().getRealValue() : null, pageParameters);
                CaseWorkItemsPanel.this.getPageBase().navigateToNext(pageCaseWorkItem);
            }
        });

        columns.addAll(ColumnUtils.getDefaultWorkItemColumns(getPageBase(), View.FULL_LIST.equals(view)));
        if (View.FULL_LIST.equals(view)) {
            columns.add(new InlineMenuButtonColumn<>(createRowActions(), getPageBase()));
        }
        return columns;
    }

    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();

        menu.add(new ButtonInlineMenuItem(createStringResource("pageWorkItem.button.reject")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        workItemActionPerformed(getRowModel(), false, target);
                    }
                };
            }

            @Override
            public IModel<Boolean> getEnabled() {
                IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = ((ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>)getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getRealValue() != null){
                    CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                    return Model.of(!CaseTypeUtil.isClosed(CaseTypeUtil.getCase(workItem)));
                } else {
                    return super.getEnabled();
                }
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                return createStringResource("CaseWorkItemsPanel.confirmWorkItemsRejectAction");
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_NO_OBJECTS);
            }
        });
        menu.add(new ButtonInlineMenuItem(createStringResource("pageWorkItem.button.approve")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>() {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        workItemActionPerformed(getRowModel(), true, target);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
            }

            @Override
            public IModel<Boolean> getEnabled() {
                IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = ((ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>)getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getRealValue() != null){
                    CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                    return Model.of(!CaseTypeUtil.isClosed(CaseTypeUtil.getCase(workItem)));
                } else {
                    return super.getEnabled();
                }
            }

            @Override
            public IModel<String> getConfirmationMessageModel(){
                return createStringResource("CaseWorkItemsPanel.confirmWorkItemsApproveAction");
            }
        });

        return menu;
    }

    private void workItemActionPerformed(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel, boolean approved,
                                         AjaxRequestTarget target){
        List<PrismContainerValueWrapper<CaseWorkItemType>> selectedWorkItems = new ArrayList<>();
        if (rowModel == null) {
            ContainerableListPanel<CaseWorkItemType> tablePanel = getContainerableListPanel();
            selectedWorkItems.addAll(tablePanel.getProvider().getSelectedData());
        } else {
            selectedWorkItems.addAll(Arrays.asList(rowModel.getObject()));
        }

        if (selectedWorkItems.size() == 0){
            warn(getString("CaseWorkItemsPanel.noWorkItemIsSelected"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        Task task = CaseWorkItemsPanel.this.getPageBase().createSimpleTask(OPERATION_LOAD_POWER_DONOR_OBJECT);
        OperationResult result = new OperationResult(OPERATION_LOAD_POWER_DONOR_OBJECT);
        final PrismObject<UserType> powerDonor = StringUtils.isNotEmpty(getPowerDonorOidParameterValue()) ?
                WebModelServiceUtils.loadObject(UserType.class, getPowerDonorOidParameterValue(),
                        CaseWorkItemsPanel.this.getPageBase(), task, result) : null;
        OperationResult completeWorkItemResult = new OperationResult(OPERATION_COMPLETE_WORK_ITEM);
        selectedWorkItems.forEach(workItemToReject -> {
            WebComponentUtil.workItemApproveActionPerformed(target, workItemToReject.getRealValue(),
                    new AbstractWorkItemOutputType(getPrismContext()).outcome(ApprovalUtils.toUri(approved)),
                    null, powerDonor, approved,  completeWorkItemResult, CaseWorkItemsPanel.this.getPageBase());
        });

        WebComponentUtil.clearProviderCache(getContainerableListPanel().getProvider());

        getPageBase().showResult(completeWorkItemResult, true);
        target.add(getPageBase().getFeedbackPanel());
        target.add(getContainerableListPanel());

    }

    public ContainerableListPanel<CaseWorkItemType> getContainerableListPanel(){
        return (ContainerableListPanel<CaseWorkItemType>) get(ID_WORKITEMS_TABLE);
    }

    protected ObjectFilter getCaseWorkItemsFilter(){
        return null;
    }

    private String getPowerDonorOidParameterValue(){
        if (pageParameters != null && pageParameters.get(PageAttorneySelection.PARAMETER_DONOR_OID) != null){
            return pageParameters.get(PageAttorneySelection.PARAMETER_DONOR_OID).toString();
        }
        return null;
    }
}
