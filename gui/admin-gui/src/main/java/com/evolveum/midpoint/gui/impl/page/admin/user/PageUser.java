/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.gui.impl.page.admin.component.UserOperationalButtonsPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.LocalizableMessageListBuilder;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/user", matchUrlForSecurity = "/admin/user")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageUser extends PageFocusDetails<UserType, UserDetailsModel> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);

    public PageUser() {
        super();
    }

    public PageUser(PageParameters params) {
        super(params);
    }

    public PageUser(PrismObject<UserType> user) {
        super(user);

    }

    @Override
    public Class<UserType> getType() {
        return UserType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<UserType> summaryModel) {
        return new UserSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected UserDetailsModel createObjectDetailsModels(PrismObject<UserType> object) {
        return new UserDetailsModel(createPrismObjectModel(object), this);
    }

    private boolean delegationChangesExist = false;

    @Override
    protected Collection<ObjectDeltaOperation<? extends ObjectType>> executeChanges(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly, ExecuteChangeOptionsDto options, Task task, OperationResult result, AjaxRequestTarget target) {
        if (ItemStatus.NOT_CHANGED == getObjectDetailsModels().getObjectStatus()) {
            List<AssignmentEditorDto> changedDelegations = getChangedDelegations();
            delegationChangesExist = !changedDelegations.isEmpty();
            if (delegationChangesExist && !processDelegations(changedDelegations, previewOnly, result)) {
                // Nothing is saved (or previewed), stay on the page and show the reason.
                showResult(result);
                return null;
            }
        }
        return super.executeChanges(deltas, previewOnly, options, task, result, target);
    }

    @Override
    protected boolean noChangesToExecute(Collection<ObjectDelta<? extends ObjectType>> deltas, ExecuteChangeOptionsDto options) {
        return deltas.isEmpty() && !options.isReconcile() && !delegationChangesExist;
    }

    @Override
    protected void collectObjectsForPreview(Map<PrismObject<UserType>, ModelContext<? extends ObjectType>> prismObjectModelContextMap) {
        super.collectObjectsForPreview(prismObjectModelContextMap);
        processAdditionalFocalObjectsForPreview(prismObjectModelContextMap);
    }

    @Override
    protected UserOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<UserType>> wrapperModel) {
        return new UserOperationalButtonsPanel(id, wrapperModel, getObjectDetailsModels().getExecuteOptionsModel(), getObjectDetailsModels().isSelfProfile()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void refresh(AjaxRequestTarget target) {
                PageUser.this.refresh(target);
            }
            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                PageUser.this.savePerformed(target);
            }

            @Override
            protected void previewPerformed(AjaxRequestTarget target) {
                PageUser.this.previewPerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageUser.this.hasUnsavedChanges(target);
            }
        };
    }

    /**
     * for now used only for delegation changes
     * @param modelContextMap preview changes deltas
     */
    protected void processAdditionalFocalObjectsForPreview(Map<PrismObject<UserType>, ModelContext<? extends ObjectType>> modelContextMap){
        for (AssignmentEditorDto dto : getObjectDetailsModels().getDelegationsModelObject()) {
            if (!UserDtoStatus.MODIFY.equals(dto.getStatus())) {
                UserType user = dto.getDelegationOwner();

                OperationResult result = new OperationResult(OPERATION_PREVIEW_CHANGES);
                Task task = createSimpleTask(OPERATION_PREVIEW_CHANGES);
                try {

                    Collection<ObjectDelta<? extends ObjectType>> deltas = prepareDelegationDelta(user.asPrismObject(), dto);

                    ModelContext<UserType> modelContext = getModelInteractionService().previewChanges(deltas, getDelegationPreviewOptions(), task, result);
                    modelContextMap.put(user.asPrismObject(), modelContext);
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Could not save delegation ", e);
                    error("Could not save delegation. Reason: " + e);
                } finally {
                    result.recomputeStatus();
                }
            }
        }
    }

    private ModelExecuteOptions getDelegationPreviewOptions() {
        ModelExecuteOptions options = getProgressPanel().getExecuteOptions().createOptions(getPrismContext());
        options.getOrCreatePartialProcessing().setApprovals(PartialProcessingTypeType.PROCESS);
        return options;
    }

    private List<AssignmentEditorDto> getChangedDelegations() {
        return getObjectDetailsModels().getDelegationsModelObject().stream()
                .filter(dto -> !UserDtoStatus.MODIFY.equals(dto.getStatus()))
                .toList();
    }

    /**
     * Delegations are stored in the other users, so they are saved separately from this user.
     * To make the whole save (or preview) fail if any of them is not allowed (e.g. because of a policy rule),
     * all of them are checked by preview first and saved only if there is no error.
     *
     * @return false if the delegations aren't allowed, so this user shouldn't be saved (or previewed) either
     */
    private boolean processDelegations(List<AssignmentEditorDto> delegations, boolean previewOnly, OperationResult result) {
        try {
            for (AssignmentEditorDto dto : delegations) {
                if (!executeDelegationChanges(dto, true, result)) {
                    return false;
                }
            }
            if (!previewOnly) {
                for (AssignmentEditorDto dto : delegations) {
                    executeDelegationChanges(dto, false, result);
                }
            }
            return true;
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save delegation ", e);
            result.recordException(e);
            return false;
        }
    }

    /**
     * @return false if the delegation is not allowed (the reason is recorded in the result)
     */
    private boolean executeDelegationChanges(AssignmentEditorDto dto, boolean previewOnly, OperationResult result)
            throws CommonException {
        PrismObject<UserType> user = dto.getDelegationOwner().asPrismObject();
        getPrismContext().adopt(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = prepareDelegationDelta(user, dto);
        ModelExecuteOptions options = getExecuteChangesOptionsDto().createOptions(PrismContext.get());
        Task task = createSimpleTask(OPERATION_SAVE);
        if (previewOnly) {
            ModelContext<UserType> modelContext = getModelInteractionService().previewChanges(deltas, options, task, result);
            return checkPolicyViolations(modelContext, result);
        }
        getModelService().executeChanges(deltas, options, task, result);
        return true;
    }

    /**
     * Enforced policy rules don't throw an exception in preview, they are only a part of the preview output.
     * So the violation is recorded to the result here, as it would be recorded when executing the changes.
     *
     * @return false if some policy rule is violated
     */
    private boolean checkPolicyViolations(ModelContext<UserType> modelContext, OperationResult result) {
        PolicyRuleEnforcerPreviewOutputType enforcements = modelContext != null
                ? modelContext.getPolicyRuleEnforcerPreviewOutput()
                : null;
        if (enforcements == null || enforcements.getRule().isEmpty()) {
            return true;
        }

        List<LocalizableMessage> messages = enforcements.getRule().stream()
                .flatMap(rule -> rule.getTrigger().stream())
                .map(EvaluatedPolicyRuleTriggerType::getMessage)
                .filter(Objects::nonNull)
                .map(com.evolveum.midpoint.schema.util.LocalizationUtil::toLocalizableMessage)
                .toList();
        LocalizableMessage message = new LocalizableMessageListBuilder()
                .messages(messages)
                .separator(LocalizableMessageList.SEMICOLON)
                .buildOptimized();
        result.recordFatalError(LocalizationUtil.translateMessage(message));
        result.setUserFriendlyMessage(message);
        return false;
    }

    private Collection<ObjectDelta<? extends ObjectType>> prepareDelegationDelta(PrismObject<UserType> user, AssignmentEditorDto dto)
            throws SchemaException {
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> delta = user.createModifyDelta();
        List<AssignmentEditorDto> userAssignmentsDtos = new ArrayList<>();
        userAssignmentsDtos.add(dto);

        deltas.add(delta);
        PrismContainerDefinition<AssignmentType> def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
        handleDelegationAssignmentDeltas(delta, userAssignmentsDtos, def);
        return deltas;
    }

    protected void handleDelegationAssignmentDeltas(ObjectDelta<UserType> focusDelta,
            List<AssignmentEditorDto> assignments, PrismContainerDefinition<AssignmentType> def) throws SchemaException {
        ContainerDelta<AssignmentType> assDelta = def.createEmptyDelta(def.getItemName());

        for (AssignmentEditorDto assDto : assignments) {
            PrismContainerValue<AssignmentType> newValue = assDto.getNewValue(getPrismContext());

            switch (assDto.getStatus()) {
                case ADD:
                    newValue.applyDefinition(def, false);
                    assDelta.addValueToAdd(newValue.clone());
                    break;
                case DELETE:
                    PrismContainerValue<AssignmentType> oldValue = assDto.getOldValue();
                    oldValue.applyDefinition(def, false);
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
            focusDelta.addModification(assDelta);
        }
    }

    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
            PrismContainerDefinition<AssignmentType> assignmentDef, PrismContainerValue<AssignmentType> newValue, ObjectDelta<UserType> focusDelta)
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
}
