/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author honchar
 */
public abstract class ChooseArchetypeMemberPopup<O extends AssignmentHolderType> extends ChooseMemberPopup<O, ArchetypeType> {
    private static final long serialVersionUID = 1L;

    public ChooseArchetypeMemberPopup(String id, AvailableRelationDto availableRelationList){
        super(id, availableRelationList);
    }

    @Override
    protected List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();//super.createAssignmentTabs();
        tabs.add(new CountablePanelTab(getPageBase().createStringResource("chooseMemberForOrgPopup.otherTypesLabel"),
                new VisibleBehaviour(() -> getAvailableObjectTypes() != null)) {

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
                    protected ArchetypeType getAbstractRoleTypeObject(){
                        return ChooseArchetypeMemberPopup.this.getAssignmentTargetRefObject();
                    }

                    @Override
                    protected List<ObjectTypes> getSupportedTypesList() {
                        return getAvailableObjectTypes().stream().map(type -> ObjectTypes.getObjectTypeFromTypeQName(type)).collect(Collectors.toList());
                    }

                    @Override
                    protected ObjectTypes getObjectType() {
                       if (CollectionUtils.isNotEmpty(getSupportedTypesList())) {
                           return getSupportedTypesList().get(0);
                       }
                       return super.getObjectType();
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
        return ArchetypeType.COMPLEX_TYPE;
    }

}
