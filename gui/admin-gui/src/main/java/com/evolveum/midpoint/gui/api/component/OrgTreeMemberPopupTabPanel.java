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

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class OrgTreeMemberPopupTabPanel extends MemberPopupTabPanel<OrgType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ORG_TREE_VIEW_PANEL = "orgTreeViewPanel";

    public OrgTreeMemberPopupTabPanel(String id, List<QName> availableRelationList){
        super(id, availableRelationList);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        OrgTreeAssignablePanel orgTreePanel = new OrgTreeAssignablePanel(
                ID_ORG_TREE_VIEW_PANEL, true, getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel) {
                onSelectionPerformed(target, rowModel);
            }

            @Override
            protected List<OrgType> getPreselectedOrgsList(){
                return getPreselectedObjects();
            }

            @Override
            protected boolean isAssignButtonVisible(){
                return false;
            }
        };
        orgTreePanel.setOutputMarkupId(true);
        add(orgTreePanel);

    }

    @Override
    protected boolean isObjectListPanelVisible(){
        return false;
    }

    protected List<OrgType> getSelectedObjectsList(){
        return getPreselectedObjects();
    }

    @Override
    protected ObjectTypes getObjectType(){
        return ObjectTypes.ORG;
    }
}
