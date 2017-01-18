/*
 * Copyright (c) 2010-2016 Evolveum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
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
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.MultipleAssignmentSelectorPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;

import java.util.*;

/**
 * Created by Kate on 31.03.2016.
 */
@PageDescriptor(url = {"/self/requestRole"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REQUEST_ROLE_URL,
                label = "PageSelfCredentials.auth.requestRole.label",
                description = "PageSelfCredentials.auth.requestRole.description")})
public class PageRequestRole extends PageSelf {
    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_SAVE_BUTTON = "save";
    private static final String ID_CANCEL_BUTTON = "cancel";

    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
    private PrismObject<UserType> user;
    private OperationResult backgroundTaskOperationResult = null;

    private static final Trace LOGGER = TraceManager.getTrace(PageRequestRole.class);
    private static final String DOT_CLASS = PageRequestRole.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    protected static final String OPERATION_SAVE = DOT_CLASS + "save";
    protected static final String OPERATION_WF_TASK_CREATED = "com.evolveum.midpoint.wf.impl.WfHook.invoke";

    public PageRequestRole() {
        assignmentsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

            @Override
            protected List<AssignmentEditorDto> load() {
                return loadAssignments();
            }
        };
        loadUser();
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new org.apache.wicket.markup.html.form.Form(ID_MAIN_FORM);
        add(mainForm);

        MultipleAssignmentSelectorPanel<UserType, UserType, RoleType> panel =
                new MultipleAssignmentSelectorPanel<>(ID_MAIN_PANEL, assignmentsModel,
                user, UserType.class, RoleType.class, this);
        mainForm.add(panel);

        initButtons(mainForm);
    }

    private List<AssignmentEditorDto> loadAssignments() {
        List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

        List<AssignmentType> assignments = user.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
        }
        Collections.sort(list);
        return list;
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                onSavePerformed(target);
            }
        };
        mainForm.setDefaultButton(save);
        mainForm.add(save);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_CANCEL_BUTTON, createStringResource("PageBase.button.back")) {

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
//                onCancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    private void loadUser() {
        LOGGER.debug("Loading user and accounts.");
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        try {
            String userOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = createSimpleTask(OPERATION_LOAD_USER);
            user = WebModelServiceUtils.loadObject(UserType.class, userOid, null, (PageBase) getPage(),
                    task, result);
            result.recordSuccessIfUnknown();

            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load accounts", ex);
            result.recordFatalError("Couldn't load accounts", ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            showResult(result);
        }
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model("fa fa-pencil-square-o"));
    }

    private void onSavePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SAVE);
        ObjectDelta<UserType> delta;
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        try {
            delta = user.createModifyDelta();
            deltas.add(delta);
            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
            handleAssignmentDeltas(delta, assignmentsModel.getObject(), def);
            getModelService().executeChanges(deltas, null, createSimpleTask(OPERATION_SAVE), result);

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
            setResponsePage(PageRequestRole.class);
            return;
        }
        showResult(result);
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            target.add(getFeedbackPanel());
        } else {
            setResponsePage(PageRequestRole.class);
        }
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

    protected ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> focusDelta,
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
}
