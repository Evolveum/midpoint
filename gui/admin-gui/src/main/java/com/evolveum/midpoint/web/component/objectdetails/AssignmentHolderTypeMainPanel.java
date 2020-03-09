/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ObjectTabVisibleBehavior;
import com.evolveum.midpoint.gui.api.util.HistoryPageTabVisibleBehavior;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by honchar
 */
public class AssignmentHolderTypeMainPanel<AHT extends AssignmentHolderType> extends AbstractObjectMainPanel<AHT>{
    private static final long serialVersionUID = 1L;

    public AssignmentHolderTypeMainPanel(String id, LoadableModel<PrismObjectWrapper<AHT>> objectModel,
                                         PageAdminObjectDetails<AHT> parentPage) {
        super(id, objectModel, parentPage);
    }

    @Override
    protected List<ITab> createTabs(final PageAdminObjectDetails<AHT> parentPage) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(
                new PanelTab(parentPage.createStringResource("pageAdminFocus.basic"),
                        getTabVisibility(ComponentConstants.UI_FOCUS_TAB_BASIC_URL, true, parentPage)){

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new AssignmentHolderTypeDetailsTabPanel<>(panelId, getMainForm(), getObjectModel());
                    }
                });

        tabs.add(
                new CountablePanelTab(parentPage.createStringResource("pageAdminFocus.assignments"),
                        getTabVisibility(ComponentConstants.UI_FOCUS_TAB_ASSIGNMENTS_URL, true, parentPage)) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new AssignmentHolderTypeAssignmentsTabPanel<AHT>(panelId, getMainForm(), getObjectModel()){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected boolean isReadonly(){
                                return AssignmentHolderTypeMainPanel.this.isReadonly();
                            }
                        };
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(countAssignments());
                    }
                });

        return tabs;
    }

    protected IModel<PrismObject<AHT>> unwrapModel() {
        return (IModel<PrismObject<AHT>>) () -> getObjectWrapper().getObject();
    }

    protected ObjectTabVisibleBehavior<AHT> getTabVisibility(String authUrl, boolean isVisibleOnHistoryPage, PageAdminObjectDetails<AHT> parentPage){
        if (isFocusHistoryPage()){
            return new HistoryPageTabVisibleBehavior<>(unwrapModel(), authUrl, isVisibleOnHistoryPage, parentPage);
        } else {
            return new ObjectTabVisibleBehavior<>(unwrapModel(), authUrl, parentPage);
        }
    }

    protected boolean isFocusHistoryPage(){
        return false;
    }

    protected int countAssignments() {
        int rv = 0;
        PrismObject<AHT> focus = getObjectModel().getObject().getObject();
        List<AssignmentType> assignments = focus.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            if (!AssignmentsUtil.isConsentAssignment(assignment)
                    && AssignmentsUtil.isAssignmentRelevant(assignment) && !AssignmentsUtil.isArchetypeAssignment(assignment)) {
                rv++;
            }
        }
        return rv;
    }

    //this value will be used independently of object wrapper isReadonly value,
    protected boolean isReadonly(){
        return false;
    }


}
