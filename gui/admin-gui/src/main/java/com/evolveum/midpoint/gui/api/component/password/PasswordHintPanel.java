/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PasswordHintPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_HINT= "hint";
    private final IModel<String> hintModel;
    private final IModel<ProtectedStringType> passwordModel;
    private boolean isReadonly;

    public PasswordHintPanel(String id, IModel<String> hintModel, IModel<ProtectedStringType> passwordModel, boolean isReadonly){
        super(id);
        this.hintModel = hintModel;
        this.passwordModel = passwordModel;
        this.isReadonly = isReadonly;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final TextField<String> hint = new TextField<>(ID_HINT, hintModel);
        hint.setOutputMarkupId(true);
        hint.add(new EnableBehaviour(() -> !isReadonly));
        hint.add(new PasswordHintValidator(passwordModel));
        add(hint);
    }

    public List<FeedbackMessage> collectHintFeedbackMessages() {
        List<FeedbackMessage> feedbackMessages = new ArrayList<>();
        FormComponent hintInput = getBaseFormComponent();
        if (hintInput.getFeedbackMessages() != null && hintInput.getFeedbackMessages().hasMessage(0)) {
            feedbackMessages.addAll(hintInput.getFeedbackMessages().messages(null));
        }
        return feedbackMessages;
    }

    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_HINT);
    }

    private static class PasswordHintValidator implements IValidator<String> {

        private final IModel<ProtectedStringType> passwordModel;

        private PasswordHintValidator(IModel<ProtectedStringType> passwordModel) {
            this.passwordModel = passwordModel;
        }

        @Override
        public void validate(IValidatable<String> validatable) {
            String hintValue = validatable.getValue();
            if (StringUtils.isEmpty(hintValue)) {
                return;
            }

            ProtectedStringType passwordValue = passwordModel.getObject();
            WebComponentUtil.encryptProtectedString(passwordValue, false,
                    MidPointApplication.get());
            String passwordString = passwordValue != null ? passwordValue.getClearValue() : null;
            if (StringUtils.isNotEmpty(passwordString) && passwordString.contains(hintValue)) {
                ValidationError err = new ValidationError();
                err.addKey("PasswordHintPanel.incorrectHint.error");
                validatable.error(err);
            }
        }
    }

}
