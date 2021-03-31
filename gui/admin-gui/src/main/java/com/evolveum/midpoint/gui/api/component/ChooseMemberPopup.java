/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.roles.AvailableRelationDto;
import com.evolveum.midpoint.web.page.admin.roles.MemberOperationsHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
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
    protected AvailableRelationDto availableRelationList;

    public ChooseMemberPopup(String id, AvailableRelationDto availableRelationList){
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
                boolean orgPanelProcessed = false;
                for (ITab panelTab : tabs){
                    WebMarkupContainer tabPanel = ((CountablePanelTab)panelTab).getPanel();
                    if (tabPanel == null){
                        continue;
                    }
                    MemberPopupTabPanel memberPanel = (MemberPopupTabPanel) tabPanel;
                    if (memberPanel.getObjectType().equals(ObjectTypes.ORG) && orgPanelProcessed){
                        continue;
                    }
                    List<ObjectType> selectedObjects = memberPanel.getObjectType().equals(ObjectTypes.ORG) ? memberPanel.getPreselectedObjects() :
                            memberPanel.getSelectedObjectsList();

                    if (selectedObjects == null || selectedObjects.size() == 0){
                        continue;
                    }
                    executeMemberOperation(memberPanel.getAbstractRoleTypeObject(),
                            createInOidQuery(selectedObjects), memberPanel.getRelationValue(),
                            memberPanel.getObjectType().getTypeQName(), target, getPageBase());
                    if (memberPanel.getObjectType().equals(ObjectTypes.ORG)){
                        orgPanelProcessed = true;
                    }
                }
                ChooseMemberPopup.this.getPageBase().hideMainPopup(target);
            }
        };
        addButton.add(AttributeAppender.append("title", getAddButtonTitleModel()));
        addButton.add(new EnableBehaviour(() -> isAddButtonEnabled()));
        addButton.setOutputMarkupId(true);
        form.add(addButton);
    }

    protected List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();
        List<QName> objectTypes = getAvailableObjectTypes();
        List<ObjectReferenceType> archetypeRefList = getArchetypeRefList();
        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.USER"),
                new VisibleBehaviour(() -> objectTypes == null || objectTypes.contains(UserType.COMPLEX_TYPE))) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new MemberPopupTabPanel<UserType>(panelId, availableRelationList, archetypeRefList){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<UserType>> rowModel){
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

        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"),
                new VisibleBehaviour(() -> objectTypes == null || objectTypes.contains(RoleType.COMPLEX_TYPE))) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new MemberPopupTabPanel<RoleType>(panelId, availableRelationList, archetypeRefList){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<RoleType>> rowModel){
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
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"),
                        new VisibleBehaviour(() -> objectTypes == null || objectTypes.contains(OrgType.COMPLEX_TYPE))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new MemberPopupTabPanel<OrgType>(panelId, availableRelationList, archetypeRefList){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel){
                                selectedOrgsListUpdate(rowModel);
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

        if (archetypeRefList == null || archetypeRefList.isEmpty()) {
            tabs.add(new CountablePanelTab(createStringResource("TypedAssignablePanel.orgTreeView"),
                    new VisibleBehaviour(() -> isOrgTreeVisible() && (objectTypes == null || objectTypes.contains(OrgType.COMPLEX_TYPE)))) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new OrgTreeMemberPopupTabPanel(panelId, availableRelationList, archetypeRefList) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected T getAbstractRoleTypeObject() {
                            return ChooseMemberPopup.this.getAssignmentTargetRefObject();
                        }

                        @Override
                        protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel) {
                            selectedOrgsListUpdate(rowModel);
                            tabLabelPanelUpdate(target);
                        }

                        @Override
                        protected List<OrgType> getPreselectedObjects() {
                            return selectedOrgsList;
                        }
                    };
                }

                @Override
                public String getCount() {
                    return Integer.toString(selectedOrgsList.size());
                }
            });
        }

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.SERVICE"),
                        new VisibleBehaviour(() -> objectTypes == null || objectTypes.contains(ServiceType.COMPLEX_TYPE))) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new MemberPopupTabPanel<ServiceType>(panelId, availableRelationList, archetypeRefList){
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
                            protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<ServiceType>> rowModel){
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

    protected List<QName> getAvailableObjectTypes(){
        return null;
    }

    protected List<ObjectReferenceType> getArchetypeRefList(){
        return null;
    }

    protected int getTabPanelSelectedCount(WebMarkupContainer panel){
        if (panel != null && panel instanceof MemberPopupTabPanel){
            return ((MemberPopupTabPanel) panel).getSelectedObjectsList().size();
        }
        return 0;
    }

    protected void tabLabelPanelUpdate(AjaxRequestTarget target){
        getTabbedPanel().reloadCountLabels(target);
        target.add(get(ID_FORM).get(ID_ADD_BUTTON));

    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    protected ObjectQuery createInOidQuery(List<ObjectType> selectedObjectsList){
        List<String> oids = new ArrayList<>();
        for (Object selectable : selectedObjectsList) {
            oids.add(((ObjectType) selectable).getOid());
        }

        return getPrismContext().queryFactory().createQuery(getPrismContext().queryFactory().createInOid(oids));
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

    private IModel<String> getAddButtonTitleModel(){
        return new LoadableModel<String>(true) {
            @Override
            protected String load() {
                return !isAddButtonEnabled() ? createStringResource("AssignmentPopup.addButtonTitle").getString() : "";
            }
        };
    }

    private boolean isAddButtonEnabled(){
        TabbedPanel tabbedPanel = getTabbedPanel();
        List<ITab> tabs = (List<ITab>) tabbedPanel.getTabs().getObject();
        for (ITab tab : tabs){
            WebMarkupContainer memberPanel = ((CountablePanelTab)tab).getPanel();
            if (memberPanel == null){
                continue;
            }
            if (((MemberPopupTabPanel) memberPanel).getSelectedObjectsList().size() > 0) {
                return true;
            }
        }
        return false;
    }

    protected void executeMemberOperation(AbstractRoleType targetObject, ObjectQuery query,
            QName relation, QName type, AjaxRequestTarget target, PageBase pageBase) {
        MemberOperationsHelper.assignMembersPerformed(targetObject, query,
                relation, type, target, pageBase);
    }

    protected boolean isOrgTreeVisible(){
        return true;
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

    protected QName getDefaultTargetType() {
        return RoleType.COMPLEX_TYPE;
    }
}
