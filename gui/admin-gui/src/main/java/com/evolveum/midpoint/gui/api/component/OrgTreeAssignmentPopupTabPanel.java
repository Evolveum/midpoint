/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by honchar
 */
public class OrgTreeAssignmentPopupTabPanel extends FocusTypeAssignmentPopupTabPanel<OrgType>{

    private static final long serialVersionUID = 1L;

    private static final String ID_ORG_TREE_VIEW_PANEL = "orgTreeViewPanel";

    public OrgTreeAssignmentPopupTabPanel(String id){
        super(id, ObjectTypes.ORG);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        OrgTreeAssignablePanel orgTreePanel = new OrgTreeAssignablePanel(
                ID_ORG_TREE_VIEW_PANEL, true) {
            private static final long serialVersionUID = 1L;

           @Override
            protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel) {
               onSelectionPerformed(target, rowModel);
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
