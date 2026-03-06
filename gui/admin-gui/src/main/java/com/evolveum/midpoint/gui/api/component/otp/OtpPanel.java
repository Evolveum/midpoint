/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.otp;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.authentication.api.OtpManager;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.message.SimpleFeedbackPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.QRCodeUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class OtpPanel<F extends FocusType> extends InputPanel {

    private static final Trace LOGGER = TraceManager.getTrace(OtpPanel.class);

    private static final String DOT_CLASS = OtpPanel.class.getName() + ".";
    private static final String OPERATION_CREATE_AUTH_URL = DOT_CLASS + "createAuthUrl";
    private static final String OPERATION_VERIFY_CODE = DOT_CLASS + "verifyCode";

    private static final String ID_NAME = "name";
    private static final String ID_QR = "qr";
    private static final String ID_SECRET = "secret";
    private static final String ID_CODE_GROUP = "codeGroup";
    private static final String ID_CODE = "code";
    private static final String ID_CODE_FEEDBACK = "codeFeedback";
    private static final String ID_CODE_HELP = "codeHelp";

    private boolean editMode = false;

    private final IModel<F> focusModel;

    private final IModel<OtpCredentialType> model;

    private final IModel<Integer> codeModel = Model.of();

    public OtpPanel(String id, IModel<F> focusModel, IModel<OtpCredentialType> model) {
        super(id);

        this.focusModel = focusModel;
        this.model = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        IModel<String> nameModel = new PropertyModel<>(model, OtpCredentialType.F_NAME.getLocalPart());

        TextField<String> name = new TextField<>(ID_NAME, nameModel);
        name.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // intentionally left empty, we just want to update the model on blur, but we don't need to do anything else
            }
        });
        add(name);

        IModel<String> secretModel = new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                OtpCredentialType otpCredential = model.getObject();
                ProtectedStringType protectedString = otpCredential.getSecret();
                if (protectedString == null) {
                    return null;
                }

                Protector protector = MidPointApplication.get().getProtector();
                String secret = "";
                try {
                    secret = protector.decryptString(protectedString);
                } catch (Exception e) {
                    LOGGER.error("Error decrypting OTP secret", e);
                }

                return secret;
            }
        };

        IModel<String> qrModel = new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                OtpManager manager = MidPointApplication.get().getOtpManager();

                Task task = getPageBase().createSimpleTask(OPERATION_CREATE_AUTH_URL);
                OperationResult result = task.getResult();

                String url = manager.createOtpAuthUrl(focusModel.getObject().asPrismObject(), model.getObject(), task, result);
                return url != null ? QRCodeUtils.generateSvg(url) : "";
            }
        };

        TextField<String> secret = new TextField<>(ID_SECRET, secretModel);
        add(secret);

        Label qr = new Label(ID_QR, qrModel);
        qr.setRenderBodyOnly(true);
        qr.setEscapeModelStrings(false);
        qr.add(new VisibleBehaviour(() -> !editMode));
        add(qr);

        WebMarkupContainer codeGroup = new WebMarkupContainer(ID_CODE_GROUP);
        codeGroup.setOutputMarkupId(true);
        codeGroup.add(new VisibleBehaviour(() -> !editMode));
        add(codeGroup);

        TextField<Integer> code = new TextField<>(ID_CODE, codeModel);
        code.setLabel(createStringResource("OtpPanel.code.label"));
        code.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                refreshCodeGroup(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                onValidationError(target);
            }
        });
        code.setType(Integer.class);
        code.setOutputMarkupId(true);
        code.setRequired(true);
        code.add(new IValidator<>() {

            @Override
            public void validate(IValidatable<Integer> validatable) {
                Integer code = validatable.getValue();
                if (code == null) {
                    ValidationError error = new ValidationError(this);
                    error.addKey("OtpPanel.codeRequired");
                    validatable.error(error);
                    return;
                }

                OtpManager manager = MidPointApplication.get().getOtpManager();
                Task task = getPageBase().createSimpleTask(OPERATION_VERIFY_CODE);
                OperationResult result = task.getResult();

                boolean correct = manager.verifyOtpCredential(
                        focusModel.getObject().asPrismObject(), model.getObject(), code, task, result);
                if (!correct) {
                    ValidationError error = new ValidationError(this);
                    error.addKey("OtpPanel.verifyFailed");
                    validatable.error(error);
                }
            }
        });
        SimpleFeedbackPanel.addSimpleFeedbackAppender(code);
        codeGroup.add(code);

        SimpleFeedbackPanel codeFeedback = new SimpleFeedbackPanel(ID_CODE_FEEDBACK, new ComponentFeedbackMessageFilter(code));
        codeFeedback.setRenderBodyOnly(true);
        codeGroup.add(codeFeedback);

        WebMarkupContainer codeHelp = new WebMarkupContainer(ID_CODE_HELP);
        codeHelp.add(new VisibleBehaviour(() -> code.getFeedbackMessages().isEmpty()));
        codeGroup.add(codeHelp);
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return (FormComponent<?>) get(ID_CODE);
    }

    public void onValidationError(AjaxRequestTarget target) {
        refreshCodeGroup(target);
    }

    private void refreshCodeGroup(AjaxRequestTarget target) {
        target.add(get(ID_CODE_GROUP));
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
}
