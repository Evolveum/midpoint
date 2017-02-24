package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.dto.AssignmentConflictDto;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by honchar.
 */
public class PageAssignmentsList<F extends FocusType> extends PageBase{
    private static final String ID_ASSIGNMENT_TABLE_PANEL = "assignmentTablePanel";
    private static final String ID_FORM = "mainForm";
    private static final String ID_BACK = "back";
    private static final String ID_REQUEST_BUTTON = "request";
    private static final String ID_RESOLVE_CONFLICTS_BUTTON = "resolveConflicts";
    private static final String ID_DESCRIPTION = "description";

    private static final Trace LOGGER = TraceManager.getTrace(PageRequestRole.class);
    private static final String DOT_CLASS = PageAssignmentsList.class.getName() + ".";
    private static final String OPERATION_REQUEST_ASSIGNMENTS = DOT_CLASS + "requestAssignments";
    private static final String OPERATION_WF_TASK_CREATED = "com.evolveum.midpoint.wf.impl.WfHook.invoke";
    private static final String OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS = "reviewAssignmentConflicts";

    private IModel<List<AssignmentEditorDto>> assignmentsModel;
    private PrismObject<UserType> user;
    private OperationResult backgroundTaskOperationResult = null;
    IModel<String> descriptionModel = new Model<String>("");

    public PageAssignmentsList() {
        this(false);
    }

    public PageAssignmentsList(boolean setConflictsToSession){
        user = loadUser();
        initAssignmentsModel();
        if (setConflictsToSession) {
            getSessionStorage().getRoleCatalog().setConflictsList(getAssignmentConflicts());
        }
        initLayout();
    }

    private void initAssignmentsModel() {
        assignmentsModel = Model.ofList(getSessionStorage().getRoleCatalog().getAssignmentShoppingCart());
    }

    public void initLayout() {
        setOutputMarkupId(true);

        Form mainForm = new Form(ID_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        AssignmentTablePanel panel = new AssignmentTablePanel(ID_ASSIGNMENT_TABLE_PANEL,
                createStringResource("FocusType.assignment"), assignmentsModel){
            @Override
            protected List<InlineMenuItem> createAssignmentMenu() {
                List<InlineMenuItem> items = new ArrayList<>();
                InlineMenuItem item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.unassign"),
                        new InlineMenuItemAction() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                deleteAssignmentPerformed(target);
                            }
                        });
                items.add(item);
                return items;

            }
        };
        mainForm.add(panel);

        mainForm.add(new TextArea<String>(ID_DESCRIPTION, descriptionModel));

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageAssignmentDetails.backButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }

        };
        mainForm.add(back);

        AjaxSubmitButton requestAssignments = new AjaxSubmitButton(ID_REQUEST_BUTTON, createStringResource("PageAssignmentsList.requestButton")) {

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                onRequestPerformed(target);
                SessionStorage storage = getSessionStorage();
                storage.getRoleCatalog().getAssignmentShoppingCart().clear();
            }

        };
        requestAssignments.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isEnabled(){
                return areConflictsResolved();
            }
        });
        mainForm.add(requestAssignments);

        AjaxSubmitButton resolveAssignments = new AjaxSubmitButton(ID_RESOLVE_CONFLICTS_BUTTON,
                createStringResource("PageAssignmentsList.resolveConflicts")) {

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                PageAssignmentsList.this.navigateToNext(PageAssignmentConflicts.class);
            }

        };
        mainForm.add(resolveAssignments);

    }

    private void onRequestPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_REQUEST_ASSIGNMENTS);
        ObjectDelta<UserType> delta;
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        try {
            delta = user.createModifyDelta();
            deltas.add(delta);
            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
            handleAssignmentDeltas(delta, addAssignmentsToUser(), def);

            OperationBusinessContextType businessContextType = new OperationBusinessContextType();
            businessContextType.setComment(descriptionModel.getObject() != null ?
                    descriptionModel.getObject() : "");

            getModelService().executeChanges(deltas, ModelExecuteOptions.createRequestBusinessContext(businessContextType),
                    createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS), result);

            result.recordSuccess();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save assignments ", e);
            error("Could not save assignments. Reason: " + e);
            target.add(getFeedbackPanel());
        } finally {
            result.recomputeStatus();
        }

        findBackgroundTaskOperation(result);
        if (backgroundTaskOperationResult != null
                && StringUtils.isNotEmpty(backgroundTaskOperationResult.getBackgroundTaskOid())){
            result.setMessage(createStringResource("operation.com.evolveum.midpoint.web.page.self.PageRequestRole.taskCreated").getString());
            showResult(result);
            setResponsePage(PageAssignmentShoppingKart.class);
            return;
        }
        showResult(result);
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            target.add(getFeedbackPanel());
        } else {
            setResponsePage(PageAssignmentShoppingKart.class);
        }
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> focusDelta,
                                                  List<AssignmentEditorDto> assignments, PrismContainerDefinition def) throws SchemaException {
        ContainerDelta assDelta = new ContainerDelta(new ItemPath(), def.getName(), def, getPrismContext());

        for (AssignmentEditorDto assDto : assignments) {
            PrismContainerValue newValue = assDto.getNewValue(getPrismContext());

            switch (assDto.getStatus()) {
                case ADD:
                    newValue.applyDefinition(def, false);
                    assDelta.addValueToAdd(newValue.clone());
                    break;
                case DELETE:
                    PrismContainerValue oldValue = assDto.getOldValue();
                    oldValue.applyDefinition(def);
                    assDelta.addValueToDelete(oldValue.clone());
                    break;
                case MODIFY:
                    if (!assDto.isModified(getPrismContext())) {
                        LOGGER.trace("Assignment '{}' not modified.", new Object[]{assDto.getName()});
                        continue;
                    }

                    handleModifyAssignmentDelta(assDto, def, newValue, focusDelta);
                    break;
                default:
                    warn(getString("pageAdminUser.message.illegalAssignmentState", assDto.getStatus()));
            }
        }

        if (!assDelta.isEmpty()) {
            assDelta = focusDelta.addModification(assDelta);
        }

        return assDelta;
    }


    private void findBackgroundTaskOperation(OperationResult result){
        if (backgroundTaskOperationResult != null) {
            return;
        } else {
            List<OperationResult> subresults = result.getSubresults();
            if (subresults == null || subresults.size() == 0) {
                return;
            }
            for (OperationResult subresult : subresults) {
                if (subresult.getOperation().equals(OPERATION_WF_TASK_CREATED)) {
                    backgroundTaskOperationResult = subresult;
                    return;
                } else {
                    findBackgroundTaskOperation(subresult);
                }
            }
        }
        return;
    }

    private List<String> getAssignmentsOidsList(){
        List<String> oidsList = new ArrayList<>();
        for (AssignmentType assignment : user.asObjectable().getAssignment()){
            if (assignment.getTargetRef() != null && assignment.getTargetRef().getOid() != null){
                oidsList.add(assignment.getTargetRef().getOid());
            }
        }
        return oidsList;
    }

    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
                                             PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<UserType> focusDelta)
            throws SchemaException {
        LOGGER.debug("Handling modified assignment '{}', computing delta.",
                new Object[]{assDto.getName()});

        PrismValue oldValue = assDto.getOldValue();
        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

        for (ItemDelta delta : deltas) {
            ItemPath deltaPath = delta.getPath().rest();
            ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

            delta.setParentPath(WebComponentUtil.joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
            delta.applyDefinition(deltaDef);

            focusDelta.addModification(delta);
        }
    }

    private List<AssignmentEditorDto> addAssignmentsToUser(){
        List<AssignmentConflictDto> conflicts = getSessionStorage().getRoleCatalog().getConflictsList();
        List<String> assignmentsToRemove = new ArrayList<>();
        List<String> assignmentsToUnselect = new ArrayList<>();
        for (AssignmentConflictDto dto : conflicts){
            if (dto.isRemovedOld()){
                assignmentsToRemove.add(dto.getExistingAssignmentTargetObj().getOid());
            } else if (dto.isUnassignedNew()){
                assignmentsToUnselect.add(dto.getAddedAssignmentTargetObj().getOid());
            }
        }

        List<AssignmentType> userAssignments = user.asObjectable().getAssignment();
        if (userAssignments == null){
            userAssignments = new ArrayList<>();
        }
        List<AssignmentEditorDto> assignmentsList = new ArrayList<>();
        for (AssignmentType assignment : userAssignments){
            if (assignment.getTargetRef() != null && assignmentsToRemove.contains(assignment.getTargetRef().getOid())){
                assignmentsList.add(new AssignmentEditorDto(UserDtoStatus.DELETE, assignment, this));
            } else {
                assignmentsList.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
            }
        }
        if (assignmentsModel != null && assignmentsModel.getObject() != null) {
            for (AssignmentEditorDto assignmentsToAdd : assignmentsModel.getObject()) {
                if (!assignmentsToUnselect.contains(assignmentsToAdd.getTargetRef().getOid())) {
                    assignmentsToAdd.setStatus(UserDtoStatus.ADD);
                    assignmentsList.add(assignmentsToAdd);
                }
            }
        }
        return assignmentsList;
    }

    private List<AssignmentConflictDto> getAssignmentConflicts(){
        ModelContext<UserType> modelContext = null;

        ObjectDelta<UserType> delta;
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

        OperationResult result = new OperationResult(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        Task task = createSimpleTask(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        List<AssignmentConflictDto> conflictsList = new ArrayList<>();
        try {
            delta = user.createModifyDelta();

            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
            handleAssignmentDeltas(delta, addAssignmentsToUser(), def);

            modelContext = getModelInteractionService()
                    .previewChanges(WebComponentUtil.createDeltaCollection(delta), null, task, result);
            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext
                    .getEvaluatedAssignmentTriple();
            Collection<? extends EvaluatedAssignment> addedAssignments = evaluatedAssignmentTriple
                    .getPlusSet();
            List<String> userAssignmentsOidsList = getAssignmentsOidsList();
            if (addedAssignments != null) {
                for (EvaluatedAssignment<UserType> evaluatedAssignment : addedAssignments) {
                    for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getTargetPolicyRules()) {
                    	if (policyRule.getActions() == null || policyRule.getActions().getApproval() == null) {
                    		continue;
						}
                        for (EvaluatedPolicyRuleTrigger<?> trigger : policyRule.getAllTriggers()) {
                            if (trigger instanceof EvaluatedExclusionTrigger) {
                                PrismObject<F> addedAssignmentTargetObj = (PrismObject<F>)evaluatedAssignment.getTarget();
                                EvaluatedAssignment<F> conflictingAssignment = ((EvaluatedExclusionTrigger) trigger).getConflictingAssignment();
                                PrismObject<F> exclusionTargetObj = (PrismObject<F>)conflictingAssignment.getTarget();
                                String exclusionOid = exclusionTargetObj.getOid();
                                if (userAssignmentsOidsList.contains(exclusionOid)) {
                                    AssignmentConflictDto dto = new AssignmentConflictDto(exclusionTargetObj, addedAssignmentTargetObj);
                                    conflictsList.add(dto);
                                }
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get assignments conflicts. Reason: ", e);
            error("Couldn't get assignments conflicts. Reason: " + e);
        }
        return conflictsList;
    }

    private boolean areConflictsResolved(){
        List<AssignmentConflictDto> list = getSessionStorage().getRoleCatalog().getConflictsList();
        for (AssignmentConflictDto dto : list){
            if (!dto.isError()){
                continue;
            }
            if (!dto.isSolved()){
                return false;
            }
        }
        return true;
    }

    private PrismObject<UserType> loadUser() {
        if (getSessionStorage().getRoleCatalog().getTargetUser() != null){
            return getSessionStorage().getRoleCatalog().getTargetUser();
        } else {
            return loadUserSelf(PageAssignmentsList.this);
        }
    }

    private Component getRequestButton(){
        return get(ID_FORM).get(ID_REQUEST_BUTTON);
    }
}
