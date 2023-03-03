/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.orgs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.OrgStructurePanelStorage;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class OrgTreeAssignablePanel extends BasePanel<OrgType> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = OrgTreeAssignablePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ASSIGNABLE_ITEMS = DOT_CLASS + "loadAssignableOrgs";

    private static final String ID_ORG_TABS = "orgTabs";
    private static final String ID_ASSIGN = "assign";

    private final boolean selectable;
    private final List<OrgType> allTabsSelectedOrgs = new ArrayList<>();

    public OrgTreeAssignablePanel(String id, boolean selectable) {
        super(id);
        this.selectable = selectable;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AbstractOrgTabPanel tabbedPanel = new AbstractOrgTabPanel(ID_ORG_TABS) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Panel createTreePanel(String id, Model<String> model, PageBase pageBase) {
                OrgTreePanel panel = new OrgTreePanel(id, model, selectable, pageBase, "", allTabsSelectedOrgs) {
                    private static final long serialVersionUID = 1L;

                    private final OrgStructurePanelStorage storage = new OrgStructurePanelStorage();

                    @Override
                    protected IModel<Boolean> getCheckBoxValueModel(IModel<TreeSelectableBean<OrgType>> rowModel) {
                        return new LoadableModel<>(true) {

                            @Override
                            public Boolean load() {
                                for (OrgType org : getPreselectedOrgsList()) {
                                    if (rowModel.getObject().getValue().getOid().equals(org.getOid())) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        };
                    }

                    @Override
                    protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<TreeSelectableBean<OrgType>> rowModel) {
                        if (rowModel != null && rowModel.getObject() != null) {
                            boolean isAlreadyInList = false;
                            Iterator<OrgType> it = allTabsSelectedOrgs.iterator();
                            while (it.hasNext()) {
                                OrgType org = it.next();
                                if (org.getOid().equals(rowModel.getObject().getValue().getOid())) {
                                    isAlreadyInList = true;
                                    it.remove();
                                }
                            }
                            if (!isAlreadyInList) {
                                allTabsSelectedOrgs.add(rowModel.getObject().getValue());
                            }
                        }
                        OrgTreeAssignablePanel.this.onOrgTreeCheckBoxSelectionPerformed(target, rowModel);
                    }

                    @Override
                    protected void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected,
                            AjaxRequestTarget target) {
                        onItemSelect(selected, target);
                    }

                    @Override
                    public OrgTreeStateStorage getOrgTreeStateStorage() {
                        return storage;
                    }

                    @Override
                    protected ObjectFilter getCustomFilter() {
                        return OrgTreeAssignablePanel.this.getCustomFilter();
                    }
                };

                panel.setOutputMarkupId(true);
                panel.setOutputMarkupPlaceholderTag(true);
                return panel;
            }

            @Override
            protected boolean isWarnMessageVisible() {
                return false;
            }

            @Override
            protected ObjectFilter getAssignableItemsFilter() {
                return OrgTreeAssignablePanel.this.getCustomFilter();
            }

            @Override
            protected OrgStructurePanelStorage getOrgStructurePanelStorage() {
                return null;
            }

        };

        tabbedPanel.setOutputMarkupId(true);
        tabbedPanel.setOutputMarkupPlaceholderTag(true);
        add(tabbedPanel);

        AjaxButton assignButton = new AjaxButton(ID_ASSIGN,
                getPageBase().createStringResource("userBrowserDialog.button.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                assignSelectedOrgPerformed(getAllTabPanelsSelectedOrgs(), target);
            }
        };
        assignButton.setOutputMarkupId(true);
        assignButton.setOutputMarkupPlaceholderTag(true);
        assignButton.add(new VisibleBehaviour(() -> isAssignButtonVisible()));
        add(assignButton);

    }

    protected boolean isAssignButtonVisible() {
        return selectable;
    }

    protected void assignSelectedOrgPerformed(List<OrgType> selectedOrgs, AjaxRequestTarget target) {

    }

    public List<OrgType> getAllTabPanelsSelectedOrgs() {
        return allTabsSelectedOrgs;
    }

    protected void onItemSelect(SelectableBeanImpl<OrgType> selected, AjaxRequestTarget target) {

    }

    private ObjectFilter getCustomFilter() {
        ObjectFilter assignableItemsFilter = null;
        if (getAssignmentOwnerObject() != null) {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ITEMS);
            OperationResult result = task.getResult();
            assignableItemsFilter = WebComponentUtil.getAssignableRolesFilter(getAssignmentOwnerObject().asPrismObject(), OrgType.class,
                    isInducement() ? WebComponentUtil.AssignmentOrder.INDUCEMENT : WebComponentUtil.AssignmentOrder.ASSIGNMENT,
                    result, task, getPageBase());
        }

        ObjectFilter subTypeFilter = getSubtypeFilter();
        if (subTypeFilter == null) {
            return assignableItemsFilter;
        } else if (assignableItemsFilter == null) {
            return subTypeFilter;
        } else {
            ObjectQuery query = getPageBase().getPrismContext().queryFactory().createQuery();

            query.addFilter(assignableItemsFilter);
            query.addFilter(subTypeFilter);
            return query.getFilter();
        }
    }

    protected ObjectFilter getSubtypeFilter() {
        return null;
    }

    protected boolean isInducement() {
        return false;
    }

    protected <F extends FocusType> F getAssignmentOwnerObject() {
        return null;
    }

    protected List<OrgType> getPreselectedOrgsList() {
        return null;
    }

    protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<TreeSelectableBean<OrgType>> rowModel) {
    }

    @Override
    public int getWidth() {
        return 900;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("OrgTreeAssignablePanel.selectOrg");
    }
}
