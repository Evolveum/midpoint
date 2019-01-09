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

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;

/**
 * Created by honchar
 */

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.List;

public abstract class ChooseOrgMemberPopup<O extends ObjectType> extends ChooseMemberPopup<O, OrgType> {
    private static final long serialVersionUID = 1L;

    public ChooseOrgMemberPopup(String id, List<QName> availableRelationList){
        super(id, availableRelationList);
    }

    @Override
    protected List<ITab> createAssignmentTabs() {
        List<ITab> tabs = super.createAssignmentTabs();
        tabs.add(new CountablePanelTab(getPageBase().createStringResource("chooseMemberForOrgPopup.otherTypesLabel"), null) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new MultiTypesMemberPopupTabPanel<O>(panelId, availableRelationList){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel){
                        tabLabelPanelUpdate(target);
                    }

                    @Override
                    protected OrgType getAbstractRoleTypeObject(){
                        return ChooseOrgMemberPopup.this.getAssignmentTargetRefObject();
                    }
                };
            }

            @Override
            public String getCount() {
                return Integer.toString(getTabPanelSelectedCount(getPanel()));
            }
        });
        return tabs;
    }

}
