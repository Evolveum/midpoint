/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.user.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.DelegationEditorPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shood
 */
@PanelType(name = "delegatedToMe")
@PanelInstance(identifier = "delegatedToMe", applicableForType = UserType.class,
        display = @PanelDisplay(label = "FocusType.delegatedToMe", order = 100))
@Counter(provider = DelegatedToMeCounter.class)
public class DelegatedToMePanel extends AbstractObjectMainPanel<UserType, UserDetailsModel> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DelegatedToMePanel.class);

    private static final String DOT_CLASS = DelegatedToMePanel.class.getName() + ".";

    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_CHECK_ALL = "assignmentsCheckAll";
    private static final String ID_HEADER = "assignmentsHeader";
    private static final String ID_LIST = "assignmentList";
    protected static final String ID_ROW = "assignmentEditor";


    public DelegatedToMePanel(String id, UserDetailsModel userDetailsModel, ContainerPanelConfigurationType config) {
        super(id, userDetailsModel, config);
    }

    public List<AssignmentType> getAssignmentTypeList() {
        return null;
    }

    @Override
    protected void initLayout() {
        final WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
        assignments.setOutputMarkupId(true);
        add(assignments);

        Label label = new Label(ID_HEADER, getLabel());
        assignments.add(label);

        ListView<AssignmentEditorDto> list = new ListView<AssignmentEditorDto>(ID_LIST, getDelegatedToMeModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<AssignmentEditorDto> item) {
                DelegatedToMePanel.this.populateAssignmentDetailsPanel(item);
            }
        };
        list.setOutputMarkupId(true);
        assignments.add(list);

        AjaxCheckBox checkAll = new AjaxCheckBox(ID_CHECK_ALL, new Model()) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                List<AssignmentEditorDto> assignmentsList = getDelegatedToMeModelObject();

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
                for (AssignmentEditorDto dto : getDelegatedToMeModelObject()){
                    if (dto.isSimpleView()){
                        count++;
                    }
                }
                return count != getDelegatedToMeModelObject().size();
            }
        });
        assignments.add(checkAll);
    }

    public void populateAssignmentDetailsPanel(ListItem<AssignmentEditorDto> item) {
        com.evolveum.midpoint.web.component.assignment.DelegationEditorPanel editor = new DelegationEditorPanel(ID_ROW, item.getModel(), true,
                getObjectDetailsModels().getPrivilegesListModel());
        item.add(editor);
    }

    public String getExcludeOid() {
        return getObjectDetailsModels().getObjectType().getOid();
    }

    public IModel<String> getLabel() {
        return getPageBase().createStringResource("FocusType.delegatedToMe");
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

    protected void reloadMainAssignmentsComponent(AjaxRequestTarget target){
        target.add(get(ID_ASSIGNMENTS));
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

    private LoadableModel<List<AssignmentEditorDto>> getDelegatedToMeModel() {
        return getObjectDetailsModels().getDelegatedToMeModel();
    }

    private List<AssignmentEditorDto> getDelegatedToMeModelObject() {
        return getDelegatedToMeModel().getObject();
    }
}
