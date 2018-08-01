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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar
 */
public abstract class ChooseMemberPopup<O extends ObjectType, T extends AbstractRoleType> extends BasePanel<O> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ChooseMemberPopup.class);

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_FORM = "form";

    private List<OrgType> selectedOrgsList = new ArrayList<>();
    protected List<QName> availableRelationList;

    public ChooseMemberPopup(String id, List<QName> availableRelationList){
        super(id);
        this.availableRelationList = availableRelationList;
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
                ChooseMemberPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        form.add(cancelButton);

        AjaxButton addButton = new AjaxButton(ID_ADD_BUTTON,
                createStringResource("userBrowserDialog.button.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                tabs.forEach(panelTab -> {
                    WebMarkupContainer tabPanel = ((CountablePanelTab)panelTab).getPanel();
                    if (tabPanel == null){
                        return;
                    }

                    MemberPopupTabPanel memberPanel = (MemberPopupTabPanel) tabPanel;
                    executeMemberOperation(memberPanel.getObjectType().getTypeQName(), createInOidQuery(memberPanel.getSelectedObjectsList()),
                           memberPanel.prepareDelta(), target);
                });
                ChooseMemberPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        addButton.setOutputMarkupId(true);
        form.add(addButton);
    }

    protected List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();
        //TODO should we have any authorization here?
        VisibleEnableBehaviour authorization = new VisibleEnableBehaviour(){
        };

        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.USER"), authorization) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new MemberPopupTabPanel(panelId, availableRelationList){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSelectionPerformed(AjaxRequestTarget target){
                        tabLabelPanelUpdate(target);
                    }

                    @Override
                    protected ObjectTypes getObjectType(){
                        return ObjectTypes.USER;
                    }

                    @Override
                    protected T getAbstractRoleTypeObject(){
                        return ChooseMemberPopup.this.getAssignmentTargetRefObject();
                    }
                };
            }

            @Override
            public String getCount() {
                return Integer.toString(getTabPanelSelectedCount(getPanel()));
            }
        });

        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"), authorization) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new MemberPopupTabPanel(panelId, availableRelationList){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSelectionPerformed(AjaxRequestTarget target){
                        tabLabelPanelUpdate(target);
                    }

                    @Override
                    protected ObjectTypes getObjectType(){
                        return ObjectTypes.ROLE;
                    }

                    @Override
                    protected T getAbstractRoleTypeObject(){
                        return ChooseMemberPopup.this.getAssignmentTargetRefObject();
                    }
                };
            }

            @Override
            public String getCount() {
                return Integer.toString(getTabPanelSelectedCount(getPanel()));
            }
        });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new MemberPopupTabPanel(panelId, availableRelationList){
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
                            protected T getAbstractRoleTypeObject(){
                                return ChooseMemberPopup.this.getAssignmentTargetRefObject();
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


        tabs.add(new CountablePanelTab(createStringResource("TypedAssignablePanel.orgTreeView"), authorization) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new OrgTreeMemberPopupTabPanel(panelId, availableRelationList){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected T getAbstractRoleTypeObject(){
                        return ChooseMemberPopup.this.getAssignmentTargetRefObject();
                    }

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
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.SERVICE"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new MemberPopupTabPanel(panelId, availableRelationList){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected T getAbstractRoleTypeObject(){
                                return ChooseMemberPopup.this.getAssignmentTargetRefObject();
                            }

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

        return tabs;
    }

    protected int getTabPanelSelectedCount(WebMarkupContainer panel){
        if (panel != null && panel instanceof MemberPopupTabPanel){
            return ((MemberPopupTabPanel) panel).getSelectedObjectsList().size();
        }
        return 0;
    }

    protected void tabLabelPanelUpdate(AjaxRequestTarget target){
        target.add(getTabbedPanel());
    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    protected ObjectQuery createInOidQuery(List<ObjectType> selectedObjectsList){
        List<String> oids = new ArrayList<>();
        for (Object selectable : selectedObjectsList) {
            oids.add(((ObjectType) selectable).getOid());
        }

        return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
    }

    protected void executeMemberOperation(QName type, ObjectQuery memberQuery,
                                          ObjectDelta delta, AjaxRequestTarget target) {

        Task operationalTask = getPageBase().createSimpleTask("Add.members");
        OperationResult parentResult = operationalTask.getResult();

        try {
            WebComponentUtil.executeMemberOperation(operationalTask, type, memberQuery, delta, TaskCategory.EXECUTE_CHANGES, parentResult, getPageBase());
        } catch (SchemaException e) {
            parentResult.recordFatalError(parentResult.getOperation(), e);
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Failed to execute operation " + parentResult.getOperation(), e);
            target.add(getPageBase().getFeedbackPanel());
        }

        target.add(getPageBase().getFeedbackPanel());
    }

    protected abstract T getAssignmentTargetRefObject();

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
