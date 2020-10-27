/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.session.ContainerTabStorage;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageInstance;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by honchar
 */
public abstract class CaseWorkItemListWithDetailsPanel extends MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_CASE_WORK_ITEM_ACTIONS_PANEL = "caseWorkItemActionsPanel";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private WorkItemDetailsPanel workItemDetails = null;

    public CaseWorkItemListWithDetailsPanel(String id){
        super(id, CaseWorkItemType.class);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
                createStringResource("MultivalueContainerListPanel.cancelButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                setItemDetailsVisible(false);
                cancelItemDetailsPerformed(ajaxRequestTarget);
                ajaxRequestTarget.add(CaseWorkItemListWithDetailsPanel.this);
                ajaxRequestTarget.add(CaseWorkItemListWithDetailsPanel.this.getPageBase().getFeedbackPanel());
            }
        };
        getDetailsPanelContainer().add(cancelButton);

        CaseWorkItemActionsPanel actionsPanel = new CaseWorkItemActionsPanel(ID_CASE_WORK_ITEM_ACTIONS_PANEL,
                new LoadableModel<CaseWorkItemType>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected CaseWorkItemType load() {
                        return getDetailsPanelItemsList().size() > 0 ? getDetailsPanelItemsList().get(0).getRealValue() : null;
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected AbstractWorkItemOutputType getWorkItemOutput(boolean approved) {
                String comment = workItemDetails != null ? workItemDetails.getApproverComment() : null;
                byte[] evidence = workItemDetails != null ? workItemDetails.getWorkItemEvidence() : null;
                return super.getWorkItemOutput(approved)
                        .comment(comment)
                        .evidence(evidence);
            }

            @Override
            protected WorkItemDelegationRequestType getDelegationRequest(UserType delegate) {
                return super.getDelegationRequest(delegate)
                        .comment(workItemDetails != null ? workItemDetails.getApproverComment() : null);
            }

            @Override
            protected Component getCustomForm() {
                return workItemDetails != null ? workItemDetails.getCustomForm() : null;
            }

            @Override
            protected void afterActionFinished(AjaxRequestTarget target) {
                Breadcrumb previousBreadcrumb = getPageBase().getPreviousBreadcrumb();
                if (previousBreadcrumb instanceof BreadcrumbPageInstance &&
                        ((BreadcrumbPageInstance) previousBreadcrumb).getPage() instanceof PageCaseWorkItem) {
                    getPageBase().redirectBack(3);
                } else {
                    getPageBase().redirectBack();
                }
            }
        };
        actionsPanel.setOutputMarkupId(true);
        actionsPanel.add(new VisibleBehaviour(() -> {
            CaseWorkItemType workItemSelected = getDetailsPanelItemsList().size() > 0 ? getDetailsPanelItemsList().get(0).getRealValue() : null;
            return CaseWorkItemUtil.isCaseWorkItemNotClosed(workItemSelected);
        }));
        getDetailsPanelContainer().add(actionsPanel);
    }

    protected abstract UserProfileStorage.TableId getTableId();

    @Override
    protected abstract ObjectQuery createQuery();

    @Override
    protected boolean isCreateNewObjectVisible() {
        return false;
    }

    @Override
    protected boolean isSearchVisible() {
        return false;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> createColumns() {
        return getWorkItemColumns();
    }

    @Override
    protected List<PrismContainerValueWrapper<CaseWorkItemType>> postSearch(
            List<PrismContainerValueWrapper<CaseWorkItemType>> workItems) {
        return workItems;
    }

    @Override
    protected boolean isButtonPanelVisible() {
        return false;
    }

    @Override
    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<CaseWorkItemType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();
        return defs;
    }

    @Override
    protected MultivalueContainerDetailsPanel<CaseWorkItemType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<CaseWorkItemType>> item) {
        return createWorkItemDetailsPanel(item);
    }

//    @Override
//    public void itemDetailsPerformed(AjaxRequestTarget target,  IModel<PrismContainerValueWrapper<CaseWorkItemType>> model){}

//    private ContainerTabStorage getWorkitemsTabStorage() {
//        return getPageBase().getSessionStorage().getCaseWorkitemsTabStorage();
//    }

    private MultivalueContainerDetailsPanel<CaseWorkItemType> createWorkItemDetailsPanel(
            ListItem<PrismContainerValueWrapper<CaseWorkItemType>> item) {
        MultivalueContainerDetailsPanel<CaseWorkItemType> detailsPanel = new MultivalueContainerDetailsPanel<CaseWorkItemType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayNamePanel<CaseWorkItemType> createDisplayNamePanel(String displayNamePanelId) {
                ItemRealValueModel<CaseWorkItemType> displayNameModel = new ItemRealValueModel<>(item.getModel());
                return new DisplayNamePanel<CaseWorkItemType>(displayNamePanelId, displayNameModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected IModel<String> getDescriptionLabelModel() {
                        CaseType caseType = CaseWorkItemUtil.getCase(displayNameModel.getObject());
                        return Model.of(caseType != null && caseType.getDescription() != null ? caseType.getDescription() : "");
                    }
                };
            }

            @Override
            protected void addBasicContainerValuePanel(String idPanel) {
                //todo fix  implement with WorkItemDetailsPanelFactory
                workItemDetails = new WorkItemDetailsPanel(idPanel, Model.of(item.getModel().getObject().getRealValue()));
                workItemDetails.setOutputMarkupId(true);
                add(workItemDetails);
            }

        };
        return detailsPanel;
    }

    private List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> getWorkItemColumns() {
        List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> columns = new ArrayList<>();

//                        columns.add(new IconColumn<ContainerValueWrapper<CaseWorkItemType>>(Model.of("")) {
//
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            protected IModel<String> createIconModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
//                                return new IModel<String>() {
//
//                                    private static final long serialVersionUID = 1L;
//
//                                    @Override
//                                    public String getObject() {
//                                        return WebComponentUtil.createDefaultBlackIcon(AssignmentsUtil.getTargetType(rowModel.getObject().getContainerValue().asContainerable()));
//                                    }
//                                };
//                            }
//
//                        });
        columns.add(new AjaxLinkColumn<PrismContainerValueWrapper<CaseWorkItemType>>(createStringResource("PolicyRulesPanel.nameColumn")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                PolyStringType workitemName = unwrapRowModel(rowModel).getName();
                return Model.of(WebComponentUtil.getTranslatedPolyString(workitemName));
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                //TODO should we check any authorization?
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemListWithDetailsPanel.this.itemDetailsPerformed(target, rowModel);

            }
        });

        columns.addAll(ColumnUtils.getDefaultWorkItemColumns(getPageBase(), true));
        return columns;
    }

    private CaseWorkItemType unwrapRowModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
        return rowModel.getObject().getRealValue();
    }
}
