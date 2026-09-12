/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.self;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.gui.impl.page.login.module.PageLogin;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Sets the password to accounts that were created without it (shadows with {@code purpose=incomplete}),
 * typically because the user password is stored hashed and midPoint had no cleartext when the account was created.
 *
 * The page is the last step of the authentication sequence bound to the account activation channel.
 * The user is already authenticated by the mail nonce from the activation link and by the login form;
 * the cleartext password entered in the login form is taken from the authenticated module and propagated
 * to the user's own incomplete shadows. All operations run as the authenticated principal with the
 * authorizations granted by the channel, nothing runs privileged.
 */
@SuppressWarnings("unused")
@PageDescriptor(
        urls = { @Url(mountUrl = SchemaConstants.ACCOUNT_ACTIVATION_PREFIX, matchUrlForSecurity = SchemaConstants.ACCOUNT_ACTIVATION_PREFIX) },
        action = { @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ACCOUNT_ACTIVATION_URL) })
public class PageAccountActivation extends AbstractPageLogin {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAccountActivation.class);

    private static final String DOT_CLASS = PageAccountActivation.class.getName() + ".";
    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";
    private static final String OPERATION_ACTIVATE_SHADOWS = DOT_CLASS + "activateShadows";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ACCOUNT_NAME = "accountName";
    private static final String ID_NOTHING_TO_ACTIVATE = "nothingToActivate";
    private static final String ID_ACTIVATE = "activate";

    private final IModel<List<ShadowType>> shadowsToActivateModel = new LoadableDetachableModel<>() {

        @Serial private static final long serialVersionUID = 1L;

        @Override
        protected List<ShadowType> load() {
            return loadShadowsToActivate();
        }
    };

    @Override
    protected boolean isBackButtonVisible() {
        return false;
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageAccountActivation.title");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return createStringResource("PageAccountActivation.description");
    }

    @Override
    public Task createSimpleTask(String operation) {
        Task task = createAnonymousTask(operation);
        task.setChannel(SchemaConstants.CHANNEL_ACCOUNT_ACTIVATION_URI);
        FocusType principalFocus = getPrincipalFocus();
        if (principalFocus != null) {
            task.setOwner(principalFocus.asPrismObject());
        }
        return task;
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        add(form);

        ListView<ShadowType> accounts = new ListView<>(ID_ACCOUNTS, shadowsToActivateModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ShadowType> item) {
                item.add(new Label(ID_ACCOUNT_NAME, () -> describeShadow(item.getModelObject())));
            }
        };
        form.add(accounts);

        Label nothingToActivate = new Label(ID_NOTHING_TO_ACTIVATE,
                createStringResource("PageAccountActivation.nothing.to.activate"));
        nothingToActivate.add(new VisibleBehaviour(() -> shadowsToActivateModel.getObject().isEmpty()));
        form.add(nothingToActivate);

        AjaxButton activate = new AjaxButton(ID_ACTIVATE, createStringResource("PageAccountActivation.button.activate")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                activatePerformed(target);
            }
        };
        activate.add(new VisibleBehaviour(() -> !shadowsToActivateModel.getObject().isEmpty()));
        form.add(activate);
    }

    private String describeShadow(ShadowType shadow) {
        String resourceName = shadow.getResourceRef() != null && shadow.getResourceRef().getTargetName() != null
                ? shadow.getResourceRef().getTargetName().getOrig() : "";
        return createStringResource("PageAccountActivation.account.description", WebComponentUtil.getName(shadow), resourceName).getString();
    }

    private List<ShadowType> loadShadowsToActivate() {
        FocusType focus = getPrincipalFocus();
        List<ShadowType> shadows = new ArrayList<>();
        if (!(focus instanceof UserType user)) {
            return shadows;
        }
        Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .noFetch()
                .resolveNames()
                .build();
        for (ObjectReferenceType linkRef : user.getLinkRef()) {
            try {
                PrismObject<ShadowType> shadow = getModelService().getObject(ShadowType.class, linkRef.getOid(), options, task, result);
                if (shadow.asObjectable().getPurpose() == ShadowPurposeType.INCOMPLETE) {
                    shadows.add(shadow.asObjectable());
                }
            } catch (Exception e) {
                // Dead or otherwise unreadable shadow. It cannot be activated anyway, just skip it.
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't load shadow {} of user {}", e, linkRef.getOid(), user);
            }
        }
        return shadows;
    }

    private void activatePerformed(AjaxRequestTarget target) {
        List<ShadowType> shadowsToActivate = shadowsToActivateModel.getObject();
        if (shadowsToActivate.isEmpty()) {
            getSession().warn(getString("PageAccountActivation.nothing.to.activate"));
            finish();
        }

        String password = getPasswordFromAuthentication();
        if (StringUtils.isEmpty(password)) {
            LOGGER.error("Password is not available in the authentication sequence used for account activation, "
                    + "the sequence has to contain a successful login form module");
            getSession().error(getString("PageAccountActivation.password.not.available"));
            finish();
        }

        ProtectedStringType passwordValue = new ProtectedStringType();
        passwordValue.setClearValue(password);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        for (ShadowType shadow : shadowsToActivate) {
            ObjectDelta<ShadowType> shadowDelta = getPrismContext().deltaFactory().object()
                    .createModificationReplaceProperty(ShadowType.class, shadow.getOid(),
                            SchemaConstants.PATH_PASSWORD_VALUE, passwordValue.clone());
            shadowDelta.addModificationReplaceProperty(ShadowType.F_PURPOSE, ShadowPurposeType.REGULAR);
            deltas.add(shadowDelta);
        }

        Task task = createSimpleTask(OPERATION_ACTIVATE_SHADOWS);
        OperationResult result = task.getResult();
        try {
            getModelService().executeChanges(deltas, null, task, result);
            result.computeStatusIfUnknown();
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't activate accounts of {}", e, getPrincipalFocus());
            result.recordFatalError(getString("PageAccountActivation.account.activation.failed"), e);
        }

        if (result.isSuccess()) {
            getSession().success(getString("PageAccountActivation.account.activation.successful"));
            finish();
        }
        showResult(result);
        target.add(getFeedbackPanel());
    }

    /**
     * The cleartext password is the one entered in the login form module of the current (account activation)
     * authentication sequence. It is never re-asked on this page, so there is no password check outside of
     * the authentication sequence.
     */
    private String getPasswordFromAuthentication() {
        MidpointAuthentication mpAuthentication = AuthUtil.getMidpointAuthentication();
        for (ModuleAuthentication module : mpAuthentication.getAuthentications()) {
            if (!AuthenticationModuleNameConstants.LOGIN_FORM.equals(module.getModuleTypeName())
                    || module.getState() != AuthenticationModuleState.SUCCESSFULLY) {
                continue;
            }
            Authentication moduleToken = module.getAuthentication();
            if (moduleToken != null && moduleToken.getCredentials() instanceof String credentials) {
                return credentials;
            }
        }
        return null;
    }

    /** The activation session is one-shot, whatever the outcome. */
    private void finish() {
        AuthUtil.clearMidpointAuthentication();
        throw new RestartResponseException(PageLogin.class);
    }
}
