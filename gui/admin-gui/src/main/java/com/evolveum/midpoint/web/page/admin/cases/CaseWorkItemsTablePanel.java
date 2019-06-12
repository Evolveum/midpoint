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
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.apache.wicket.model.IModel;

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
