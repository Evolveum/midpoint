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

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author honchar
 */
public abstract class ChooseArchetypeMemberPopup<O extends AssignmentHolderType> extends ChooseMemberPopup<O, ArchetypeType> {
    private static final long serialVersionUID = 1L;

    public ChooseArchetypeMemberPopup(String id, Search search){
        super(id, search, null); //todo
    }

    @Override
    protected List<ITab> createAssignmentTabs(AssignmentObjectRelation relationSpec) {
        List<ITab> tabs = new ArrayList<>();//super.createAssignmentTabs();
        tabs.add(new CountablePanelTab(getPageBase().createStringResource("chooseMemberForOrgPopup.otherTypesLabel"),
                new VisibleBehaviour(() -> getAvailableObjectTypes() != null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new MultiTypesMemberPopupTabPanel<O>(panelId, search, getArchetypeRefList()){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<O>>> rowModelList, DataTable dataTable){
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
