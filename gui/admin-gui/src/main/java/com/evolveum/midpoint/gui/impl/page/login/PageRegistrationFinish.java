/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

//CONFIRMATION_LINK = "http://localhost:8080/midpoint/confirm/registration/";
@PageDescriptor(urls = {@Url(mountUrl = "/registration/result")}, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REGISTRATION_FINISH_URL,
                label = "PageSelfCredentials.auth.registration.finish.label",
                description = "PageSelfCredentials.auth.registration.finish.description")})
public class PageRegistrationFinish extends PageRegistrationBase {

    private static final Trace LOGGER = TraceManager.getTrace(PageRegistrationFinish.class);

    private static final String DOT_CLASS = PageRegistrationFinish.class.getName() + ".";

    private static final String ID_LABEL_SUCCESS = "successLabel";
    private static final String ID_LABEL_ERROR = "errorLabel";
    private static final String ID_LINK_LOGIN = "linkToLogin";
    private static final String ID_SUCCESS_PANEL = "successPanel";
    private static final String ID_ERROR_PANEL = "errorPanel";

    private static final String OPERATION_ASSIGN_DEFAULT_ROLES = DOT_CLASS + "assignDefaultRoles";
    private static final String OPERATION_ASSIGN_ADDITIONAL_ROLE = DOT_CLASS + "assignAdditionalRole";
    private static final String OPERATION_FINISH_REGISTRATION = DOT_CLASS + "finishRegistration";
    private static final String OPERATION_CHECK_CREDENTIALS = DOT_CLASS + "checkCredentials";
    private static final String OPERATION_REMOVE_NONCE_AND_SET_LIFECYCLE_STATE = DOT_CLASS + "removeNonceAndSetLifecycleState";

    private static final long serialVersionUID = 1L;

    public PageRegistrationFinish() {
        super();
        init();
    }

    private void init() {

        OperationResult result = new OperationResult(OPERATION_FINISH_REGISTRATION);

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!authentication.isAuthenticated()) {
                LOGGER.error("Unauthenticated request");
                String msg = createStringResource("PageSelfRegistration.unauthenticated").getString();
                getSession().error(createStringResource(msg));
                result.recordFatalError(msg);
                initLayout(result);
                throw new RestartResponseException(PageSelfRegistration.class);
            }
            FocusType user = ((MidPointPrincipal)authentication.getPrincipal()).getFocus();
            PrismObject<UserType> administrator = getAdministratorPrivileged(result);

            assignDefaultRoles(user.getOid(), administrator, result);
            result.computeStatus();
            if (result.getStatus() == OperationResultStatus.FATAL_ERROR) {
                LOGGER.error("Failed to assign default roles, {}", result.getMessage());
            } else {
                NonceType nonceClone = user.getCredentials().getNonce().clone();
                removeNonceAndSetLifecycleState(user.getOid(), nonceClone, administrator, result);
                assignAdditionalRoleIfPresent(user.getOid(), nonceClone, administrator, result);
                result.computeStatus();
            }
            initLayout(result);
        } catch (CommonException|AuthenticationException e) {
            result.computeStatus();
            initLayout(result);
        }
    }

    private void assignDefaultRoles(String userOid, PrismObject<UserType> administrator, OperationResult parentResult) throws CommonException {
        List<ObjectReferenceType> rolesToAssign = getSelfRegistrationConfiguration().getDefaultRoles();
        if (CollectionUtils.isEmpty(rolesToAssign)) {
            return;
        }

        OperationResult result = parentResult.createSubresult(OPERATION_ASSIGN_DEFAULT_ROLES);
        try {
            PrismContext prismContext = getPrismContext();
            List<AssignmentType> assignmentsToCreate = rolesToAssign.stream()
                    .map(ref -> ObjectTypeUtil.createAssignmentTo(ref))
                    .collect(Collectors.toList());
            ObjectDelta<Objectable> delta = prismContext.deltaFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT).addRealValues(assignmentsToCreate)
                    .asObjectDelta(userOid);
            runAsChecked(() -> {
                Task task = createSimpleTask(OPERATION_ASSIGN_DEFAULT_ROLES);
                WebModelServiceUtils.save(delta, result, task, PageRegistrationFinish.this);
                return null;
            }, administrator);
        } catch (CommonException|RuntimeException e) {
            result.recordFatalError(getString("PageRegistrationConfirmation.message.assignDefaultRoles.fatalError"), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void removeNonceAndSetLifecycleState(String userOid, NonceType nonce, PrismObject<UserType> administrator,
            OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createSubresult(OPERATION_REMOVE_NONCE_AND_SET_LIFECYCLE_STATE);
        try {
            runAsChecked(() -> {
                Task task = createSimpleTask(OPERATION_REMOVE_NONCE_AND_SET_LIFECYCLE_STATE);
                ObjectDelta<UserType> delta = getPrismContext().deltaFactory().object()
                        .createModificationDeleteContainer(UserType.class, userOid,
                        ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_NONCE),
                                nonce);
                delta.addModificationReplaceProperty(UserType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE);
                WebModelServiceUtils.save(delta, result, task, PageRegistrationFinish.this);
                return null;
            }, administrator);
        } catch (CommonException|RuntimeException e) {
            result.recordFatalError(getString("PageRegistrationConfirmation.message.removeNonceAndSetLifecycleState.fatalError"), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove nonce and set lifecycle state", e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void assignAdditionalRoleIfPresent(String userOid, NonceType nonceType,
            PrismObject<UserType> administrator, OperationResult parentResult) throws CommonException {
        if (nonceType.getName() == null) {
            return;
        }
        OperationResult result = parentResult.createSubresult(OPERATION_ASSIGN_ADDITIONAL_ROLE);
        try {
            runAsChecked(() -> {
                Task task = createAnonymousTask(OPERATION_ASSIGN_ADDITIONAL_ROLE);
                ObjectDelta<UserType> assignRoleDelta;
                AssignmentType assignment = new AssignmentType();
                assignment.setTargetRef(ObjectTypeUtil.createObjectRef(nonceType.getName(), ObjectTypes.ABSTRACT_ROLE));
                getPrismContext().adopt(assignment);
                List<ItemDelta> userDeltas = new ArrayList<>();
                userDeltas.add(getPrismContext().deltaFactory().container().createModificationAdd(UserType.F_ASSIGNMENT,
                        UserType.class, assignment));
                assignRoleDelta = getPrismContext().deltaFactory().object().createModifyDelta(userOid, userDeltas, UserType.class
                );
                assignRoleDelta.setPrismContext(getPrismContext());
                WebModelServiceUtils.save(assignRoleDelta, result, task, PageRegistrationFinish.this);
                return null;
            }, administrator);
        } catch (CommonException|RuntimeException e) {
            result.recordFatalError(getString("PageRegistrationConfirmation.message.assignAdditionalRoleIfPresent.fatalError"), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assign additional role", e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void initLayout(final OperationResult result) {

        WebMarkupContainer successPanel = new WebMarkupContainer(ID_SUCCESS_PANEL);
        add(successPanel);
        successPanel.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return result.getStatus() != OperationResultStatus.FATAL_ERROR;
            }

            @Override
            public boolean isEnabled() {
                return result.getStatus() != OperationResultStatus.FATAL_ERROR;
            }
        });

        Label successMessage = new Label(ID_LABEL_SUCCESS,
                createStringResource("PageRegistrationConfirmation.confirmation.successful"));
        successPanel.add(successMessage);

        WebMarkupContainer errorPanel = new WebMarkupContainer(ID_ERROR_PANEL);
        add(errorPanel);
        errorPanel.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return result.getStatus() == OperationResultStatus.FATAL_ERROR;
            }

            @Override
            public boolean isVisible() {
                return result.getStatus() == OperationResultStatus.FATAL_ERROR;
            }
        });
        Label errorMessage = new Label(ID_LABEL_ERROR,
                createStringResource("PageRegistrationConfirmation.confirmation.error"));
        errorPanel.add(errorMessage);

    }

}
