/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  @author shood
 * */
public class AssignmentTablePanel extends SimplePanel<AssignmentTableDto>{

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentTablePanel.class);

    private static final String DOT_CLASS = AssignmentTablePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";

    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_CHECK_ALL = "assignmentsCheckAll";
    private static final String ID_HEADER = "assignmentsHeader";
    private static final String ID_MENU = "assignmentsMenu";
    private static final String ID_LIST = "assignmentList";
    private static final String ID_ROW = "assignmentEditor";
    private static final String ID_MODAL_ASSIGN = "assignablePopup";
    private static final String ID_MODAL_DELETE_ASSIGNMENT = "deleteAssignmentPopup";

    IModel<List<AssignmentEditorDto>> assignmentModel;

    public AssignmentTablePanel(String id, IModel<AssignmentTableDto> model, IModel<String> label){
        super(id, model);

        assignmentModel = new LoadableModel<List<AssignmentEditorDto>>() {

            @Override
            protected List<AssignmentEditorDto> load() {
                return loadFromAssignmentTypeList(getAssignmentTypeList(), new OperationResult(OPERATION_LOAD_ASSIGNMENTS));
            }
        };

        initPanelLayout(label);
    }

    public List<AssignmentType> getAssignmentTypeList(){
        return null;
    }

    public List<AssignmentEditorDto> loadFromAssignmentTypeList(List<AssignmentType> asgList, OperationResult result){
        List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

        for (AssignmentType assignment : asgList) {
            ObjectType targetObject = null;
            AssignmentEditorDtoType type = AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION;
            if (assignment.getTarget() != null) {
                // object assignment
                targetObject = assignment.getTarget();
                type = AssignmentEditorDtoType.getType(targetObject.getClass());
            } else if (assignment.getTargetRef() != null) {
                // object assignment through reference
                ObjectReferenceType ref = assignment.getTargetRef();
                PrismObject target = getReference(ref, result);

                if (target != null) {
                    targetObject = (ObjectType) target.asObjectable();
                    type = AssignmentEditorDtoType.getType(target.getCompileTimeClass());
                }
            } else if (assignment.getConstruction() != null) {
                // account assignment through account construction
                ConstructionType construction = assignment.getConstruction();
                if (construction.getResource() != null) {
                    targetObject = construction.getResource();
                } else if (construction.getResourceRef() != null) {
                    ObjectReferenceType ref = construction.getResourceRef();
                    PrismObject target = getReference(ref, result);
                    if (target != null) {
                        targetObject = (ObjectType) target.asObjectable();
                    }
                }
            }

            list.add(new AssignmentEditorDto(targetObject, type, UserDtoStatus.MODIFY, assignment));
        }

        Collections.sort(list);

        return list;
    }

    private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
        subResult.addParam("targetRef", ref.getOid());
        PrismObject target = null;
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
            Class type = ObjectType.class;
            if (ref.getType() != null) {
                type = getPageBase().getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
            }
            target = getPageBase().getModelService().getObject(type, ref.getOid(), null, task, subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
            subResult.recordFatalError("Couldn't get assignment target ref.", ex);
        }

        return target;
    }

    private void initPanelLayout(IModel<String> labelModel){
        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
        assignments.setOutputMarkupId(true);
        add(assignments);

        Label label = new Label(ID_HEADER, labelModel);
        assignments.add(label);

        InlineMenu assignmentMenu = new InlineMenu(ID_MENU, new Model((Serializable) createAssignmentMenu()));
        assignments.add(assignmentMenu);

        final ListView<AssignmentEditorDto> list = new ListView<AssignmentEditorDto>(ID_LIST, assignmentModel) {

            @Override
                protected void populateItem(ListItem<AssignmentEditorDto> item) {
                AssignmentEditorPanel editor = new AssignmentEditorPanel(ID_ROW, item.getModel());
                item.add(editor);
            }
        };
        list.setOutputMarkupId(true);
        assignments.add(list);

        AjaxCheckBox checkAll = new AjaxCheckBox(ID_CHECK_ALL, new Model<Boolean>()) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //TODO
            }
        };
        assignments.add(checkAll);
    }

    private List<InlineMenuItem> createAssignmentMenu(){
        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

        InlineMenuItem item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.assign"),
                new InlineMenuItemAction(){

                    @Override
                    public void onClick(AjaxRequestTarget target){
                        showAssignablePopupPerformed(target, ResourceType.class);
                    }
                });
        items.add(item);

        item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.assignRole"),
                new InlineMenuItemAction(){

                    @Override
                    public void onClick(AjaxRequestTarget target){
                        showAssignablePopupPerformed(target, RoleType.class);
                    }
                });
        items.add(item);

        item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.assignOrg"),
                new InlineMenuItemAction(){

                    @Override
                    public void onClick(AjaxRequestTarget target){
                        showAssignablePopupPerformed(target, OrgType.class);
                    }
                });
        items.add(item);

        items.add(new InlineMenuItem());

        item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.unassign"), new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAssignmentPerformed(target);
            }
        });
        items.add(item);

        return items;
    }

    private void showAssignablePopupPerformed(AjaxRequestTarget target, Class<? extends ObjectType> type){
        //TODO
    }

    private void deleteAssignmentPerformed(AjaxRequestTarget target){
        //TODO
    }
}
