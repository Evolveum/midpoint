/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

import java.util.*;

import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;

import com.evolveum.midpoint.web.application.Url;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
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
import com.evolveum.midpoint.web.component.assignment.UserSelectionButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
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

/**
 * Created by honchar.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/requestAssignments")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL,
                label = "PageAssignmentShoppingCart.auth.requestAssignments.label",
                description = "PageAssignmentShoppingCart.auth.requestAssignments.description") })
public class PageAssignmentsList<F extends FocusType> extends PageBase {
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
    private static final String OPERATION_WF_TASK_CREATED = "com.evolveum.midpoint.wf.impl.hook.WfHook.invoke";
    private static final String OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS = "reviewAssignmentConflicts";
    private static final String OPERATION_LOAD_ASSIGNMENT_TARGET_USER_OBJECT = "loadAssignmentTargetUserObject";

    private IModel<List<AssignmentEditorDto>> assignmentsModel;
    private IModel<String> descriptionModel;
    private boolean conflictProblemExists = false;

    private final boolean loadConflicts;

    public PageAssignmentsList() {
        this(false);
    }

    public PageAssignmentsList(boolean loadConflicts) {
        this.loadConflicts = loadConflicts;
        initModels();
        if (loadConflicts) {
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

        MidpointForm mainForm = new MidpointForm(ID_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        AssignmentTablePanel panel = new AssignmentTablePanel<UserType>(ID_ASSIGNMENT_TABLE_PANEL,
                assignmentsModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<InlineMenuItem> createAssignmentMenu() {
                List<InlineMenuItem> items = new ArrayList<>();
                InlineMenuItem item = new InlineMenuItem(createStringResource("PageAssignmentsList.deleteAllItemsFromShoppingCart")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new InlineMenuItemAction() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                getModelObject().forEach(assignmentEditorDto -> assignmentEditorDto.setSelected(true));
                                deleteAssignmentPerformed(target, null);
                            }
                        };
                    }
                };
                items.add(item);
                return items;
            }

            @Override
            public IModel<String> getLabel() {
                return createStringResource("PageAssignmentsList.assignmentsToRequest");
            }

            @Override
            protected boolean isRelationEditable() {
                return false;
            }

            @Override
            protected void reloadMainFormButtons(AjaxRequestTarget target) {
                Component requestButton = PageAssignmentsList.this.get(createComponentPath(ID_FORM, ID_REQUEST_BUTTON));
                if (requestButton != null) {
                    refreshRequestButton(requestButton);
                }
                target.add(getFeedbackPanel());
                target.add(requestButton);
            }
        };
        mainForm.add(panel);

        UserSelectionButton targetUserPanel = new UserSelectionButton(ID_TARGET_USER_PANEL,
                new IModel<List<UserType>>() {
                    @Override
                    public List<UserType> getObject() {
                        return WebComponentUtil.loadTargetUsersListForShoppingCart(OPERATION_LOAD_ASSIGNMENT_TARGET_USER_OBJECT,
                                PageAssignmentsList.this);
                    }
                },
                true, createStringResource("AssignmentCatalogPanel.selectTargetUser")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getUserButtonLabel() {
                return getTargetUserSelectionButtonLabel(getModelObject());
            }

            @Override
            protected void onDeleteSelectedUsersPerformed(AjaxRequestTarget target) {
                super.onDeleteSelectedUsersPerformed(target);
                getSessionStorage().getRoleCatalog().setTargetUserOidsList(new ArrayList<>());
                targetUserChangePerformed(target);
            }

            @Override
            protected void multipleUsersSelectionPerformed(AjaxRequestTarget target, List<UserType> usersList) {
                if (CollectionUtils.isNotEmpty(usersList)) {
                    List<String> usersOidsList = new ArrayList<>();
                    usersList.forEach(user -> usersOidsList.add(user.getOid()));
                    getSessionStorage().getRoleCatalog().setTargetUserOidsList(usersOidsList);
                }
                targetUserChangePerformed(target);
            }

        };

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

        AjaxButton requestAssignments = new AjaxButton(ID_REQUEST_BUTTON, createStringResource("PageAssignmentsList.requestButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getSessionStorage().getRoleCatalog().getTargetUserOidsList() == null ||
                        getSessionStorage().getRoleCatalog().getTargetUserOidsList().size() <= 1) {
                    onSingleUserRequestPerformed(target);
                } else {
                    onMultiUserRequestPerformed(target);
                }
            }

        };
        requestAssignments.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return isRequestButtonEnabled();
            }
        });
        mainForm.add(requestAssignments);
        refreshRequestButton(requestAssignments);

        AjaxSubmitButton resolveAssignments = new AjaxSubmitButton(ID_RESOLVE_CONFLICTS_BUTTON,
                createStringResource("PageAssignmentsList.resolveConflicts")) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                PageAssignmentsList.this.navigateToNext(PageAssignmentConflicts.class);
            }

        };
        resolveAssignments.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !getSessionStorage().getRoleCatalog().isMultiUserRequest()
                        && !areConflictsResolved();
            }
        });
        mainForm.add(resolveAssignments);

    }

    private void refreshRequestButton(Component requestButton) {
        if (isRequestButtonEnabled()) {
            requestButton.add(AttributeModifier.remove("disabled"));
        } else {
            warn(createStringResource("PageAssignmentsList.message.notContainsAssignments").getString());
            requestButton.add(AttributeModifier.replace("disabled", ""));
        }
    }

    private boolean isRequestButtonEnabled() {
        return (getSessionStorage().getRoleCatalog().isMultiUserRequest() ||
                onlyWarnings() || areConflictsResolved()) &&
                !conflictProblemExists &&
                getSessionStorage().getRoleCatalog().getAssignmentShoppingCart() != null &&
                getSessionStorage().getRoleCatalog().getAssignmentShoppingCart().size() > 0;
    }

    private void onSingleUserRequestPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_REQUEST_ASSIGNMENTS);
        ObjectDelta<UserType> delta;
        try {
            PrismObject<UserType> user = getTargetUser();
            delta = prepareDelta(user, result);

            ModelExecuteOptions options = createOptions();
            options.initialPartialProcessing(new PartialProcessingOptionsType().inbound(SKIP).projection(SKIP)); // TODO make this configurable?
            getModelService().executeChanges(Collections.singletonList(delta), options, createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS), result);

            result.recordSuccess();
            SessionStorage storage = getSessionStorage();
            storage.getRoleCatalog().getAssignmentShoppingCart().clear();
        } catch (Exception e) {
            result.recordFatalError(e);
            result.setMessage(createStringResource("PageAssignmentsList.requestError").getString());
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save assignments ", e);
        } finally {
            result.recomputeStatus();
        }

        if (hasBackgroundTaskOperation(result)) {
            result.setMessage(createStringResource("PageAssignmentsList.requestInProgress").getString());
            showResult(result);
            clearStorage();
            setResponsePage(PageAssignmentShoppingCart.class);
            return;
        }
        if (WebComponentUtil.isSuccessOrHandledError(result)
                || OperationResultStatus.IN_PROGRESS.equals(result.getStatus())) {
            clearStorage();
            result.setMessage(createStringResource("PageAssignmentsList.requestSuccess").getString());
            setResponsePage(PageAssignmentShoppingCart.class);
        } else {
            result.setMessage(createStringResource("PageAssignmentsList.requestError").getString());
            target.add(getFeedbackPanel());
            target.add(PageAssignmentsList.this.get(ID_FORM));
        }
        showResult(result);
    }

    private void targetUserChangePerformed(AjaxRequestTarget target) {
        PageAssignmentsList.this.getFeedbackMessages().clear();
        conflictProblemExists = false;
        if (loadConflicts) {
            getSessionStorage().getRoleCatalog().setConflictsList(getAssignmentConflicts());
        }
        target.add(PageAssignmentsList.this);
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
        ModelExecuteOptions options = ExecuteChangeOptionsDto.createFromSystemConfiguration().createOptions(getPrismContext());
        options.requestBusinessContext(businessContextType);
        return options;
    }

    // TODO initial partial processing options - MID-4059 (but it's not so important here, because the task runs on background)
    private void onMultiUserRequestPerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_REQUEST_ASSIGNMENTS);
        String executionTaskOid = null;
        try {
            TaskType task = WebComponentUtil.createIterativeChangeExecutionTask(
                    createStringResource(OPERATION_REQUEST_ASSIGNMENTS).getString(),
                    UserType.COMPLEX_TYPE,
                    getTaskQuery(),
                    prepareDelta(null, result),
                    createOptions(),
                    PageAssignmentsList.this);
            executionTaskOid = WebModelServiceUtils.runTask(task, result, PageAssignmentsList.this);
        } catch (SchemaException e) {
            result.recordFatalError(result.getOperation(), e);
            result.setMessage(createStringResource("PageAssignmentsList.requestError").getString());
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Failed to execute operation " + result.getOperation(), e);
            target.add(getFeedbackPanel());
        }
        if (hasBackgroundTaskOperation(result) || StringUtils.isNotEmpty(executionTaskOid)) {
            result.setMessage(createStringResource("PageAssignmentsList.requestInProgress").getString());
            showResult(result);
            clearStorage();
            setResponsePage(PageAssignmentShoppingCart.class);
            return;
        }
        if (WebComponentUtil.isSuccessOrHandledError(result)
                || OperationResultStatus.IN_PROGRESS.equals(result.getStatus())) {
            clearStorage();
            result.setMessage(createStringResource("PageAssignmentsList.requestSuccess").getString());
            setResponsePage(PageAssignmentShoppingCart.class);
        } else {
            result.setMessage(createStringResource("PageAssignmentsList.requestError").getString());
            target.add(getFeedbackPanel());
            target.add(PageAssignmentsList.this.get(ID_FORM));
        }
        showResult(result);
    }

    private void clearStorage() {
        SessionStorage storage = getSessionStorage();
        if (storage.getRoleCatalog().getAssignmentShoppingCart() != null) {
            storage.getRoleCatalog().getAssignmentShoppingCart().clear();
        }
        if (storage.getRoleCatalog().getTargetUserOidsList() != null) {
            storage.getRoleCatalog().getTargetUserOidsList().clear();
        }
        storage.getRoleCatalog().setRequestDescription("");
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> focusDelta,
            List<AssignmentEditorDto> assignments, PrismContainerDefinition def) throws SchemaException {
        ContainerDelta assDelta = getPrismContext().deltaFactory().container().create(ItemPath.EMPTY_PATH, def.getItemName(), def);

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
                        LOGGER.trace("Assignment '{}' not modified.", assDto.getName());
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

    private boolean hasBackgroundTaskOperation(OperationResult result) {
        String caseOid = OperationResult.referenceToCaseOid(result.findAsynchronousOperationReference());
        return StringUtils.isNotEmpty(caseOid);
    }

    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
            PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<UserType> focusDelta)
            throws SchemaException {
        LOGGER.debug("Handling modified assignment '{}', computing delta.", assDto.getName());

        PrismValue oldValue = assDto.getOldValue();
        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue, EquivalenceStrategy.IGNORE_METADATA);

        for (ItemDelta delta : deltas) {
            ItemPath deltaPath = delta.getPath().rest();
            ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

            delta.setParentPath(WebComponentUtil.joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
            delta.applyDefinition(deltaDef);

            focusDelta.addModification(delta);
        }
    }

    private List<ConflictDto> getAssignmentConflicts() {
        ObjectDelta<UserType> delta;
        OperationResult result = new OperationResult(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        Task task = createSimpleTask(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
        Map<String, ConflictDto> conflictsMap = new HashMap<>();
        try {
            PrismObject<UserType> user = getTargetUser();
            delta = user.createModifyDelta();

            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
            handleAssignmentDeltas(delta, getSessionStorage().getRoleCatalog().getAssignmentShoppingCart(), def);

            PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
            partialProcessing.setInbound(SKIP);
            partialProcessing.setProjection(SKIP);
            ModelExecuteOptions recomputeOptions = executeOptions().partialProcessing(partialProcessing);
            ModelContext<UserType> modelContext = getModelInteractionService()
                    .previewChanges(MiscUtil.createCollection(delta), recomputeOptions, task, result);
            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple =
                    modelContext.getEvaluatedAssignmentTriple();
            if (evaluatedAssignmentTriple != null) {
                Collection<? extends EvaluatedAssignment> addedAssignments = evaluatedAssignmentTriple.getPlusSet();
                for (EvaluatedAssignment<UserType> evaluatedAssignment : addedAssignments) {
                    for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
                        if (!policyRule.containsEnabledAction()) {
                            continue;
                        }
                        // everything other than 'enforce' is a warning
                        boolean isWarning = !policyRule.containsEnabledAction(EnforcementPolicyActionType.class);
                        fillInConflictedObjects(evaluatedAssignment, policyRule.getAllTriggers(), isWarning, conflictsMap);
                    }
                }
            } else if (!result.isSuccess() && StringUtils.isNotEmpty(getSubresultWarningMessages(result))) {
                getFeedbackMessages().warn(PageAssignmentsList.this,
                        createStringResource("PageAssignmentsList.conflictsWarning").getString() + " " + getSubresultWarningMessages(result));
                conflictProblemExists = true;
            }
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get assignments conflicts. Reason: ", e);
            error("Couldn't get assignments conflicts. Reason: " + e);
        }
        return new ArrayList<>(conflictsMap.values());
    }

    private String getSubresultWarningMessages(OperationResult result) {
        if (result == null || result.getSubresults() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        result.getSubresults().forEach(subresult -> {
            if (subresult.isWarning()) {
                sb.append(subresult.getMessage());
                sb.append("\n");
            }
        });
        return sb.toString();
    }

    private void fillInConflictedObjects(EvaluatedAssignment<UserType> evaluatedAssignment, Collection<EvaluatedPolicyRuleTrigger<?>> triggers, boolean isWarning, Map<String, ConflictDto> conflictsMap) {

        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {

            if (trigger instanceof EvaluatedExclusionTrigger) {
                fillInFromEvaluatedExclusionTrigger(evaluatedAssignment, (EvaluatedExclusionTrigger) trigger, isWarning, conflictsMap);
            } else if (trigger instanceof EvaluatedCompositeTrigger) {
                EvaluatedCompositeTrigger compositeTrigger = (EvaluatedCompositeTrigger) trigger;
                Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers = compositeTrigger.getInnerTriggers();
                fillInConflictedObjects(evaluatedAssignment, innerTriggers, isWarning, conflictsMap);
            }
        }

    }

    private void fillInFromEvaluatedExclusionTrigger(EvaluatedAssignment<UserType> evaluatedAssignment, EvaluatedExclusionTrigger exclusionTrigger, boolean isWarning, Map<String, ConflictDto> conflictsMap) {
        EvaluatedAssignment<F> conflictingAssignment = exclusionTrigger.getConflictingAssignment();
        PrismObject<F> addedAssignmentTargetObj = (PrismObject<F>) evaluatedAssignment.getTarget();
        PrismObject<F> exclusionTargetObj = (PrismObject<F>) conflictingAssignment.getTarget();

        AssignmentConflictDto<F> dto1 = new AssignmentConflictDto<>(exclusionTargetObj,
                conflictingAssignment.getAssignment(true) != null);
        AssignmentConflictDto<F> dto2 = new AssignmentConflictDto<>(addedAssignmentTargetObj,
                evaluatedAssignment.getAssignment(true) != null);
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

    private boolean onlyWarnings() {
        List<ConflictDto> list = getSessionStorage().getRoleCatalog().getConflictsList();
        for (ConflictDto dto : list) {
            if (!dto.isWarning()) {
                return false;
            }
        }
        return true;
    }

    private boolean areConflictsResolved() {
        List<ConflictDto> list = getSessionStorage().getRoleCatalog().getConflictsList();
        for (ConflictDto dto : list) {
            if (!dto.isResolved()) {
                return false;
            }
        }
        return true;
    }

    private ObjectDelta<UserType> prepareDelta(PrismObject<UserType> user, OperationResult result) {
        ObjectDelta<UserType> delta = null;
        try {
            //noinspection unchecked
            delta = getPrismContext().deltaFactory().object()
                    .createModificationAddContainer(UserType.class, user == null ? "fakeOid" : user.getOid(),
                            FocusType.F_ASSIGNMENT, getAddAssignmentContainerValues(assignmentsModel.getObject()));
            if (!getSessionStorage().getRoleCatalog().isMultiUserRequest()) {
                //noinspection unchecked
                delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT,
                        getDeleteAssignmentContainerValues(user.asObjectable()));
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for operation " + OPERATION_REQUEST_ASSIGNMENTS, e);
            result.recordFatalError(getString("PageAssignmentsList.message.prepareDelta.fatalError", OPERATION_REQUEST_ASSIGNMENTS), e);
        }
        return delta;
    }

    private ObjectQuery getTaskQuery() {
        List<String> targetUsersOids = getSessionStorage().getRoleCatalog().isSelfRequest()
                ? Collections.singletonList(getPrincipalFocus().getOid())
                : getSessionStorage().getRoleCatalog().getTargetUserOidsList();
        QueryFactory queryFactory = getPrismContext().queryFactory();
        return queryFactory.createQuery(queryFactory.createInOid(targetUsersOids));
    }

    private PrismContainerValue[] getAddAssignmentContainerValues(List<AssignmentEditorDto> assignments) throws SchemaException {

        List<PrismContainerValue<AssignmentType>> addContainerValues = new ArrayList<>();
        for (AssignmentEditorDto assDto : assignments) {
            if (UserDtoStatus.ADD.equals(assDto.getStatus())) {
                addContainerValues.add(assDto.getNewValue(getPrismContext()).clone());      // clone is to eliminate "Attempt to reset value parent ..." exceptions (in some cases)
            }

        }
        return addContainerValues.toArray(new PrismContainerValue[0]);
    }

    private PrismContainerValue[] getDeleteAssignmentContainerValues(UserType user) throws SchemaException {
        List<PrismContainerValue<AssignmentType>> deleteAssignmentValues = new ArrayList<>();
        for (AssignmentEditorDto assDto : getAssignmentsToRemoveList(user)) {
            deleteAssignmentValues.add(assDto.getNewValue(getPrismContext()));
        }
        return deleteAssignmentValues.toArray(new PrismContainerValue[0]);
    }

    private List<AssignmentEditorDto> getAssignmentsToRemoveList(UserType user) {
        List<ConflictDto> conflicts = getSessionStorage().getRoleCatalog().getConflictsList();
        List<String> assignmentsToRemoveOids = new ArrayList<>();
        for (ConflictDto dto : conflicts) {
            if (dto.isResolved()) {
                if (dto.getAssignment1().isResolved() && dto.getAssignment1().isOldAssignment()) {
                    assignmentsToRemoveOids.add(dto.getAssignment1().getAssignmentTargetObject().getOid());
                } else if (dto.getAssignment2().isResolved() && dto.getAssignment2().isOldAssignment()) {
                    assignmentsToRemoveOids.add(dto.getAssignment2().getAssignmentTargetObject().getOid());
                }
            }
        }

        List<AssignmentEditorDto> assignmentsToDelete = new ArrayList<>();
        for (AssignmentType assignment : user.getAssignment()) {
            if (assignment.getTargetRef() == null) {
                continue;
            }
            if (assignmentsToRemoveOids.contains(assignment.getTargetRef().getOid())) {
                assignmentsToDelete.add(new AssignmentEditorDto(UserDtoStatus.DELETE, assignment, this));
            }
        }
        return assignmentsToDelete;
    }

    private TextArea getDescriptionComponent() {
        return (TextArea) get(ID_FORM).get(ID_DESCRIPTION);
    }

    private PrismObject<UserType> getTargetUser() throws SchemaException {
        String targetUserOid = getSessionStorage().getRoleCatalog().isSelfRequest() ?
                getPrincipalFocus().getOid() :
                getSessionStorage().getRoleCatalog().getTargetUserOidsList().get(0);
        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT_TARGET_USER_OBJECT);
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_TARGET_USER_OBJECT);
        PrismObject<UserType> user = WebModelServiceUtils.loadObject(UserType.class,
                targetUserOid, PageAssignmentsList.this, task, result);
        if (user == null) {
            return null;
        }
        getPrismContext().adopt(user);
        return user;
    }

    @Override
    public boolean canRedirectBack() {
        return true;
    }

    private String getTargetUserSelectionButtonLabel(List<UserType> usersList) {
        if (usersList == null || usersList.size() == 0) {
            String label = createStringResource("AssignmentCatalogPanel.requestFor",
                    createStringResource("AssignmentCatalogPanel.requestForMe").getString()).getString();
            return label;
        } else if (usersList.size() == 1) {
            String name = usersList.get(0).getName().getOrig();
            return createStringResource("AssignmentCatalogPanel.requestFor", name).getString();
        } else {
            return createStringResource("AssignmentCatalogPanel.requestForMultiple",
                    usersList.size()).getString();
        }
    }
}
