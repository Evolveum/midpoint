package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.TargetUserSelectorComponent;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.dto.AssignmentConflictDto;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/self/requestAssignments", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL,
                label = "PageAssignmentShoppingKart.auth.requestAssignments.label",
                description = "PageAssignmentShoppingKart.auth.requestAssignments.description")})
public class PageAssignmentsList<F extends FocusType> extends PageBase{
    private static final String ID_ASSIGNMENT_TABLE_PANEL = "assignmentTablePanel";
    private static final String ID_FORM = "mainForm";
    private static final String ID_BACK = "back";
    private static final String ID_REQUEST_BUTTON = "request";
    private static final String ID_RESOLVE_CONFLICTS_BUTTON = "resolveConflicts";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_TARGET_USER_PANEL = "targetUserPanel";

    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentsList.class);
    private static final String DOT_CLASS = PageAssignmentsList.class.getName() + ".";
    private static final String OPERATION_REQUEST_ASSIGNMENTS = DOT_CLASS + "requestAssignments";
    private static final String OPERATION_WF_TASK_CREATED = "com.evolveum.midpoint.wf.impl.WfHook.invoke";
    private static final String OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS = "reviewAssignmentConflicts";

    private IModel<List<AssignmentEditorDto>> assignmentsModel;
    private OperationResult backgroundTaskOperationResult = null;
    IModel<String> descriptionModel;

    public PageAssignmentsList(){
        this(false);
    }

    public PageAssignmentsList(boolean loadConflicts){
        initModels();
        if (loadConflicts){
            getSessionStorage().getRoleCatalog().setConflictsList(getAssignmentConflicts());
        }
        initLayout();
    }

    private void initModels() {
        assignmentsModel = Model.ofList(getSessionStorage().getRoleCatalog().getAssignmentShoppingCart());
        descriptionModel = Model.of(getSessionStorage().getRoleCatalog().getRequestDescription());
    }

    public void initLayout() {
        setOutputMarkupId(true);

        Form mainForm = new Form(ID_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        AssignmentTablePanel panel = new AssignmentTablePanel<UserType>(ID_ASSIGNMENT_TABLE_PANEL,
                createStringResource("FocusType.assignment"), assignmentsModel, PageAssignmentsList.this){
            @Override
            protected List<InlineMenuItem> createAssignmentMenu() {
                List<InlineMenuItem> items = new ArrayList<>();
                InlineMenuItem item = new InlineMenuItem(createStringResource("AssignmentTablePanel.menu.unassign"),
                        new InlineMenuItemAction() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                deleteAssignmentPerformed(target, null);
                            }
                        });
                items.add(item);
                return items;
            }
        };
        mainForm.add(panel);

        WebMarkupContainer targetUserPanel = new TargetUserSelectorComponent(ID_TARGET_USER_PANEL, PageAssignmentsList.this);
        targetUserPanel.setOutputMarkupId(true);
        mainForm.add(targetUserPanel);

        TextArea<String> descriptionInput = new TextArea<>(ID_DESCRIPTION, descriptionModel);
        descriptionInput.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getSessionStorage().getRoleCatalog().setRequestDescription(getDescriptionComponent().getValue());
            }
        });
        mainForm.add(descriptionInput);

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
                if (getSessionStorage().getRoleCatalog().getTargetUserList() == null ||
                        getSessionStorage().getRoleCatalog().getTargetUserList().size() <= 1) {
                    onSingleUserRequestPerformed(target);
                } else {
                    onMultiUserRequestPerformed(target);
                }
            }

        };
        requestAssignments.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isEnabled(){
                return (getSessionStorage().getRoleCatalog().isMultiUserRequest() ||
                        onlyWarnings() || areConflictsResolved()) &&
                        getSessionStorage().getRoleCatalog().getAssignmentShoppingCart() != null &&
                        getSessionStorage().getRoleCatalog().getAssignmentShoppingCart().size() > 0;
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
        resolveAssignments.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return !getSessionStorage().getRoleCatalog().isMultiUserRequest()
                        && !areConflictsResolved();
            }
        });
        mainForm.add(resolveAssignments);

    }

    private void onSingleUserRequestPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_REQUEST_ASSIGNMENTS);
        ObjectDelta<UserType> delta;
        try {
            PrismObject<UserType> user = getTargetUser();
            delta = prepareDelta(user, result);

            ModelExecuteOptions options = createOptions();
            options.setInitialPartialProcessing(new PartialProcessingOptionsType().inbound(SKIP).projection(SKIP)); // TODO make this configurable?
            getModelService().executeChanges(Collections.singletonList(delta), options, createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS), result);

            result.recordSuccess();
            SessionStorage storage = getSessionStorage();
            storage.getRoleCatalog().getAssignmentShoppingCart().clear();
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
            clearStorage();
            setResponsePage(PageAssignmentShoppingKart.class);
            return;
        }
        showResult(result);
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            target.add(getFeedbackPanel());
            target.add(PageAssignmentsList.this.get(ID_FORM));
        } else {
            clearStorage();
            setResponsePage(PageAssignmentShoppingKart.class);
        }
    }

    @NotNull
    private ModelExecuteOptions createOptions() {
        OperationBusinessContextType businessContextType;
        if (descriptionModel.getObject() != null) {
            businessContextType = new OperationBusinessContextType();
            businessContextType.setComment(descriptionModel.getObject());
        } else {
            businessContextType = null;
        }
        ModelExecuteOptions options = ExecuteChangeOptionsDto.createFromSystemConfiguration().createOptions();
        options.setRequestBusinessContext(businessContextType);
        return options;
    }

    // TODO initial partial processing options - MID-4059 (but it's not so important here, because the task runs on background)
    private void onMultiUserRequestPerformed(AjaxRequestTarget target) {
            OperationResult result = new OperationResult(OPERATION_REQUEST_ASSIGNMENTS);
            Task operationalTask = createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS);

            try {
                TaskType task = WebComponentUtil.createSingleRecurrenceTask(
                        createStringResource(OPERATION_REQUEST_ASSIGNMENTS).getString(),
                        UserType.COMPLEX_TYPE,
                        getTaskQuery(), prepareDelta(null, result), createOptions(), TaskCategory.EXECUTE_CHANGES, PageAssignmentsList.this);
                WebModelServiceUtils.runTask(task, operationalTask, result, PageAssignmentsList.this);
            } catch (SchemaException e) {
                result.recordFatalError(result.getOperation(), e);
                LoggingUtils.logUnexpectedException(LOGGER,
                        "Failed to execute operaton " + result.getOperation(), e);
                target.add(getFeedbackPanel());
            }
            findBackgroundTaskOperation(result);
            if (backgroundTaskOperationResult != null
                    && StringUtils.isNotEmpty(backgroundTaskOperationResult.getBackgroundTaskOid())) {
                result.setMessage(createStringResource("operation.com.evolveum.midpoint.web.page.self.PageRequestRole.taskCreated").getString());
                showResult(result);
                clearStorage();
                setResponsePage(PageAssignmentShoppingKart.class);
                return;
            }
            if (WebComponentUtil.isSuccessOrHandledError(result)
                    || OperationResultStatus.IN_PROGRESS.equals(result.getStatus())) {
                clearStorage();
                setResponsePage(PageAssignmentShoppingKart.class);
            } else {
                showResult(result);
                target.add(getFeedbackPanel());
                target.add(PageAssignmentsList.this.get(ID_FORM));
            }
    }

    private void clearStorage(){
        SessionStorage storage = getSessionStorage();
        if (storage.getRoleCatalog().getAssignmentShoppingCart() != null) {
            storage.getRoleCatalog().getAssignmentShoppingCart().clear();
        }
        if (storage.getRoleCatalog().getTargetUserList() != null){
            storage.getRoleCatalog().getTargetUserList().clear();
        }
        storage.getRoleCatalog().setRequestDescription("");
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> focusDelta,
                                                  List<AssignmentEditorDto> assignments, PrismContainerDefinition def) throws SchemaException {
        ContainerDelta assDelta = new ContainerDelta(ItemPath.EMPTY_PATH, def.getName(), def, getPrismContext());

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

    private List<ConflictDto> getAssignmentConflicts(){
        ModelContext<UserType> modelContext = null;

        ObjectDelta<UserType> delta;
        OperationResult result = new OperationResult(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        Task task = createSimpleTask(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        List<ConflictDto> conflictsList = new ArrayList<>();
        Map<String, ConflictDto> conflictsMap = new HashedMap();
        try {
            PrismObject<UserType> user = getTargetUser();
            delta = user.createModifyDelta();

            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
            handleAssignmentDeltas(delta, getSessionStorage().getRoleCatalog().getAssignmentShoppingCart(), def);

            PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
            partialProcessing.setInbound(SKIP);
            partialProcessing.setProjection(SKIP);
			ModelExecuteOptions recomputeOptions = ModelExecuteOptions.createPartialProcessing(partialProcessing);
			modelContext = getModelInteractionService()
                    .previewChanges(WebComponentUtil.createDeltaCollection(delta), recomputeOptions, task, result);
            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext
                    .getEvaluatedAssignmentTriple();
            Collection<? extends EvaluatedAssignment> addedAssignments = evaluatedAssignmentTriple
                    .getPlusSet();
            if (addedAssignments != null) {
                for (EvaluatedAssignment<UserType> evaluatedAssignment : addedAssignments) {
                    for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
                        for (EvaluatedPolicyRuleTrigger<?> trigger : policyRule.getAllTriggers()) {
                            if (trigger instanceof EvaluatedExclusionTrigger) {
                                EvaluatedExclusionTrigger exclusionTrigger = (EvaluatedExclusionTrigger) trigger;
                                EvaluatedAssignment<F> conflictingAssignment = exclusionTrigger.getConflictingAssignment();
                                PrismObject<F> addedAssignmentTargetObj = (PrismObject<F>)evaluatedAssignment.getTarget();
                                PrismObject<F> exclusionTargetObj = (PrismObject<F>)conflictingAssignment.getTarget();

                                AssignmentConflictDto<F> dto1 = new AssignmentConflictDto<>(exclusionTargetObj,
                                        conflictingAssignment.getAssignmentType(true) != null);
                                AssignmentConflictDto<F> dto2 = new AssignmentConflictDto<>(addedAssignmentTargetObj,
                                        evaluatedAssignment.getAssignmentType(true) != null);
                                // everything other than 'enforce' is a warning
                                boolean isWarning = policyRule.getActions() == null
                                        || policyRule.getActions().getEnforcement() == null;
                                ConflictDto conflict = new ConflictDto(dto1, dto2, isWarning);
                                String oid1 = exclusionTargetObj.getOid();
                                String oid2 = addedAssignmentTargetObj.getOid();
                                if (!conflictsMap.containsKey(oid1 + oid2) && !conflictsMap.containsKey(oid2 + oid1)) {
                                    conflictsMap.put(oid1 + oid2, conflict);
                                } else if (!isWarning) {
                                    // error is stronger than warning, so we replace (potential) warnings with this error
                                    // TODO Kate please review this
                                    if (conflictsMap.containsKey(oid1 + oid2)) {
                                        conflictsMap.replace(oid1 + oid2, conflict);
                                    }
                                    if (conflictsMap.containsKey(oid2 + oid1)) {
                                        conflictsMap.replace(oid2 + oid1, conflict);
                                    }
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
        conflictsList.addAll(conflictsMap.values());
        return conflictsList;
    }

    private boolean onlyWarnings(){
        List<ConflictDto> list = getSessionStorage().getRoleCatalog().getConflictsList();
        for (ConflictDto dto : list){
            if (!dto.isWarning()){
                return false;
            }
        }
        return true;
    }

    private boolean areConflictsResolved(){
        List<ConflictDto> list = getSessionStorage().getRoleCatalog().getConflictsList();
        for (ConflictDto dto : list){
            if (!dto.isResolved()){
                return false;
            }
        }
        return true;
    }

    private ObjectDelta prepareDelta(PrismObject<UserType> user, OperationResult result) {
        ObjectDelta delta = null;
        try{
            delta = ObjectDelta.createModificationAddContainer(UserType.class, user == null ? "fakeOid" : user.getOid(),
                FocusType.F_ASSIGNMENT, getPrismContext(), getAddAssignmentContainerValues(assignmentsModel.getObject()));
            if (!getSessionStorage().getRoleCatalog().isMultiUserRequest()) {
                delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT,
                        getDeleteAssignmentContainerValues(user.asObjectable()));
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for operation " + OPERATION_REQUEST_ASSIGNMENTS, e);
            result.recordFatalError("Failed to prepare delta for operation " + OPERATION_REQUEST_ASSIGNMENTS, e);
        }
        return delta;

    }

    private ObjectQuery getTaskQuery(){
        List<PrismObject<UserType>> userList = getSessionStorage().getRoleCatalog().getTargetUserList();
        if (getSessionStorage().getRoleCatalog().isSelfRequest()){
            userList = new ArrayList<>();
            userList.add(loadUserSelf());
        }
        Set<String> oids = new HashSet<>();
        for (PrismObject<UserType> user : userList){
            oids.add(user.getOid());
        }
        return ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
    }

    private PrismContainerValue[] getAddAssignmentContainerValues(List<AssignmentEditorDto> assignments) throws SchemaException {

        List<PrismContainerValue<AssignmentType>> addContainerValues = new ArrayList<>();
        for (AssignmentEditorDto assDto : assignments) {
            if (UserDtoStatus.ADD.equals(assDto.getStatus())) {
                addContainerValues.add(assDto.getNewValue(getPrismContext()).clone());      // clone is to eliminate "Attempt to reset value parent ..." exceptions (in some cases)
            }

        }
        return addContainerValues.toArray(new PrismContainerValue[addContainerValues.size()]);
    }

    private PrismContainerValue[] getDeleteAssignmentContainerValues(UserType user) throws SchemaException {
        List<PrismContainerValue<AssignmentType>> deleteAssignmentValues = new ArrayList<>();
        for (AssignmentEditorDto assDto : getAssignmentsToRemoveList(user)) {
            deleteAssignmentValues.add(assDto.getNewValue(getPrismContext()));
        }
        return deleteAssignmentValues.toArray(new PrismContainerValue[deleteAssignmentValues.size()]);
    }

    private List<AssignmentEditorDto> getAssignmentsToRemoveList(UserType user){
        List<ConflictDto> conflicts = getSessionStorage().getRoleCatalog().getConflictsList();
        List<String> assignmentsToRemoveOids = new ArrayList<>();
        for (ConflictDto dto : conflicts){
            if (dto.isResolved()){
                if (dto.getAssignment1().isResolved() && dto.getAssignment1().isOldAssignment()){
                    assignmentsToRemoveOids.add(dto.getAssignment1().getAssignmentTargetObject().getOid());
                } else if (dto.getAssignment2().isResolved() && dto.getAssignment2().isOldAssignment()){
                    assignmentsToRemoveOids.add(dto.getAssignment2().getAssignmentTargetObject().getOid());
                }
            }
        }

        List<AssignmentEditorDto> assignmentsToDelete = new ArrayList<>();
        for (AssignmentType assignment : user.getAssignment()){
            if (assignment.getTargetRef() == null){
                continue;
            }
            if (assignmentsToRemoveOids.contains(assignment.getTargetRef().getOid())){
                assignmentsToDelete.add(new AssignmentEditorDto(UserDtoStatus.DELETE, assignment, this));
            }
        }
        return assignmentsToDelete;
    }

    private TextArea getDescriptionComponent(){
        return (TextArea) get(ID_FORM).get(ID_DESCRIPTION);
    }

    private PrismObject<UserType> getTargetUser() throws SchemaException {
        List<PrismObject<UserType>> usersList = getSessionStorage().getRoleCatalog().getTargetUserList();
        PrismObject<UserType> user = getSessionStorage().getRoleCatalog().isSelfRequest() ?
                loadUserSelf() : usersList.get(0);
        getPrismContext().adopt(user);
        return user;
    }

    @Override
    public boolean canRedirectBack(){
        return true;
    }

    private boolean isCreatedConflict(Map<String, String> oidsMap, String oid1, String oid2){
        if ((oidsMap.containsKey(oid1) && oidsMap.get(oid1).equals(oid2))
                || (oidsMap.containsKey(oid2) && oidsMap.get(oid2).equals(oid1))){
            return true;
        }
        return false;
    }
}
