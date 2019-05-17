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

        CaseWorkItemListWithDetailsPanel caseWorkItems =
                new CaseWorkItemListWithDetailsPanel(ID_WORKITEMS_TABLE, getModel(), getTableId(), getWorkitemsTabStorage()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ObjectQuery createQuery() {
                        return CaseWorkItemsTablePanel.this.createQuery();
                    }

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return CaseWorkItemsTablePanel.this.getTableId();
                    }

//                    @Override
//                    public void itemDetailsPerformed(AjaxRequestTarget target,  IModel<PrismContainerValueWrapper<CaseWorkItemType>> model){
//                        getCaseWorkItemsTablePanel().itemDetailsPerformed(target, model);
//                    }

                };
        caseWorkItems.setOutputMarkupId(true);
        add(caseWorkItems);

        setOutputMarkupId(true);

    }

    protected MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentCandidatesSpecification> getCaseWorkItemsTablePanel() {
        return ((MultivalueContainerListPanelWithDetailsPanel<CaseWorkItemType, AssignmentCandidatesSpecification>)get(ID_WORKITEMS_TABLE));
    }

    protected abstract ObjectQuery createQuery();

    protected abstract UserProfileStorage.TableId getTableId();

    private ObjectTabStorage getWorkitemsTabStorage(){
        return getPageBase().getSessionStorage().getCaseWorkitemsTabStorage();
    }

}
