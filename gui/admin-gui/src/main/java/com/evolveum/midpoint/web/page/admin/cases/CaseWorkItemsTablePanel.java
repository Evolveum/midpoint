/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
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
 * Created by honchar.
 */
public abstract class CaseWorkItemsTablePanel extends BasePanel<PrismContainerWrapper<CaseWorkItemType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_WORKITEMS_TABLE = "workitemsTable";

    public CaseWorkItemsTablePanel(String id, IModel<PrismContainerWrapper<CaseWorkItemType>> workItemsContainerWrapperModel) {
        super(id, workItemsContainerWrapperModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentObjectRelation> multivalueContainerListPanel =
                new MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentObjectRelation>(ID_WORKITEMS_TABLE,
                        getModel(), getTableId(),
                        getWorkitemsTabStorage()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void initPaging() {
                        getWorkitemsTabStorage().setPaging(getPrismContext().queryFactory()
                                .createPaging(0, ((int) CaseWorkItemsTablePanel.this.getPageBase().getItemsPerPage(getTableId()))));
                    }

                    @Override
                    protected ObjectQuery createQuery() {
                        return CaseWorkItemsTablePanel.this.createQuery();
                    }

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

                };
        multivalueContainerListPanel.setOutputMarkupId(true);
        add(multivalueContainerListPanel);

        setOutputMarkupId(true);

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
                getCaseWorkItemsTablePanel().itemDetailsPerformed(target, rowModel);

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
                        WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), CaseWorkItemsTablePanel.this.getPageBase())));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), CaseWorkItemsTablePanel.this.getPageBase()));
            }
        });
        columns.add(new AbstractExportableColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.deadline")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId,
                        WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(), CaseWorkItemsTablePanel.this.getPageBase())));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(),
                        CaseWorkItemsTablePanel.this.getPageBase()));
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

    protected MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentCandidatesSpecification> getCaseWorkItemsTablePanel() {
        return ((MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentCandidatesSpecification>)get(ID_WORKITEMS_TABLE));
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
                        //todo display parent case object description ?
                        CaseType caseType = CaseWorkItemUtil.getCase(displayNameModel.getObject());
                        return Model.of(caseType != null && caseType.getDescription() != null ? caseType.getDescription() : "");
                    }
                };
            }

            @Override
            protected void addBasicContainerValuePanel(String idPanel) {
                add(new WebMarkupContainer(idPanel));
            }

        };
        return detailsPanel;
    }

    protected abstract ObjectQuery createQuery();

    protected abstract UserProfileStorage.TableId getTableId();

    private ObjectTabStorage getWorkitemsTabStorage(){
        return getPageBase().getSessionStorage().getCaseWorkitemsTabStorage();
    }

    private CaseWorkItemType unwrapRowModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel){
        return rowModel.getObject().getRealValue();
    }

}
