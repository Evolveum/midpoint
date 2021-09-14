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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.UserSummaryPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/userNew")
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
    protected Class<UserType> getType() {
        return UserType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<UserType> summaryModel) {
        return new UserSummaryPanel(id, summaryModel, PageUser.this);
    }

    @Override
    protected UserDetailsModel createObjectDetailsModels(PrismObject<UserType> object) {
        return new UserDetailsModel(createPrismObejctModel(object), this);
    }

    @Override
    protected void executeModifyDelta(Collection<ObjectDelta<? extends ObjectType>> deltas, boolean previewOnly,
            ExecuteChangeOptionsDto executeChangeOptionsDto, Task task, OperationResult result, AjaxRequestTarget target) {
        boolean delegationChangesExist = processDeputyAssignments(previewOnly);
        if (deltas.isEmpty() && !executeChangeOptionsDto.isReconcile()) {
            progressPanel.clearProgressPanel();
            if (!previewOnly) {
                if (!delegationChangesExist) {
                    result.recordWarning(getString("PageAdminObjectDetails.noChangesSave"));
                    showResult(result);
                }
                redirectBack();
            } else {
                if (!delegationChangesExist) {
                    warn(getString("PageAdminObjectDetails.noChangesPreview"));
                    target.add(getFeedbackPanel());
                }
            }
            return;
        }
        super.executeModifyDelta(deltas, previewOnly, executeChangeOptionsDto, task, result, target);
    }

    private boolean processDeputyAssignments(boolean previewOnly) {
        boolean isAnythingChanged = false;
        for (AssignmentEditorDto dto : ((UserDetailsModel)getObjectDetailsModels()).getDelegationsModel().getObject()) {
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

        showResult(result);
    }

    private Collection<ObjectDelta<? extends ObjectType>> prepareDelegationDelta(PrismObject<UserType> user, AssignmentEditorDto dto)
            throws SchemaException {
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> delta = user.createModifyDelta();
        List<AssignmentEditorDto> userAssignmentsDtos = new ArrayList<>();
        userAssignmentsDtos.add(dto);

        deltas.add(delta);
        PrismContainerDefinition<?> def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
        handleDelegationAssignmentDeltas(delta, userAssignmentsDtos, def);
        return deltas;
    }

    protected void handleDelegationAssignmentDeltas(ObjectDelta<UserType> focusDelta,
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
                    if (true) {
                        oldValue.applyDefinition(def, false);
                    } else {
                        oldValue.applyDefinition(def);
                    }
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

}
