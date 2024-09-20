/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class OrgTreeMemberPopupTabPanel extends MemberPopupTabPanel<OrgType> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ORG_TREE_VIEW_PANEL = "orgTreeViewPanel";

    public OrgTreeMemberPopupTabPanel(String id, Search search, List<ObjectReferenceType> archetypeReferenceList){
        super(id, search, archetypeReferenceList);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        OrgTreeAssignablePanel orgTreePanel = new OrgTreeAssignablePanel(
                ID_ORG_TREE_VIEW_PANEL, true) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target, IModel<TreeSelectableBean<OrgType>> rowModel) {
                onSelectionPerformed(target, Arrays.asList((IModel) rowModel), null);
            }

            @Override
            protected List<OrgType> getPreselectedOrgsList(){
                return getPreselectedObjects();
            }

            @Override
            protected boolean isAssignButtonVisible(){
                return false;
            }

            @Override
            protected boolean isInducement(){
                return OrgTreeMemberPopupTabPanel.this.isInducement();
            }
        };
        orgTreePanel.setOutputMarkupId(true);
        add(orgTreePanel);

    }

    @Override
    protected boolean isObjectListPanelVisible(){
        return false;
    }

    @Override
    protected List<OrgType> getSelectedObjectsList(){
        return getPreselectedObjects();
    }

    @Override
    protected ObjectTypes getObjectType(){
        return ObjectTypes.ORG;
    }
}
