/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.TypedAssignablePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author shood
 */
public class AssignmentTablePanel<T extends ObjectType> extends AbstractAssignmentListPanel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentTablePanel.class);

    private static final String DOT_CLASS = AssignmentTablePanel.class.getName() + ".";

    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_CHECK_ALL = "assignmentsCheckAll";
    private static final String ID_HEADER = "assignmentsHeader";
    private static final String ID_MENU = "assignmentsMenu";
    private static final String ID_LIST = "assignmentList";
    protected static final String ID_ROW = "assignmentEditor";


    public AssignmentTablePanel(String id, IModel<List<AssignmentEditorDto>> assignmentModel) {
        super(id, assignmentModel);
    }

    public List<AssignmentType> getAssignmentTypeList() {
        return null;
    }

    public String getExcludeOid() {
        return null;
    }

    public IModel<String> getLabel() {
        return new Model<>("label");
    }



    @Override
    protected void onInitialize() {
        super.onInitialize();

        final WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
        assignments.setOutputMarkupId(true);
        add(assignments);

        Label label = new Label(ID_HEADER, getLabel());
        assignments.add(label);

        DropdownButtonDto model = new DropdownButtonDto(null, null, null, createAssignmentMenu());
        DropdownButtonPanel assignmentMenu = new DropdownButtonPanel(ID_MENU, model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return "btn btn-default";
            }
        };
        assignmentMenu.setVisible(getAssignmentMenuVisibility());
        assignments.add(assignmentMenu);

        ListView<AssignmentEditorDto> list = new ListView<AssignmentEditorDto>(ID_LIST, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<AssignmentEditorDto> item) {
                AssignmentTablePanel.this.populateAssignmentDetailsPanel(item);
            }
        };
        list.setOutputMarkupId(true);
        assignments.add(list);

        AjaxCheckBox checkAll = new AjaxCheckBox(ID_CHECK_ALL, new Model()) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                List<AssignmentEditorDto> assignmentsList = getAssignmentModel().getObject();

                for (AssignmentEditorDto dto : assignmentsList) {
                    dto.setSelected(this.getModelObject());
                }

                target.add(assignments);
            }
        };
        checkAll.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                int count = 0;
                for (AssignmentEditorDto dto : getModelObject()){
                    if (dto.isSimpleView()){
                        count++;
                    }
                }
                return count != getModelObject().size();
            }
        });
        assignments.add(checkAll);

    }

    protected void populateAssignmentDetailsPanel(ListItem<AssignmentEditorDto> item){
        AssignmentEditorPanel editor = new AssignmentEditorPanel(ID_ROW, item.getModel()){
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean ignoreMandatoryAttributes(){
                return AssignmentTablePanel.this.ignoreMandatoryAttributes();
            }

            @Override
            protected boolean isRelationEditable(){
                return AssignmentTablePanel.this.isRelationEditable();
            }

            @Override
            protected void removeButtonClickPerformed(AssignmentEditorDto assignmentDto, AjaxRequestTarget target){
                deleteAssignmentPerformed(target, assignmentDto);
            }

        };
        item.add(editor);

        editor.add(getClassModifier(item));
    }

    protected AttributeModifier getClassModifier(ListItem<AssignmentEditorDto> item){
        return AttributeModifier.append("class", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                AssignmentEditorDto dto = item.getModel().getObject();
                ObjectReferenceType targetRef = dto.getTargetRef();
                if (targetRef != null && targetRef.getType() != null) {
                    return WebComponentUtil.getBoxThinCssClasses(targetRef.getType());
                } else {
                    return GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_THIN_CSS_CLASSES;
                }
            }
        });
    }

    protected List<InlineMenuItem> createAssignmentMenu() {
        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem item;
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI)) {
            item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.assign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new InlineMenuItemAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            TypedAssignablePanel panel = new TypedAssignablePanel(
                                    getPageBase().getMainPopupBodyId(), RoleType.class) {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected void addPerformed(AjaxRequestTarget target, List selected, QName relation, ShadowKindType kind, String intent) {
                                    super.addPerformed(target, selected, relation, kind, intent);
                                    addSelectedAssignablePerformed(target, selected, relation,
                                            getPageBase().getMainPopup().getId());
                                    reloadMainFormButtons(target);
                                }

                            };
                            panel.setOutputMarkupId(true);
                            getPageBase().showMainPopup(panel, target);
                        }
                    };
                }
            };
            items.add(item);

            item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.assignOrg")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new InlineMenuItemAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            int count = WebModelServiceUtils.countObjects(OrgType.class, null, getPageBase());
                            if (count > 0) {
                                OrgTreeAssignablePanel orgTreePanel = new OrgTreeAssignablePanel(
                                        getPageBase().getMainPopupBodyId(), true) {
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected void assignSelectedOrgPerformed(List<OrgType> selectedOrgs,
                                                                              AjaxRequestTarget target) {
                                        // TODO Auto-generated method stub
                                        addSelectedAssignablePerformed(target, (List) selectedOrgs, WebComponentUtil.getDefaultRelationOrFail(),
                                                getPageBase().getMainPopup().getId());
                                        reloadMainFormButtons(target);
                                    }
                                };
                                orgTreePanel.setOutputMarkupId(true);
                                getPageBase().showMainPopup(orgTreePanel, target);
                            } else {
                                warn(createStringResource("AssignmentTablePanel.menu.assignOrg.noorgs").getString());
                                target.add(getPageBase().getFeedbackPanel());
                            }

                        }
                    };
                }
            };
            items.add(item);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI)) {
            item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.unassign")){
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new InlineMenuItemAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            AssignmentTablePanel.this.deleteAssignmentPerformed(target, null);
                        }
                    };
                }
            };
            items.add(item);
        }
        if (isShowAllAssignmentsVisible()) {
            item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.showAllAssignments")){
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new InlineMenuItemAction() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            showAllAssignments(target);
                        }
                    };
                }
            };
            items.add(item);
        }
        return items;
    }

    protected boolean isRelationEditable(){
        return true;
    }

    protected void showAllAssignments(AjaxRequestTarget target) {

    }

    @Override
    protected void reloadMainAssignmentsComponent(AjaxRequestTarget target){
        target.add(get(ID_ASSIGNMENTS));
    }

    protected boolean isShowAllAssignmentsVisible(){
        return false;
    }

    protected void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignments,
            QName relation, String popupId) {
        ModalDialog window = (ModalDialog) get(popupId);
        if (window != null) {
            window.close(target);
        }
        getPageBase().hideMainPopup(target);
        if (newAssignments.isEmpty()) {
            warn(getNoAssignmentsSelectedMessage());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        List<AssignmentEditorDto> assignments = getAssignmentModel().getObject();
        for (ObjectType object : newAssignments) {
            assignments.add(createAssignmentFromSelectedObjects(object, relation));
        }
        reloadAssignmentsPanel(target);
    }

    protected void reloadAssignmentsPanel(AjaxRequestTarget target){
        target.add(getPageBase().getFeedbackPanel(), get(ID_ASSIGNMENTS));
    }

    /**
     * Override to provide handle operation for partial error during provider
     * iterator operation.
     */
    protected void handlePartialError(OperationResult result) {
    }

    protected boolean getAssignmentMenuVisibility(){
        return true;
    }

    protected boolean ignoreMandatoryAttributes(){
        return false;
    }


    protected void reloadMainFormButtons(AjaxRequestTarget target){
//        AbstractObjectMainPanel panel = AbstractAssignmentListPanel.this.findParent(AbstractObjectMainPanel.class);
//        if (panel != null){
//            panel.reloadSavePreviewButtons(target);
//        }
    }
}
