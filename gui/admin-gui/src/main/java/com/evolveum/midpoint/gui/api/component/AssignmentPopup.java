package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
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
                addPerformed(target);

            }
        };
        addButton.setOutputMarkupId(true);
        form.add(addButton);
    }

    private List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();
        VisibleEnableBehaviour authorization = new VisibleEnableBehaviour(){
        };


        List<O> selectedRoles = new ArrayList<>();
        IModel<List<O>> selectedRolesModel = new IModel<List<O>>() {
            @Override
            public List<O> getObject() {
                return selectedRoles;
            }

            @Override
            public void setObject(List<O> os) {
                selectedRoles.clear();
                selectedRoles.addAll(os);
            }

            @Override
            public void detach() {

            }
        };
        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.ROLE, selectedRolesModel){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }
                        };
                    }

                    @Override
                    public String getCount() {
                        List selectedObjectsList = ((FocusTypeAssignmentPopupTabPanel)this.getPanel()).getObjectListPanel().getSelectedObjects();
                        return Integer.toString(selectedObjectsList.size());
                    }
                });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.ORG, Model.ofList(new ArrayList<>())){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }

                        };
                    }

                    @Override
                    public String getCount() {
                        List selectedObjectsList = ((FocusTypeAssignmentPopupTabPanel)this.getPanel()).getObjectListPanel().getSelectedObjects();
                        return Integer.toString(selectedObjectsList.size());
                    }
                });

        tabs.add(
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.SERVICE"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.SERVICE, Model.ofList(new ArrayList<>())){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
                            }

                        };
                    }

                    @Override
                    public String getCount() {
                        List selectedObjectsList = ((FocusTypeAssignmentPopupTabPanel)this.getPanel()).getObjectListPanel().getSelectedObjects();
                        return Integer.toString(selectedObjectsList.size());
                    }
                });

//        tabs.add(
//                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.RESOURCE"), authorization) {
//
//                    private static final long serialVersionUID = 1L;
//
//                    @Override
//                    public WebMarkupContainer createPanel(String panelId) {
//                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.RESOURCE);
//                    }
//
//                    @Override
//                    public String getCount() {
//                        return "0";
//                    }
//                });

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

    protected void addPerformed(AjaxRequestTarget target) {
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
