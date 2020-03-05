/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.password;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.self.PageSelfProfile;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.jetbrains.annotations.NotNull;

/**
 * @author lazyman
 */
public class PasswordPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_LINK_CONTAINER = "linkContainer";
    private static final String ID_PASSWORD_SET = "passwordSet";
    private static final String ID_PASSWORD_REMOVE = "passwordRemove";
    private static final String ID_CHANGE_PASSWORD_LINK = "changePasswordLink";
    private static final String ID_REMOVE_PASSWORD_LINK = "removePasswordLink";
    private static final String ID_REMOVE_BUTTON_CONTAINER = "removeButtonContainer";
    private static final String ID_INPUT_CONTAINER = "inputContainer";
    private static final String ID_PASSWORD_ONE = "password1";
    private static final String ID_PASSWORD_TWO = "password2";

    private static final Trace LOGGER = TraceManager.getTrace(PasswordPanel.class);

    private boolean passwordInputVisble;
    private static boolean clearPasswordInput = false;

    public PasswordPanel(String id, IModel<ProtectedStringType> model) {
        this(id, model, false, model == null || model.getObject() == null);
    }

    public PasswordPanel(String id, IModel<ProtectedStringType> model, boolean isReadOnly, boolean isInputVisible) {
        super(id);
        this.passwordInputVisble = isInputVisible;
        initLayout(model, isReadOnly);
    }

    private void initLayout(final IModel<ProtectedStringType> model, final boolean isReadOnly) {
        setOutputMarkupId(true);
        final WebMarkupContainer inputContainer = new WebMarkupContainer(ID_INPUT_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return passwordInputVisble;
            }
        };
        inputContainer.setOutputMarkupId(true);
        add(inputContainer);

        final PasswordTextField password1 = new SecureModelPasswordTextField(ID_PASSWORD_ONE, new PasswordModel(model)){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (clearPasswordInput) {
                    tag.remove("value");
                }
            }

        };
        password1.setRequired(false);
        password1.setOutputMarkupId(true);
        password1.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        inputContainer.add(password1);

        final PasswordTextField password2 = new SecureModelPasswordTextField(ID_PASSWORD_TWO, new PasswordModel(Model.of(new ProtectedStringType())));
        password2.setRequired(false);
        password2.setOutputMarkupId(true);
        inputContainer.add(password2);

        password1.add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean required = !StringUtils.isEmpty(password1.getModelObject());
                password2.setRequired(required);

                changePasswordPerformed();
                //fix of MID-2463
//                target.add(password2);
//                target.appendJavaScript("$(\"#"+ password2.getMarkupId() +"\").focus()");
            }
        });
        password2.add(new PasswordValidator(password1));

        final WebMarkupContainer linkContainer = new WebMarkupContainer(ID_LINK_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !passwordInputVisble;
            }
        };
        inputContainer.setOutputMarkupId(true);
        linkContainer.setOutputMarkupId(true);
        add(linkContainer);

        final Label passwordSetLabel = new Label(ID_PASSWORD_SET, new ResourceModel("passwordPanel.passwordSet"));
        linkContainer.add(passwordSetLabel);

        final Label passwordRemoveLabel = new Label(ID_PASSWORD_REMOVE, new ResourceModel("passwordPanel.passwordRemoveLabel"));
        passwordRemoveLabel.setVisible(false);
        linkContainer.add(passwordRemoveLabel);

        AjaxLink<Void> link = new AjaxLink<Void>(ID_CHANGE_PASSWORD_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearPasswordInput = true;
                onLinkClick(target);
            }

            @Override
            public boolean isVisible() {
                return !passwordInputVisble && model != null && model.getObject() != null;
            }
        };
        link.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !isReadOnly;

            }
        });
        link.setBody(new ResourceModel("passwordPanel.passwordChange"));
        link.setOutputMarkupId(true);
        linkContainer.add(link);

        final WebMarkupContainer removeButtonContainer = new WebMarkupContainer(ID_REMOVE_BUTTON_CONTAINER);
        AjaxLink<Void> removePassword = new AjaxLink<Void>(ID_REMOVE_PASSWORD_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRemovePassword(model, target);
            }

        };
        removePassword.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                PageBase pageBase = (PageBase)getPage();
                if (pageBase == null){
                    return false;
                }
                if (pageBase instanceof PageSelfProfile){
                    return false;
                }
                if (pageBase instanceof PageUser && !((PageUser) pageBase).isLoggedInUserPage()
                        && model.getObject() != null){
                    return true;
                }
                return false;
            }
        });
        removePassword.setBody(new ResourceModel("passwordPanel.passwordRemove"));
        removePassword.setOutputMarkupId(true);
        removeButtonContainer.add(removePassword);
        add(removeButtonContainer);
    }

    private void onLinkClick(AjaxRequestTarget target) {
        passwordInputVisble = true;
        target.add(this);
    }

    private void onRemovePassword(IModel<ProtectedStringType> model, AjaxRequestTarget target) {
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_SET).setVisible(false);
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_REMOVE).setVisible(true);
        passwordInputVisble = false;
        model.setObject(null);
        target.add(this);
    }

    @Override
    public List<FormComponent> getFormComponents() {
        List<FormComponent> list = new ArrayList<>();
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE));
        list.add((FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_TWO));
        return list;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT_CONTAINER + ":" + ID_PASSWORD_ONE);
    }

    private static class PasswordValidator implements IValidator<String> {

        private PasswordTextField p1;

        private PasswordValidator(@NotNull PasswordTextField p1) {
            this.p1 = p1;
        }

        @Override
        public void validate(IValidatable<String> validatable) {
            String s1 = p1.getModelObject();
            String s2 = validatable.getValue();

            if (StringUtils.isEmpty(s1) && StringUtils.isEmpty(s2)) {
                return;
            }

            if (!Objects.equals(s1, s2)) {
                validatable = p1.newValidatable();
                ValidationError err = new ValidationError();
                err.addKey("passwordPanel.error");
                validatable.error(err);
            }
        }
    }

    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {
        private static final long serialVersionUID = 1L;

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("blur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }

    private static class PasswordModel implements IModel<String> {
        private static final long serialVersionUID = 1L;

        IModel<ProtectedStringType> psModel;

        PasswordModel(IModel<ProtectedStringType> psModel) {
            this.psModel = psModel;
        }

        @Override
        public void detach() {
            // Nothing to do
        }

        private Protector getProtector() {
            return ((MidPointApplication) Application.get()).getProtector();
        }

        @Override
        public String getObject() {
            ProtectedStringType ps = psModel.getObject();
            if (ps == null) {
                return null;
            } else {
                try {
                    return getProtector().decryptString(ps);
                } catch (EncryptionException e) {
                    throw new SystemException(e.getMessage(), e);   // todo handle somewhat better
                }
            }
        }

        @Override
        public void setObject(String object) {
            if (clearPasswordInput){
                clearPasswordInput = false;
                return;
            }
            if (object == null) {
                psModel.setObject(null);
            } else {
                if (psModel.getObject() == null) {
                    psModel.setObject(new ProtectedStringType());
                } else {
                    psModel.getObject().clear();
                }
                psModel.getObject().setClearValue(object);
                try {
                    getProtector().encrypt(psModel.getObject());
                } catch (EncryptionException e) {
                    throw new SystemException(e.getMessage(), e);   // todo handle somewhat better
                }
            }
        }
    }

    protected void changePasswordPerformed(){}
}
