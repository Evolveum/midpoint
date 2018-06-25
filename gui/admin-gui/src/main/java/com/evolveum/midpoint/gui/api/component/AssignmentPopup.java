package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
                List<AssignmentType> newAssignmentsList = new ArrayList<>();
                List<AssignmentType> newOrgTypeAssignmentsList = new ArrayList<>();

                tabs.forEach(panelTab -> {
                    WebMarkupContainer assignmentPanel = ((CountablePanelTab)panelTab).getPanel();
                    if (assignmentPanel == null){
                        return;
                    }
                    if (assignmentPanel instanceof OrgTypeAssignmentPopupTabPanel){
                        if (newOrgTypeAssignmentsList.isEmpty()) {
                            newOrgTypeAssignmentsList.addAll(((AbstractAssignmentPopupTabPanel) assignmentPanel).getSelectedAssignmentsList());
                            return;
                        } else {
                            return;
                        }
                    }
                    newAssignmentsList.addAll(((AbstractAssignmentPopupTabPanel)assignmentPanel).getSelectedAssignmentsList());
                });
                newAssignmentsList.addAll(newOrgTypeAssignmentsList);
                addPerformed(target, newAssignmentsList);
            }
        };
        addButton.setOutputMarkupId(true);
        form.add(addButton);
    }

    private List<ITab> createAssignmentTabs() {
        List<ITab> tabs = new ArrayList<>();
        //TODO check authorization for each tab
        VisibleEnableBehaviour authorization = new VisibleEnableBehaviour(){
        };

        tabs.add(new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ROLE"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.ROLE){
                            private static final long serialVersionUID = 1L;

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
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.ORG"), authorization) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new OrgTypeAssignmentPopupTabPanel(panelId, false, selectedOrgsList){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void onSelectionPerformed(AjaxRequestTarget target){
                                tabLabelPanelUpdate(target);
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
                return new OrgTypeAssignmentPopupTabPanel(panelId, true, selectedOrgsList){
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onOrgTreeCheckBoxSelectionPerformed(AjaxRequestTarget target){
                        tabLabelPanelUpdate(target);
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
                        return new FocusTypeAssignmentPopupTabPanel(panelId, ObjectTypes.SERVICE){
                            private static final long serialVersionUID = 1L;

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
                new CountablePanelTab(getPageBase().createStringResource("ObjectTypes.RESOURCE"), authorization) {

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

    private int getTabPanelSelectedCount(WebMarkupContainer panel){
        if (panel != null && panel instanceof FocusTypeAssignmentPopupTabPanel){
            return ((FocusTypeAssignmentPopupTabPanel) panel).getSelectedObjectsList().size();
        }
        return 0;
    }

    private TabbedPanel getTabbedPanel(){
        return (TabbedPanel) get(ID_FORM).get(ID_TABS_PANEL);
    }

    private void tabLabelPanelUpdate(AjaxRequestTarget target){
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
