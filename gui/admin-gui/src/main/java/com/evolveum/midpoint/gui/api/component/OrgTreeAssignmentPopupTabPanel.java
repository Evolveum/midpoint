/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 */
public class OrgTreeAssignmentPopupTabPanel extends FocusTypeAssignmentPopupTabPanel<OrgType>{

    private static final long serialVersionUID = 1L;

    private static final String ID_ORG_TREE_VIEW_PANEL = "orgTreeViewPanel";

    public OrgTreeAssignmentPopupTabPanel(String id, AssignmentObjectRelation assignmentObjectRelation){
        super(id, ObjectTypes.ORG, assignmentObjectRelation);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        OrgTreeAssignablePanel orgTreePanel = new OrgTreeAssignablePanel(
                ID_ORG_TREE_VIEW_PANEL, true) {
            private static final long serialVersionUID = 1L;

           @Override
            protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<TreeSelectableBean<OrgType>> rowModel) {
               onSelectionPerformed(target, Arrays.asList((IModel) rowModel), null);
            }

            @Override
            protected boolean isAssignButtonVisible(){
                return false;
            }

            @Override
            protected List<OrgType> getPreselectedOrgsList(){
                return getPreselectedObjects();
            }

            @Override
            protected boolean isInducement(){
                return OrgTreeAssignmentPopupTabPanel.this.isInducement();
            }

            @Override
            protected ObjectFilter getSubtypeFilter(){
                return OrgTreeAssignmentPopupTabPanel.this.getSubtypeFilter();
            }
        };
        orgTreePanel.setOutputMarkupId(true);
        add(orgTreePanel);

    }

    @Override
    protected ObjectTypes getObjectType(){
        return ObjectTypes.ORG;
    }

    @Override
    protected boolean isObjectListPanelVisible(){
        return false;
    }

    protected List<OrgType> getSelectedObjectsList(){
        if (get(ID_ORG_TREE_VIEW_PANEL) == null){
            return null;
        }
        return ((OrgTreeAssignablePanel)get(ID_ORG_TREE_VIEW_PANEL)).getAllTabPanelsSelectedOrgs();
    }

}
