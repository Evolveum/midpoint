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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.ClientProperties;
import org.apache.wicket.protocol.http.WebSession;

import java.util.*;

/**
 * Created by honchar.
 */
public class AssignmentPopup extends BasePanel implements Popupable{
    private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_ASSIGN_BUTTON = "assignButton";
    private static final String ID_FORM = "form";

    private List<OrgType> selectedOrgsList = new ArrayList<>();

    public AssignmentPopup(String id){
        super(id);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        Form form = new Form(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        List<ITab> tabs = createAssignmentTabs();
        TabbedPanel<ITab> tabPanel = WebComponentUtil.createTabPanel(ID_TABS_PANEL, getPageBase(), tabs, null);
        tabPanel.setOutputMarkupId(true);
        form.add(tabPanel);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
                createStringResource("userBrowserDialog.button.cancelButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        form.add(cancelButton);

        AjaxButton addButton = new AjaxButton(ID_ASSIGN_BUTTON,
                createStringResource("userBrowserDialog.button.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Map<String, AssignmentType> selectedAssignmentsMap = new HashMap<>();

                tabs.forEach(panelTab -> {
                    WebMarkupContainer assignmentPanel = ((CountablePanelTab)panelTab).getPanel();
                    if (assignmentPanel == null){
                        return;
                    }

                    (((AbstractAssignmentPopupTabPanel) assignmentPanel).getSelectedAssignmentsMap()).forEach((k, v) ->
                            selectedAssignmentsMap.putIfAbsent((String)k, (AssignmentType) v));


                });
                List assignments = new ArrayList<>();
                assignments.addAll(Arrays.asList(selectedAssignmentsMap.values().toArray()));
                addPerformed(target, assignments);
            }
        };
        addButton.add(AttributeAppender.append("title", getAddButtonTitleModel()));
        addButton.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isAssignButtonEnabled();
            }
        });
        addButton.setOutputMarkupId(true);
        form.add(addButton);
    }

    protected List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"),
                new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ROLE))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.ROLE){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }

                            @Override
                            protected ObjectTypes getObjectType(){
                                return ObjectTypes.ROLE;
                            }
                        };
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(getTabPanelSelectedCount(getPanel()));
                    }
                });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"),
                        new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ORG))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.ORG){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }

                            @Override
                            protected ObjectTypes getObjectType(){
                                return ObjectTypes.ORG;
                            }

                            @Override
                            protected List<OrgType> getPreselectedObjects(){
                                return selectedOrgsList;
                            }

                        };
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(selectedOrgsList.size());
                    }
                });


        tabs.add(new CountablePanelTab(createStringResource("TypedAssignablePanel.orgTreeView"),
                new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ORG))) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new OrgTreeAssignmentPopupTabPanel(panelId){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target){
                        tabLabelPanelUpdate(target);
                    }

                    @Override
                    protected List<OrgType> getPreselectedObjects(){
                        return selectedOrgsList;
                    }
                };
            }

            @Override
            public String getCount() {
                return Integer.toString(selectedOrgsList.size());
            }
        });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.SERVICE"),
                        new VisibleBehaviour(() -> isTabVisible(ObjectTypes.SERVICE))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.SERVICE){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected ObjectTypes getObjectType(){
                                return ObjectTypes.SERVICE;
                            }

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }

                        };
                    }

                    @Override
                    public String getCount() {
                        return Integer.toString(getTabPanelSelectedCount(getPanel()));
                    }
                });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.RESOURCE"),
                        new VisibleBehaviour(() -> isTabVisible(ObjectTypes.RESOURCE))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new ResourceTypeAssignmentPopupTabPanel(panelId){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                super.onSelectionPerformed(target);
                                tabLabelPanelUpdate(target);
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

    private boolean isTabVisible(ObjectTypes objectType){
        List<ObjectTypes> availableObjectTypesList = getAvailableObjectTypesList();
        return availableObjectTypesList == null || availableObjectTypesList.size() == 0 || availableObjectTypesList.contains(objectType);
    }

    protected List<ObjectTypes> getAvailableObjectTypesList(){
        return WebComponentUtil.createAssignableTypesList();
    }

    private int getTabPanelSelectedCount(WebMarkupContainer panel){
        if (panel != null && panel instanceof AbstractAssignmentPopupTabPanel){
            return ((AbstractAssignmentPopupTabPanel) panel).getSelectedObjectsList().size();
        }
        return 0;
    }

    private void tabLabelPanelUpdate(AjaxRequestTarget target){
        target.add(AssignmentPopup.this);
    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    protected void addPerformed(AjaxRequestTarget target, List newAssignmentsList) {
        getPageBase().hideMainPopup(target);
    }

    private IModel<String> getAddButtonTitleModel(){
        return new LoadableModel<String>(true) {
            @Override
            protected String load() {
                return !isAssignButtonEnabled() ? createStringResource("AssignmentPopup.addButtonTitle").getString() : "";
            }
        };
    }

    private boolean isAssignButtonEnabled(){
        TabbedPanel tabbedPanel = getTabbedPanel();
        List<ITab> tabs = (List<ITab>) tabbedPanel.getTabs().getObject();
        for (ITab tab : tabs){
            WebMarkupContainer assignmentPanel = ((CountablePanelTab)tab).getPanel();
            if (assignmentPanel == null){
                continue;
            }
            if (((AbstractAssignmentPopupTabPanel) assignmentPanel).getSelectedObjectsList().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public int getWidth(){
        return 80;
    }

    public int getHeight(){
        return 80;
    }

    @Override
    public String getWidthUnit(){
        return "%";
    }

    @Override
    public String getHeightUnit(){
        return "%";
    }

    public StringResourceModel getTitle(){
        return createStringResource("TypedAssignablePanel.selectObjects");
    }

    public Component getComponent(){
        return this;
    }
}
