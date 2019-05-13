/**
 * Copyright (c) 2015-2019 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
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

    private AssignmentHolderTypeAssignmentsTabPanel assignmentsTabPanel = null;

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
                        return new AssignmentHolderTypeAssignmentsTabPanel<>(panelId, getMainForm(), getObjectModel(), parentPage);
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(countAssignments());
                    }
                });

        return tabs;
    }

    protected IModel<PrismObject<AHT>> unwrapModel() {
        return new IModel<PrismObject<AHT>>() {

            @Override
            public PrismObject<AHT> getObject() {
                return getObjectWrapper().getObject();
            }
        };
    }

    protected FocusTabVisibleBehavior getTabVisibility(String authUrl, boolean isVisibleOnHistoryPage, PageAdminObjectDetails<AHT> parentPage){
        if (isFocusHistoryPage()){
            return new HistoryPageTabVisibleBehavior<AHT>(unwrapModel(), authUrl, isVisibleOnHistoryPage, parentPage);
        } else {
            return new FocusTabVisibleBehavior<AHT>(unwrapModel(), authUrl, parentPage);
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
            if (!AssignmentsUtil.isPolicyRuleAssignment(assignment) && !AssignmentsUtil.isConsentAssignment(assignment)
                    && AssignmentsUtil.isAssignmentRelevant(assignment) && !AssignmentsUtil.isArchetypeAssignment(assignment)) {
                rv++;
            }
        }
        return rv;
    }


}
