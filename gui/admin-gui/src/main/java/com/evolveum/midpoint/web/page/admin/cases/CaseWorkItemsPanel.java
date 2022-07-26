/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.component.util.ContainerListDataProvider;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.workflow.PageAttorneySelection;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

    private final View view;

    private PageParameters pageParameters;
    private ContainerPanelConfigurationType config;

    public CaseWorkItemsPanel(String id, View view) {
        super(id);
        this.view = view;
    }
    public CaseWorkItemsPanel(String id, View view, ContainerPanelConfigurationType configurationType) {
        super(id);
        this.view = view;
        this.config = configurationType;
    }


    public CaseWorkItemsPanel(String id, View view, PageParameters pageParameters) {
        super(id);
        this.view = view;
        this.pageParameters = pageParameters;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        ContainerableListPanel workItemsPanel =
                new ContainerableListPanel<CaseWorkItemType, PrismContainerValueWrapper<CaseWorkItemType>>(ID_WORKITEMS_TABLE, CaseWorkItemType.class, null, config) {

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> createDefaultColumns() {
                        return CaseWorkItemsPanel.this.createDefaultColumns();
                    }

                    @Override
                    protected List<InlineMenuItem> createInlineMenu() {
                        if (View.FULL_LIST.equals(view)) {
                            return createRowActions();
                        }
                        return null;
                    }

                    @Override
                    protected ISelectableDataProvider<CaseWorkItemType, PrismContainerValueWrapper<CaseWorkItemType>> createProvider() {
                        return CaseWorkItemsPanel.this.createProvider(getSearchModel());
                    }

                    @Override
                    public List<CaseWorkItemType> getSelectedRealObjects() {
                        return getSelectedObjects().stream().map(o -> o.getRealValue()).collect(Collectors.toList());
                    }

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.PAGE_CASE_WORK_ITEMS_PANEL;
                    }

                    @Override
                    protected boolean isPagingVisible() {
                        return View.DASHBOARD != view;
                    }

                    @Override
                    protected boolean isHeaderVisible() {
                        return !View.DASHBOARD.equals(view);
                    }

                    @Override
                    protected IColumn createCheckboxColumn() {
                        return CaseWorkItemsPanel.this.createCheckboxColumn();
                    }

                    @Override
                    protected IColumn createIconColumn() {
                        return CaseWorkItemsPanel.this.createIconColumn();
                    }

                    @Override
                    protected IColumn createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
                        return CaseWorkItemsPanel.this.createNameColumn();
                    }

                    @Override
                    protected String getStorageKey() {
                        return SessionStorage.KEY_WORK_ITEMS;
                    }

                    @Override
                    protected CaseWorkItemType getRowRealValue(PrismContainerValueWrapper<CaseWorkItemType> rowModelObject) {
                        if (rowModelObject == null) {
                            return null;
                        }
                        return rowModelObject.getRealValue();
                    }
                };
        workItemsPanel.setOutputMarkupId(true);
        workItemsPanel.setDashboard(view == View.DASHBOARD);
        add(workItemsPanel);
    }

    private ContainerListDataProvider<CaseWorkItemType> createProvider(IModel<Search<CaseWorkItemType>> searchModel) {
        Collection<SelectorOptions<GetOperationOptions>> options = CaseWorkItemsPanel.this.getPageBase().getOperationOptionsBuilder()
                .item(AbstractWorkItemType.F_ASSIGNEE_REF).resolve()
                .item(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF).resolve()
                .item(PrismConstants.T_PARENT, CaseType.F_TARGET_REF).resolve()
                .build();
        ContainerListDataProvider<CaseWorkItemType> provider = new ContainerListDataProvider<>(this,
                searchModel, options) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getWorkItemStorage();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                ObjectQuery query = null;
                ObjectFilter filter = getCaseWorkItemsFilter();
                if (filter != null) {
                    query = getPrismContext().queryFactory().createQuery(filter);
                }
                return query;
            }

        };
        provider.setSort(CaseWorkItemType.F_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }

    private IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String> createCheckboxColumn(){
        if (View.FULL_LIST.equals(view)) {
            return  new CheckBoxHeaderColumn<>();
        }
        return null;
    }

    private IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String> createIconColumn(){
        return new IconColumn<>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(CaseWorkItemType.COMPLEX_TYPE));
            }

        };
    }

    private IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String> createNameColumn(){
        return new LinkColumn<PrismContainerValueWrapper<CaseWorkItemType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                PolyStringType workitemName = ColumnUtils.unwrapRowModel(rowModel).getName();
                return Model.of(WebComponentUtil.getTranslatedPolyString(workitemName));
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return rowModel.getObject() != null && rowModel.getObject().getRealValue() != null;
            }

            @Override
            public void onClick(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                PageCaseWorkItem pageCaseWorkItem = new PageCaseWorkItem(rowModel.getObject() != null ? rowModel.getObject().getRealValue() : null, pageParameters);
                CaseWorkItemsPanel.this.getPageBase().navigateToNext(pageCaseWorkItem);
            }
        };
    }

    private List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> createDefaultColumns(){
        return ColumnUtils.getDefaultWorkItemColumns(getPageBase(), View.FULL_LIST.equals(view));
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
                IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = ((ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>) getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getRealValue() != null) {
                    CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                    return Model.of(!CaseTypeUtil.isClosed(CaseTypeUtil.getCase(workItem)));
                } else {
                    return super.getEnabled();
                }
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return createStringResource("CaseWorkItemsPanel.confirmWorkItemsRejectAction");
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_NO_OBJECTS);
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = ((ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>) getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getRealValue() != null) {
                    CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                    return Model.of(!CaseTypeUtil.isCorrelationCase(CaseTypeUtil.getCase(workItem)));
                } else {
                    return super.getVisible();
                }
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
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_ACTIVATION_ACTIVE);
            }

            @Override
            public IModel<Boolean> getEnabled() {
                IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = ((ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>) getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getRealValue() != null) {
                    CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                    return Model.of(!CaseTypeUtil.isClosed(CaseTypeUtil.getCase(workItem)));
                } else {
                    return super.getEnabled();
                }
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                return createStringResource("CaseWorkItemsPanel.confirmWorkItemsApproveAction");
            }

            @Override
            public IModel<Boolean> getVisible() {
                IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel = ((ColumnMenuAction<PrismContainerValueWrapper<CaseWorkItemType>>) getAction()).getRowModel();
                if (rowModel != null && rowModel.getObject() != null && rowModel.getObject().getRealValue() != null) {
                    CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                    return Model.of(!CaseTypeUtil.isCorrelationCase(CaseTypeUtil.getCase(workItem)));
                } else {
                    return super.getVisible();
                }
            }
        });

        return menu;
    }

    private void workItemActionPerformed(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel, boolean approved,
            AjaxRequestTarget target) {
        List<PrismContainerValueWrapper<CaseWorkItemType>> selectedWorkItems = new ArrayList<>();
        if (rowModel == null) {
            ContainerableListPanel<CaseWorkItemType, PrismContainerValueWrapper<CaseWorkItemType>> tablePanel = getContainerableListPanel();
            selectedWorkItems.addAll(tablePanel.getSelectedObjects());
        } else {
            selectedWorkItems.addAll(Collections.singletonList(rowModel.getObject()));
        }

        if (selectedWorkItems.size() == 0) {
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
                    null, powerDonor, approved, completeWorkItemResult, CaseWorkItemsPanel.this.getPageBase());
        });

        WebComponentUtil.clearProviderCache(getContainerableListPanel().getTable().getDataTable().getDataProvider());

        target.add(getPageBase().getFeedbackPanel());
        target.add(getContainerableListPanel());

    }

    public ContainerableListPanel<CaseWorkItemType, PrismContainerValueWrapper<CaseWorkItemType>> getContainerableListPanel() {
        return (ContainerableListPanel<CaseWorkItemType, PrismContainerValueWrapper<CaseWorkItemType>>) get(ID_WORKITEMS_TABLE);
    }

    protected ObjectFilter getCaseWorkItemsFilter() {
        return null;
    }

    private String getPowerDonorOidParameterValue() {
        if (pageParameters != null && pageParameters.get(PageAttorneySelection.PARAMETER_DONOR_OID) != null) {
            return pageParameters.get(PageAttorneySelection.PARAMETER_DONOR_OID).toString();
        }
        return null;
    }
}
