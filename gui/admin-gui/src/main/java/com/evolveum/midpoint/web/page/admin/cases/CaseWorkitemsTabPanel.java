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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.GlobalPolicyRuleTabPanel;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.web.page.admin.workflow.dto.WorkItemDto.F_STAGE_INFO;

/**
 * Created by honchar
 */
public class CaseWorkitemsTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_WORKITEMS_PANEL = "workitemsPanel";

    public CaseWorkitemsTabPanel(String id, Form<ObjectWrapper<CaseType>> mainForm, LoadableModel<ObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel, pageBase);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ContainerWrapperFromObjectWrapperModel<CaseWorkItemType, CaseType> workitemsModel = new ContainerWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), CaseType.F_WORK_ITEM);

        MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentObjectRelation> multivalueContainerListPanel =
                new MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentObjectRelation>(ID_WORKITEMS_PANEL,
                        workitemsModel, UserProfileStorage.TableId.PAGE_CASE_WORKITEMS_TAB,
                        getWorkitemsTabStorage()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void initPaging() {
                        getWorkitemsTabStorage().setPaging(getPrismContext().queryFactory()
                                .createPaging(0, ((int) pageBase.getItemsPerPage(UserProfileStorage.TableId.PAGE_CASE_WORKITEMS_TAB))));
                    }

                    @Override
                    protected ObjectQuery createQuery() {
                        return CaseWorkitemsTabPanel.this.getPageBase().getPrismContext().queryFactory().createQuery();
                    }

                    @Override
                    protected boolean enableActionNewObject() {
                        return true;
                    }

                    @Override
                    protected List<IColumn<ContainerValueWrapper<CaseWorkItemType>, String>> createColumns() {
                        return getWorkItemColumns();
                    }

                    @Override
                    protected void newItemPerformed(AjaxRequestTarget target){
                        //todo clean up
//                        newAssignmentClickPerformed(target, null);
                    }

                    @Override
                    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation assignmentTargetRelation) {
//                        newAssignmentClickPerformed(target, assignmentTargetRelation);
                    }

                    @Override
                    protected MultivalueContainerDetailsPanel<CaseWorkItemType> getMultivalueContainerDetailsPanel(
                            ListItem<ContainerValueWrapper<CaseWorkItemType>> item) {
                        return null;
//                        return createMultivalueContainerDetailsPanel(item);
                    }

                    @Override
                    protected WebMarkupContainer getSearchPanel(String contentAreaId) {
                        return new WebMarkupContainer(contentAreaId);
                    }

                    @Override
                    protected List<ContainerValueWrapper<CaseWorkItemType>> postSearch(
                            List<ContainerValueWrapper<CaseWorkItemType>> workItems) {
                        return workItems;
                    }

                    @Override
                    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<CaseWorkItemType> containerDef) {
                            List<SearchItemDefinition> defs = new ArrayList<>();
                            return defs;
                        }

//                    @Override
//                    protected WebMarkupContainer initButtonToolbar(String id) {
//                        WebMarkupContainer buttonToolbar = initCustomButtonToolbar(id);
//                        if(buttonToolbar == null) {
//                            return super.initButtonToolbar(id);
//                        }
//                        return buttonToolbar;
//                    }

                };
        add(multivalueContainerListPanel);

        setOutputMarkupId(true);
    }

    private List<IColumn<ContainerValueWrapper<CaseWorkItemType>, String>> getWorkItemColumns(){
        List<IColumn<ContainerValueWrapper<CaseWorkItemType>, String>> columns = new ArrayList<>();

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
        columns.add(new LinkColumn<ContainerValueWrapper<CaseWorkItemType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(unwrapRowModel(rowModel).getName());
            }

            @Override
            public boolean isEnabled(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                //TODO should we check any authorization?
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
//                                AssignmentPanel.this.assignmentDetailsPerformed(target);
//                                getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }
        });

        columns.add(new AbstractExportableColumn<ContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.stage")) {

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId, WfContextUtil.getStageInfo(unwrapRowModel(rowModel))));
            }

            @Override
            public IModel<String> getDataModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WfContextUtil.getStageInfo(unwrapRowModel(rowModel)));
            }


        });
        columns.add(new AbstractExportableColumn<ContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.actors")) {

                         @Override
                         public void populateItem(Item<ICellPopulator<ContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                                  String componentId, IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {

                             String assignee = WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getAssigneeRef(), false);
                             cellItem.add(new Label(componentId,
                                     assignee != null ? assignee : WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getCandidateRef(), true)));
                         }

                         @Override
                         public IModel<String> getDataModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                             String assignee = WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getAssigneeRef(), false);
                             return Model.of(assignee != null ? assignee : WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getCandidateRef(), true));
                         }
                     });
        columns.add(new AbstractExportableColumn<ContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.created")) {

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    cellItem.add(new Label(componentId,
                            WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), pageBase)));
            }

            @Override
            public IModel<String> getDataModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), pageBase));
            }
        });
        columns.add(new AbstractExportableColumn<ContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.deadline")) {

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId,
                        WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(), pageBase)));
            }

            @Override
            public IModel<String> getDataModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(), pageBase));
            }
        });
        columns.add(new AbstractExportableColumn<ContainerValueWrapper<CaseWorkItemType>, String>(
                createStringResource("WorkItemsPanel.escalationLevel")) {

            @Override
            public void populateItem(Item<ICellPopulator<ContainerValueWrapper<CaseWorkItemType>>> cellItem,
                                     String componentId, IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId, WfContextUtil.getEscalationLevelInfo(unwrapRowModel(rowModel))));
            }

            @Override
            public IModel<String> getDataModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WfContextUtil.getEscalationLevelInfo(unwrapRowModel(rowModel)));
            }
        });
        return columns;
    }

    private ObjectTabStorage getWorkitemsTabStorage(){
        return getPageBase().getSessionStorage().getCaseWorkitemsTabStorage();
    }


    private CaseWorkItemType unwrapRowModel(IModel<ContainerValueWrapper<CaseWorkItemType>> rowModel){
        return rowModel.getObject().getContainerValue().asContainerable();
    }
}
