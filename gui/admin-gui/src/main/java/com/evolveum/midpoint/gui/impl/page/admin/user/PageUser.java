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

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.gui.impl.page.admin.component.UserOperationalButtonsPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
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
            delegationChangesExist = processDeputyAssignments(previewOnly);
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
        return new UserOperationalButtonsPanel(id, wrapperModel, getObjectDetailsModels().getDelegationsModel(), getObjectDetailsModels().getExecuteOptionsModel(), getObjectDetailsModels().isSelfProfile()) {

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

    private boolean processDeputyAssignments(boolean previewOnly) {
        boolean isAnythingChanged = false;
        for (AssignmentEditorDto dto : getObjectDetailsModels().getDelegationsModelObject()) {
            if (!UserDtoStatus.MODIFY.equals(dto.getStatus())) {
                if (!previewOnly) {
                    UserType user = dto.getDelegationOwner();
                    saveDelegationToUser(user.asPrismObject(), dto);
                }
                isAnythingChanged = true;
            }
        }
        return isAnythingChanged;
    }

    private void saveDelegationToUser(PrismObject<UserType> user, AssignmentEditorDto assignmentDto) {
        OperationResult result = new OperationResult(OPERATION_SAVE);
        try {
            getPrismContext().adopt(user);
            Collection<ObjectDelta<? extends ObjectType>> deltas = prepareDelegationDelta(user, assignmentDto);
            getModelService().executeChanges(deltas, getExecuteChangesOptionsDto().createOptions(PrismContext.get()), createSimpleTask(OPERATION_SAVE), result);

            result.recordSuccess();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save assignments ", e);
            error("Could not save assignments. Reason: " + e);
        } finally {
            result.recomputeStatus();
        }
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
