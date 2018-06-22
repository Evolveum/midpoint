package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class AssignmentPopup<O extends ObjectType> extends BasePanel implements Popupable{
    private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_ASSIGN_BUTTON = "assignButton";
    private static final String ID_FORM = "form";

    private FocusTypeAssignmentPopupTabPanel rolesTabPanel;
    private FocusTypeAssignmentPopupTabPanel orgsTabPanel;
    private FocusTypeAssignmentPopupTabPanel servicesTabPanel;
    private FocusTypeAssignmentPopupTabPanel usersTabPanel;
    private ResourceTypeAssignmentPopupTabPanel resourcesTabPanel;


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
//                TypedAssignablePanel.this.assignButtonClicked(target, new ArrayList<>());
            }
        };
        cancelButton.setOutputMarkupId(true);
        form.add(cancelButton);

        AjaxButton addButton = new AjaxButton(ID_ASSIGN_BUTTON,
                createStringResource("userBrowserDialog.button.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<AssignmentType> newAssignmentsList = new ArrayList<>();
                if (rolesTabPanel != null){
                    List<O> selectedRoles = rolesTabPanel.getSelectedObjectsList();
                    QName relation = rolesTabPanel.getRelationValue();
                    selectedRoles.forEach(selectedRole -> {
                        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(selectedRole, relation);
                        AssignmentType newAssignment = new AssignmentType();
                        newAssignment.setTargetRef(ref);
                        newAssignmentsList.add(newAssignment);
                    });
                }
                if (orgsTabPanel != null){
                    List<O> selectedOrgs = orgsTabPanel.getSelectedObjectsList();
                    QName relation = orgsTabPanel.getRelationValue();
                    selectedOrgs.forEach(selectedOrg -> {
                        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(selectedOrg, relation);
                        AssignmentType newAssignment = new AssignmentType();
                        newAssignment.setTargetRef(ref);
                        newAssignmentsList.add(newAssignment);
                    });
                }
                if (servicesTabPanel != null){
                    List<O> selectedServices = servicesTabPanel.getSelectedObjectsList();
                    QName relation = servicesTabPanel.getRelationValue();
                    selectedServices.forEach(selectedService -> {
                        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(selectedService, relation);
                        AssignmentType newAssignment = new AssignmentType();
                        newAssignment.setTargetRef(ref);
                        newAssignmentsList.add(newAssignment);
                    });
                }
                if (resourcesTabPanel != null){
                    List<ResourceType> selectedResourcces = resourcesTabPanel.getSelectedObjectsList();
                    String intent = resourcesTabPanel.getIntentValue();
                    ShadowKindType kind = resourcesTabPanel.getKindValue();
                    selectedResourcces.forEach(selectedResource -> {
                        ConstructionType constructionType = new ConstructionType();
                        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(selectedResource);
                        constructionType.setResourceRef(ref);
                        constructionType.setKind(kind);
                        constructionType.setIntent(intent);

                        AssignmentType newAssignment = new AssignmentType();
                        newAssignment.setConstruction(constructionType);
                        newAssignmentsList.add(newAssignment);
                    });
                }
                addPerformed(target, newAssignmentsList);

            }
        };
        addButton.setOutputMarkupId(true);
        form.add(addButton);
    }

    private List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();
        VisibleEnableBehaviour authorization = new VisibleEnableBehaviour(){
        };

        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        rolesTabPanel = new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.ROLE){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }
                        };
                        return rolesTabPanel;
                    }

                    @Override
                    public String getCount() {
                        if (rolesTabPanel == null){
                            return "0";
                        }
                        return Integer.toString(rolesTabPanel.getObjectListPanel().getSelectedObjectsCount());
                    }
                });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        orgsTabPanel = new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.ORG){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }

                        };
                        return orgsTabPanel;
                    }

                    @Override
                    public String getCount() {
                        if (orgsTabPanel == null){
                            return "0";
                        }
                        return Integer.toString(orgsTabPanel.getObjectListPanel().getSelectedObjectsCount());
                    }
                });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.SERVICE"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        servicesTabPanel = new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.SERVICE){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }

                        };
                        return servicesTabPanel;
                    }

                    @Override
                    public String getCount() {
                        if (servicesTabPanel == null){
                            return "0";
                        }
                        return Integer.toString(servicesTabPanel.getObjectListPanel().getSelectedObjectsCount());
                    }
                });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.RESOURCE"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        resourcesTabPanel = new ResourceTypeAssignmentPopupTabPanel(panelId){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                super.onSelectionPerformed(target);
                                tabLabelPanelUpdate(target);
                            }
                        };
                        return resourcesTabPanel;
                    }

                    @Override
                    public String getCount() {
                        if (resourcesTabPanel == null){
                            return "0";
                        }
                        return Integer.toString(resourcesTabPanel.getObjectListPanel().getSelectedObjectsCount());
                    }
                });

        return tabs;
    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    private void tabLabelPanelUpdate(AjaxRequestTarget target){
//        TabbedPanel tabbedPanel = getTabbedPanel();
//        Loop tabs = tabbedPanel.getTabsPanel();
//        tabs.forEach(tabPanel -> {
//            target.add(tabPanel.get(tabbedPanel.getTabLinkPanelId()));
//        });

        target.add(getTabbedPanel());
    }

    protected void addPerformed(AjaxRequestTarget target, List newAssignmentsList) {
        getPageBase().hideMainPopup(target);
    }

    public int getWidth(){
        return 900;
    }

    public int getHeight(){
        return 1200;
    }

    public StringResourceModel getTitle(){
        return createStringResource("TypedAssignablePanel.selectObjects");
    }

    public Component getComponent(){
        return this;
    }
}
