package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.component.AssignmentConflictPanel;
import com.evolveum.midpoint.web.page.self.dto.AssignmentConflictDto;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/self/assignmentsConflicts", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENTS_CONFLICTS_URL,
                label = "PageAssignmentShoppingKart.auth.assignmentsConflicts.label",
                description = "PageAssignmentShoppingKart.auth.assignmentsConflicts.description")})
public class PageAssignmentConflicts extends PageSelf {
    private static final String ID_CONFLICTS_PANEL = "conflictsPanel";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_BACK = "back";
    private static final String ID_SUBMIT = "submit";
    private Map<String, FocusType> loadedObjectsMap = new HashMap<>();

    public PageAssignmentConflicts() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        RepeatingView conflictsPanel = new RepeatingView(ID_CONFLICTS_PANEL);
        conflictsPanel.setOutputMarkupId(true);

        List<ConflictDto> conflicts = getSessionStorage().getRoleCatalog().getConflictsList();
        for (ConflictDto dto : conflicts) {
            AssignmentConflictPanel panel = new AssignmentConflictPanel(conflictsPanel.newChildId(), Model.of(dto));
            conflictsPanel.add(panel);
        }
        mainForm.add(conflictsPanel);

        AjaxSubmitButton back = new AjaxSubmitButton(ID_BACK, createStringResource("PageAssignmentConflicts.back")) {

            @Override
            public void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                redirectBack();
            }

        };
        mainForm.add(back);

        AjaxSubmitButton submit = new AjaxSubmitButton(ID_SUBMIT, createStringResource("PageAssignmentConflicts.submit")) {

            @Override
            public void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                processConflictDecisions();
                redirectBack();
            }

        };
        mainForm.add(submit);

    }

    private void processConflictDecisions(){
        List<ConflictDto> conflictsList = getSessionStorage().getRoleCatalog().getConflictsList();
        for (ConflictDto conflictDto : conflictsList){
            if (!conflictDto.isResolved()){
                continue;
            }
            if (conflictDto.getAssignment1().isResolved() && !conflictDto.getAssignment1().isOldAssignment()){
                deselectNewAssignment(conflictDto.getAssignment1().getAssignmentTargetObject().getOid());
            } else if (conflictDto.getAssignment2().isResolved() && !conflictDto.getAssignment2().isOldAssignment()){
                deselectNewAssignment(conflictDto.getAssignment2().getAssignmentTargetObject().getOid());
            }
        }
    }

    private void deselectNewAssignment(String oid){
        if (oid == null){
            return;
        }
        List<AssignmentEditorDto> assignmentsList = getSessionStorage().getRoleCatalog().getAssignmentShoppingCart();
        Iterator<AssignmentEditorDto> it = assignmentsList.iterator();
        while (it.hasNext()){
            AssignmentEditorDto assignment = it.next();
            if (oid.equals(assignment.getTargetRef().getOid())){
                it.remove();
            }
        }
    }
}
