package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class CaseWorkItemListWithDetailsPanel extends MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, String> {

    private static final long serialVersionUID = 1L;

    private static final String ID_WORK_ITEM_ACTIVITY_BUTTON = "workItemActivityButton";

    public CaseWorkItemListWithDetailsPanel(String id, IModel<PrismContainerWrapper<CaseWorkItemType>> model, UserProfileStorage.TableId tableId, PageStorage pageStorage){
        super(id, model, tableId, pageStorage);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        AjaxButton workItemActivitiesButton = new AjaxButton(ID_WORK_ITEM_ACTIVITY_BUTTON,
                createStringResource("CaseWorkItemListWithDetailsPanel.workItemActivities")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        getDetailsPanelContainer().add(workItemActivitiesButton);
    }

    @Override
    protected void initPaging() {
        getWorkitemsTabStorage().setPaging(getPrismContext().queryFactory()
                .createPaging(0, ((int) CaseWorkItemListWithDetailsPanel.this.getPageBase().getItemsPerPage(getTableId()))));
    }

    protected abstract UserProfileStorage.TableId getTableId();

    @Override
    protected abstract ObjectQuery createQuery();

    @Override
    protected boolean enableActionNewObject() {
        return false;
    }

    @Override
    protected boolean isSearchEnabled(){
        return false;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> createColumns() {
        return getWorkItemColumns();
    }

    @Override
    protected WebMarkupContainer getSearchPanel(String contentAreaId) {
        return new WebMarkupContainer(contentAreaId);
    }

    @Override
    protected List<PrismContainerValueWrapper<CaseWorkItemType>> postSearch(
            List<PrismContainerValueWrapper<CaseWorkItemType>> workItems) {
        return workItems;
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

    private ObjectTabStorage getWorkitemsTabStorage(){
        return getPageBase().getSessionStorage().getCaseWorkitemsTabStorage();
    }

    private MultivalueContainerDetailsPanel<CaseWorkItemType> createWorkItemDetailsPanel(
            ListItem<PrismContainerValueWrapper<CaseWorkItemType>> item) {
        MultivalueContainerDetailsPanel<CaseWorkItemType> detailsPanel = new  MultivalueContainerDetailsPanel<CaseWorkItemType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayNamePanel<CaseWorkItemType> createDisplayNamePanel(String displayNamePanelId) {
                ItemRealValueModel<CaseWorkItemType> displayNameModel =
                        new ItemRealValueModel<CaseWorkItemType>(item.getModel());
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
                WorkItemDetailsPanel workItemDetails = new WorkItemDetailsPanel(idPanel, Model.of(item.getModel().getObject().getRealValue()));
                workItemDetails.setOutputMarkupId(true);
                add(workItemDetails);
            }

        };
        return detailsPanel;
    }


    private List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> getWorkItemColumns(){
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
        columns.add(new LinkColumn<PrismContainerValueWrapper<CaseWorkItemType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(unwrapRowModel(rowModel).getName());
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

        columns.add(new AbstractExportableColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.stage")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId, WfContextUtil.getStageInfo(unwrapRowModel(rowModel))));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WfContextUtil.getStageInfo(unwrapRowModel(rowModel)));
            }


        });
        columns.add(new AbstractExportableColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.actors")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {

                String assignee = WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getAssigneeRef(), false);
                cellItem.add(new Label(componentId,
                        assignee != null ? assignee : WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getCandidateRef(), true)));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                String assignee = WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getAssigneeRef(), false);
                return Model.of(assignee != null ? assignee : WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getCandidateRef(), true));
            }
        });
        columns.add(new AbstractExportableColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.created")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId,
                        WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), CaseWorkItemListWithDetailsPanel.this.getPageBase())));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), CaseWorkItemListWithDetailsPanel.this.getPageBase()));
            }
        });
        columns.add(new AbstractExportableColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.deadline")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId,
                        WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(), CaseWorkItemListWithDetailsPanel.this.getPageBase())));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(),
                        CaseWorkItemListWithDetailsPanel.this.getPageBase()));
            }
        });
        columns.add(new AbstractExportableColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.escalationLevel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId, WfContextUtil.getEscalationLevelInfo(unwrapRowModel(rowModel))));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WfContextUtil.getEscalationLevelInfo(unwrapRowModel(rowModel)));
            }
        });
        return columns;
    }

    private CaseWorkItemType unwrapRowModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel){
        return rowModel.getObject().getRealValue();
    }

}
