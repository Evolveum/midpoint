/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
public class AssignmentPopup extends BasePanel implements Popupable{
    private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
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
        tabPanel.setOutputMarkupPlaceholderTag(true);
        form.add(tabPanel);

        MessagePanel warningMessage = new MessagePanel(ID_WARNING_MESSAGE, MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);

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
                    WebMarkupContainer assignmentPanel = ((PanelTab)panelTab).getPanel();
                    if (assignmentPanel == null){
                        return;
                    }
                    if (AbstractAssignmentPopupTabPanel.class.isAssignableFrom(assignmentPanel.getClass())) {
                        Map<String, AssignmentType> map = (((AbstractAssignmentPopupTabPanel) assignmentPanel).getSelectedAssignmentsMap());
                        map.forEach(selectedAssignmentsMap::putIfAbsent);
                    }
                });
                List<AssignmentType> assignments = new ArrayList<>(selectedAssignmentsMap.values());
                getPageBase().hideMainPopup(target);
                addPerformed(target, assignments);
            }
        };
        addButton.add(AttributeAppender.append("title", getAddButtonTitleModel()));
        addButton.add(new EnableBehaviour(this::isAssignButtonEnabled));
        addButton.setOutputMarkupId(true);
        form.add(addButton);
    }

    protected List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();

        if (isTabVisible(ObjectTypes.ROLE)) {
            tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"),
                    new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ROLE))) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new FocusTypeAssignmentPopupTabPanel<RoleType>(panelId, ObjectTypes.ROLE) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel) {
                            tabLabelPanelUpdate(target);
                        }

                        @Override
                        protected ObjectTypes getObjectType() {
                            return ObjectTypes.ROLE;
                        }

                        @Override
                        protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                            return AssignmentPopup.this.getAssignmentWrapperModel();
                        }

                        @Override
                        protected QName getPredefinedRelation() {
                            return AssignmentPopup.this.getPredefinedRelation();
                        }

                        @Override
                        protected List<ObjectReferenceType> getArchetypeRefList() {
                            return AssignmentPopup.this.getArchetypeRefList();
                        }
                    };
                }

                @Override
                public String getCount() {
                    return Integer.toString(getTabPanelSelectedCount(getPanel()));
                }
            });
        }

        if (isTabVisible(ObjectTypes.ORG)) {
            tabs.add(
                    new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"),
                            new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ORG))) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public WebMarkupContainer createPanel(String panelId) {
                            return new FocusTypeAssignmentPopupTabPanel<OrgType>(panelId, ObjectTypes.ORG) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel) {
                                    selectedOrgsListUpdate(rowModel);
                                    tabLabelPanelUpdate(target);
                                }

                                @Override
                                protected ObjectTypes getObjectType() {
                                    return ObjectTypes.ORG;
                                }

                                @Override
                                protected List<OrgType> getPreselectedObjects() {
                                    return selectedOrgsList;
                                }

                                @Override
                                protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                                    return AssignmentPopup.this.getAssignmentWrapperModel();
                                }

                                @Override
                                protected QName getPredefinedRelation() {
                                    return AssignmentPopup.this.getPredefinedRelation();
                                }

                                @Override
                                protected List<ObjectReferenceType> getArchetypeRefList() {
                                    return AssignmentPopup.this.getArchetypeRefList();
                                }

                                @Override
                                protected ObjectFilter getSubtypeFilter() {
                                    return AssignmentPopup.this.getSubtypeFilter();
                                }
                            };
                        }

                        @Override
                        public String getCount() {
                            return Integer.toString(selectedOrgsList.size());
                        }
                    });
        }

        if (isTabVisible(ObjectTypes.ORG) && isOrgTreeTabVisible()) {
            tabs.add(new CountablePanelTab(createStringResource("TypedAssignablePanel.orgTreeView"),
                    new VisibleBehaviour(() -> isTabVisible(ObjectTypes.ORG) && isOrgTreeTabVisible())) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new OrgTreeAssignmentPopupTabPanel(panelId) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel) {
                            selectedOrgsListUpdate(rowModel);
                            tabLabelPanelUpdate(target);
                        }

                        @Override
                        protected List<OrgType> getPreselectedObjects() {
                            return selectedOrgsList;
                        }

                        @Override
                        protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                            return AssignmentPopup.this.getAssignmentWrapperModel();
                        }

                        @Override
                        protected QName getPredefinedRelation() {
                            return AssignmentPopup.this.getPredefinedRelation();
                        }

                        @Override
                        protected List<ObjectReferenceType> getArchetypeRefList() {
                            return AssignmentPopup.this.getArchetypeRefList();
                        }

                        @Override
                        protected ObjectFilter getSubtypeFilter() {
                            return AssignmentPopup.this.getSubtypeFilter();
                        }
                    };
                }

                @Override
                public String getCount() {
                    return Integer.toString(selectedOrgsList.size());
                }
            });
        }

        if (isTabVisible(ObjectTypes.SERVICE)) {
            tabs.add(
                    new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.SERVICE"),
                            new VisibleBehaviour(() -> isTabVisible(ObjectTypes.SERVICE))) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public WebMarkupContainer createPanel(String panelId) {
                            return new FocusTypeAssignmentPopupTabPanel<ServiceType>(panelId, ObjectTypes.SERVICE) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected ObjectTypes getObjectType() {
                                    return ObjectTypes.SERVICE;
                                }

                                @Override
                                protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<ServiceType>> rowModel) {
                                    tabLabelPanelUpdate(target);
                                }

                                @Override
                                protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                                    return AssignmentPopup.this.getAssignmentWrapperModel();
                                }

                                @Override
                                protected QName getPredefinedRelation() {
                                    return AssignmentPopup.this.getPredefinedRelation();
                                }

                                @Override
                                protected List<ObjectReferenceType> getArchetypeRefList() {
                                    return AssignmentPopup.this.getArchetypeRefList();
                                }
                            };
                        }

                        @Override
                        public String getCount() {
                            return Integer.toString(getTabPanelSelectedCount(getPanel()));
                        }
                    });
        }

        if (isTabVisible(ObjectTypes.RESOURCE)) {
            tabs.add(
                    new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.RESOURCE"),
                            new VisibleBehaviour(() -> isTabVisible(ObjectTypes.RESOURCE))) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public WebMarkupContainer createPanel(String panelId) {
                            return new ResourceTypeAssignmentPopupTabPanel(panelId) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<ResourceType>> rowModel) {
                                    super.onSelectionPerformed(target, rowModel);
                                    tabLabelPanelUpdate(target);
                                }

                                @Override
                                protected boolean isEntitlementAssignment() {
                                    return AssignmentPopup.this.isEntitlementAssignment();
                                }

                                @Override
                                protected List<ObjectReferenceType> getArchetypeRefList() {
                                    return AssignmentPopup.this.getArchetypeRefList();
                                }
                            };
                        }

                        @Override
                        public String getCount() {
                            return Integer.toString(getTabPanelSelectedCount(getPanel()));
                        }
                    });
        }

        return tabs;
    }

    protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel(){
        return null;
    }

    protected List<ObjectReferenceType> getArchetypeRefList(){
        return null;
    }

    protected ObjectFilter getSubtypeFilter(){
        return null;
    }

    private boolean isTabVisible(ObjectTypes objectType){
        List<ObjectTypes> availableObjectTypesList = getAvailableObjectTypesList();
        return availableObjectTypesList == null || availableObjectTypesList.size() == 0 || availableObjectTypesList.contains(objectType);
    }

    protected boolean isOrgTreeTabVisible(){
        return true;
    }

    protected List<ObjectTypes> getAvailableObjectTypesList(){
        return WebComponentUtil.createAssignableTypesList();
    }

    protected QName getPredefinedRelation(){
        return null;
    }

    protected boolean isEntitlementAssignment(){
        return false;
    }

    private int getTabPanelSelectedCount(WebMarkupContainer panel){
        if (panel instanceof AbstractAssignmentPopupTabPanel){
            return ((AbstractAssignmentPopupTabPanel) panel).getSelectedObjectsList().size();
        }
        return 0;
    }

    protected void tabLabelPanelUpdate(AjaxRequestTarget target){
        getTabbedPanel().reloadCountLabels(target);
        target.add(get(ID_FORM).get(ID_ASSIGN_BUTTON));
    }

    private void selectedOrgsListUpdate(IModel<SelectableBean<OrgType>> rowModel){
        if (rowModel == null){
            return;
        }
        if (rowModel.getObject().isSelected()){
            selectedOrgsList.add(rowModel.getObject().getValue());
        } else {
            selectedOrgsList.removeIf((OrgType org) -> org.getOid().equals(rowModel.getObject().getValue().getOid()));
        }
    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    protected void addPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
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
        TabbedPanel<ITab> tabbedPanel = getTabbedPanel();
        List<ITab> tabs = tabbedPanel.getTabs().getObject();
        for (ITab tab : tabs){
            WebMarkupContainer assignmentPanel = ((PanelTab)tab).getPanel();
            if (assignmentPanel == null){
                continue;
            }
            if (((AbstractAssignmentPopupTabPanel) assignmentPanel).getSelectedObjectsList().size() > 0) {
                return true;
            }
        }
        return false;
    }

    protected IModel<String> getWarningMessageModel(){
        return null;
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
