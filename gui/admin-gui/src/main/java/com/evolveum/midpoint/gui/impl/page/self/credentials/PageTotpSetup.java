/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.self.credentials;

import java.io.Serial;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.component.otp.OtpPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialsType;

/**
 * Page where user is forced to set up TOTP credential right after successful authentication.
 * Used when TOTP module in authentication sequence has emptyCredentialsPolicy = forceSetup
 * and user doesn't have any (verified) TOTP credential yet. Nothing else is accessible until
 * the credential is created.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = AuthConstants.PATH_TOTP_SETUP, matchUrlForSecurity = AuthConstants.PATH_TOTP_SETUP)
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_CREDENTIALS_URL,
                        label = "PageSelfCredentials.auth.credentials.label",
                        description = "PageSelfCredentials.auth.credentials.description") })
public class PageTotpSetup extends AbstractPageLogin<ModuleAuthentication> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageTotpSetup.class);

    private static final String DOT_CLASS = PageTotpSetup.class.getName() + ".";
    private static final String OPERATION_LOAD_PRINCIPAL = DOT_CLASS + "loadPrincipalObject";
    private static final String OPERATION_CREATE_CREDENTIAL = DOT_CLASS + "createCredential";
    private static final String OPERATION_SAVE_CREDENTIAL = DOT_CLASS + "saveCredential";

    private static final String ID_FORM = "form";
    private static final String ID_OTP = "otp";
    private static final String ID_SUBMIT = "submit";

    private IModel<FocusType> focusModel;

    private IModel<OtpCredentialType> credentialModel;

    public PageTotpSetup() {
        if (!isSetupRequired()) {
            throw new RestartResponseException(getMidpointApplication().getHomePage());
        }
    }

    private boolean isSetupRequired() {
        MidpointAuthentication authentication = AuthUtil.getMidpointAuthenticationNotRequired();
        return authentication != null && authentication.isCredentialSetupRequired();
    }

    @Override
    protected void initCustomLayout() {
        focusModel = new LoadableDetachableModel<>() {

            @Override
            protected FocusType load() {
                MidPointPrincipal principal = getPrincipal();

                Task task = createSimpleTask(OPERATION_LOAD_PRINCIPAL);
                PrismObject<FocusType> focus = WebModelServiceUtils.loadObject(
                        FocusType.class, principal.getOid(), PageTotpSetup.this, task, task.getResult());

                return focus != null ? focus.asObjectable() : principal.getFocus();
            }
        };

        // The secret is kept only in session until the code is verified, unverified credential is never stored.
        credentialModel = new LoadableModel<>(false) {

            @Override
            protected OtpCredentialType load() {
                MidPointApplication application = MidPointApplication.get();

                Task task = createSimpleTask(OPERATION_CREATE_CREDENTIAL);
                OtpCredentialType credential = application.getOtpManager()
                        .createOtpCredential(focusModel.getObject().asPrismObject(), task, task.getResult());
                try {
                    // page (and this model with it) is serialized to page store, secret must not be there in clear text
                    application.getProtector().encrypt(credential.getSecret());
                } catch (EncryptionException ex) {
                    throw new SystemException("Couldn't encrypt TOTP secret", ex);
                }

                return credential;
            }
        };

        MidpointForm<Void> form = new MidpointForm<>(ID_FORM);
        add(form);

        OtpPanel<FocusType> otp = OtpPanel.createPanel(ID_OTP, focusModel, credentialModel);
        form.add(otp);

        form.add(new AjaxSubmitButton(ID_SUBMIT, createStringResource("OtpPopupPanel.verifyAndEnable")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                otp.onValidationError(target);
                target.add(getFeedbackPanel());
            }
        });
    }

    private void savePerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_SAVE_CREDENTIAL);
        OperationResult result = task.getResult();

        FocusType focus = focusModel.getObject();
        OtpCredentialType credential = credentialModel.getObject();
        if (BooleanUtils.isNotTrue(credential.isVerified())) {
            // should not happen, code is validated by the panel before form submit gets here
            error(getString("OtpPanel.verifyFailed"));
            target.add(getFeedbackPanel());
            return;
        }

        try {
            ObjectDelta<? extends FocusType> delta = getPrismContext().deltaFor(focus.getClass())
                    .item(ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_OTPS, OtpCredentialsType.F_TOTP))
                    .add(credential.clone())
                    .asObjectDelta(focus.getOid());

            WebModelServiceUtils.save(delta, result, task, this);
        } catch (CommonException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save TOTP credential", ex);
            result.recordFatalError(getString("PageTotpSetup.message.save.fatalError"), ex);
        }

        result.computeStatusIfUnknown();

        if (!result.isAcceptable()) {
            // login-like pages can't show operation result, only plain messages
            String message = result.getMessage();
            error(StringUtils.isNotEmpty(message) ? message : getString("PageTotpSetup.message.save.fatalError"));
            target.add(getFeedbackPanel());
            return;
        }

        AuthUtil.getMidpointAuthentication().credentialSetupCompleted();

        try {
            getModelInteractionService().refreshPrincipal(focus.getOid(), focus.getClass());
        } catch (CommonException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't refresh principal after TOTP credential setup", ex);
        }

        setResponsePage(getMidpointApplication().getHomePage());
    }

    @Override
    protected ModuleAuthentication getAuthenticationModuleConfiguration() {
        // authentication sequence is already finished, there's no module being processed
        return null;
    }

    @Override
    protected boolean isBackButtonVisible() {
        return true;
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageTotpSetup.title");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return createStringResource("PageTotpSetup.description");
    }
}
