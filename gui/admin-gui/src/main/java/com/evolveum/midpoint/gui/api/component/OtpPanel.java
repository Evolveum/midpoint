/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.QRCodeUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class OtpPanel extends InputPanel {

    private static final Trace LOGGER = TraceManager.getTrace(OtpPanel.class);

    private static final String DOT_CLASS = OtpPanel.class.getName() + ".";

    private static final String ID_NAME = "name";
    private static final String ID_QR = "qr";
    private static final String ID_SETUP_MANUALLY = "setupManually";
    private static final String ID_SECRET = "secret";
    private static final String ID_CODE = "code";
    private static final String ID_VERIFY = "verify";

    private final IModel<OtpCredentialType> model;

    private final IModel<Boolean> setupManuallyModel = Model.of(false);

    public OtpPanel(String id, IModel<OtpCredentialType> model) {
        super(id);

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
                try {
                    return protector.decryptString(protectedString);
                } catch (Exception e) {
                    LOGGER.error("Error decrypting OTP secret", e);
                    return null;
                }
            }
        };

        IModel<String> qrModel = new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                String url = MidPointApplication.get().getOtpManager().createOtpAuthUrl(model.getObject());
                if (url == null) {
                    // this shouldn't happen, but just in case
                    return "";
                }

                return QRCodeUtils.generateSvg(url);
            }
        };

        Label secret = new Label(ID_SECRET, secretModel);
        secret.setOutputMarkupPlaceholderTag(true);
        secret.add(new VisibleBehaviour(() -> secretModel.getObject() != null && setupManuallyModel.getObject()));
        add(secret);

        Label qr = new Label(ID_QR, qrModel);
        qr.setRenderBodyOnly(true);
        qr.setEscapeModelStrings(false);
        add(qr);

        IModel<String> setupManuallyLabelModel = () -> {

            String key = setupManuallyModel.getObject() ? "OtpSetupPanel.setupManually.hide" : "OtpSetupPanel.setupManually.show";

            return getString(key);
        };

        AjaxButton setupManually = new AjaxButton(ID_SETUP_MANUALLY, setupManuallyLabelModel) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSetupManuallyClicked(target, secret);
            }
        };
        add(setupManually);

        TextField<String> code = new TextField<>(ID_CODE, Model.of());  // todo fix model
        code.setOutputMarkupId(true);
        add(code);

        AjaxButton verify = new AjaxButton(ID_VERIFY, createStringResource("OtpSetupPanel.verify")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onVerifyClicked(target, code.getModelObject());
            }
        };
        add(verify);
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return (FormComponent<?>) get(ID_CODE);
    }

    private void onSetupManuallyClicked(AjaxRequestTarget target, Component secret) {
        setupManuallyModel.setObject(true);

        target.add(secret);
    }

    private void onVerifyClicked(AjaxRequestTarget target, String code) {
        // todo verify code
    }
}
