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
import com.evolveum.midpoint.gui.api.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.wf.WorkItemsPanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by honchar
 */
public class CaseWorkItemsPanel extends BasePanel<CaseWorkItemType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_WORKITEMS_TABLE = "workitemsTable";

    public enum View {
        FULL_LIST,				// selectable, full information
        DASHBOARD, 				// not selectable, reduced info (on dashboard)
        ITEMS_FOR_PROCESS		// work items for a process
    }

    private View view;

    public CaseWorkItemsPanel(String id, View view){
        super(id);
        this.view = view;
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

        };
        workItemsPanel.setOutputMarkupId(true);
        add(workItemsPanel);
    }

    private List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> initColumns(){
        List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> columns = new ArrayList<>();

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
                return Model.of(ColumnUtils.unwrapRowModel(rowModel).getName());
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                //TODO should we check any authorization?
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                PageParameters pageParameters = new PageParameters();
                CaseWorkItemType caseWorkItemType = rowModel.getObject().getRealValue();
                CaseType parentCase = CaseTypeUtil.getCase(caseWorkItemType);
                WorkItemId workItemId = WorkItemId.create(parentCase != null ? parentCase.getOid() : "", caseWorkItemType.getId());
                pageParameters.add(OnePageParameterEncoder.PARAMETER, workItemId.asString());
                CaseWorkItemsPanel.this.getPageBase().navigateToNext(PageCaseWorkItem.class, pageParameters);
            }
        });

        columns.addAll(ColumnUtils.getDefaultWorkItemColumns(getPageBase(), View.FULL_LIST.equals(view)));
        return columns;
    }

    protected ObjectFilter getCaseWorkItemsFilter(){
        return null;
    }
}
