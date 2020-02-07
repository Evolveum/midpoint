/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author honchar
 */
public abstract class ChooseOrgMemberPopup<O extends ObjectType> extends ChooseMemberPopup<O, OrgType> {
    private static final long serialVersionUID = 1L;

    public ChooseOrgMemberPopup(String id, AvailableRelationDto availableRelationList){
        super(id, availableRelationList);
    }

    @Override
    protected List<ITab> createAssignmentTabs() {
        List<ITab> tabs = super.createAssignmentTabs();
        tabs.add(new CountablePanelTab(getPageBase().createStringResource("chooseMemberForOrgPopup.otherTypesLabel"),
                new VisibleBehaviour(() -> getAvailableObjectTypes() == null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new MultiTypesMemberPopupTabPanel<O>(panelId, availableRelationList, getArchetypeRefList()){
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

    @Override
    protected QName getDefaultTargetType() {
        return OrgType.COMPLEX_TYPE;
    }

}
