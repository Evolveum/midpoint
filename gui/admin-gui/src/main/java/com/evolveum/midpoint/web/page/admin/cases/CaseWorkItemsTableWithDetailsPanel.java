/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public abstract class CaseWorkItemsTableWithDetailsPanel extends BasePanel<PrismContainerWrapper<CaseWorkItemType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_WORKITEMS_TABLE = "workitemsTable";

    public CaseWorkItemsTableWithDetailsPanel(String id, IModel<PrismContainerWrapper<CaseWorkItemType>> workItemsContainerWrapperModel) {
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
                new CaseWorkItemListWithDetailsPanel(ID_WORKITEMS_TABLE, getModel(), getTableId()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected PageStorage getPageStorage() {
                        return getPageBase().getSessionStorage().getCaseWorkitemsTabStorage();
                    }

                    @Override
                    protected ObjectQuery createQuery() {
                        return CaseWorkItemsTableWithDetailsPanel.this.createQuery();
                    }

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return CaseWorkItemsTableWithDetailsPanel.this.getTableId();
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
}
