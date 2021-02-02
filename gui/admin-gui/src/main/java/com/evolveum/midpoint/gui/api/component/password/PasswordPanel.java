/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.validator.StringLimitationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.self.PageOrgSelfProfile;
import com.evolveum.midpoint.web.page.self.PageRoleSelfProfile;
import com.evolveum.midpoint.web.page.self.PageServiceSelfProfile;
import com.evolveum.midpoint.web.page.self.PageUserSelfProfile;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jfree.util.Log;

/**
 * @author lazyman
 */
public class PasswordPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PasswordPanel.class);

    private static final String ID_LINK_CONTAINER = "linkContainer";
    private static final String ID_PASSWORD_SET = "passwordSet";
    private static final String ID_PASSWORD_REMOVE = "passwordRemove";
    private static final String ID_CHANGE_PASSWORD_LINK = "changePasswordLink";
    private static final String ID_REMOVE_PASSWORD_LINK = "removePasswordLink";
    private static final String ID_REMOVE_BUTTON_CONTAINER = "removeButtonContainer";
    private static final String ID_INPUT_CONTAINER = "inputContainer";
    private static final String ID_PASSWORD_ONE = "password1";
    private static final String ID_PASSWORD_TWO = "password2";
    private static final String ID_VALIDATION_CONTAINER = "validationContainer";
    private static final String ID_VALIDATION_PARENT_ITEMS = "validationParentItems";
    private static final String ID_VALIDATION_ITEMS = "validationItems";
    private static final String ID_VALIDATION_ITEM = "validationItem";

    private boolean passwordInputVisible;
    private static boolean clearPasswordInput = false;
    private static boolean setPasswordInput = false;
    private final PageBase pageBase;

    public PasswordPanel(String id, IModel<ProtectedStringType> model) {
        this(id, model, false, model == null || model.getObject() == null);
    }

    public <F extends FocusType> PasswordPanel(String id, IModel<ProtectedStringType> model, PrismObject<F> object,
            PageBase pageBase) {
        this(id, model, false, model == null || model.getObject() == null, object, pageBase);
    }

    public <F extends FocusType> PasswordPanel(String id, IModel<ProtectedStringType> model, boolean isReadOnly, boolean isInputVisible) {
        this(id, model,isReadOnly,isInputVisible, null, null);
    }

    public <F extends FocusType> PasswordPanel(String id, IModel<ProtectedStringType> model, boolean isReadOnly, boolean isInputVisible,
            PrismObject<F> object, PageBase pageBase) {
        super(id);
        this.passwordInputVisible = isInputVisible;
        this.pageBase = pageBase;
        initLayout(model, isReadOnly, object);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    private <F extends FocusType> void initLayout(final IModel<ProtectedStringType> model, final boolean isReadOnly, PrismObject<F> object) {
        setOutputMarkupId(true);
        final WebMarkupContainer inputContainer = new WebMarkupContainer(ID_INPUT_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return passwordInputVisible;
            }
        };
        inputContainer.setOutputMarkupId(true);
        add(inputContainer);

        PrismObject<ValuePolicyType> valuePolicy = null;
        try {
            if (object != null) {
                Task task = getPageBase().createSimpleTask("load value policy");
                CredentialsPolicyType credentials = getPageBase().getModelInteractionService().getCredentialsPolicy(object, task, task.getResult());
                if (credentials != null && credentials.getPassword() != null
                        && credentials.getPassword().getValuePolicyRef() != null) {
                    valuePolicy = WebModelServiceUtils.resolveReferenceNoFetch(
                            credentials.getPassword().getValuePolicyRef(), getPageBase(), task, task.getResult());

                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't load security policy for focus " + object, e);
        }

        PrismObject<ValuePolicyType> finalValuePolicy = valuePolicy;
        LoadableModel<List<StringLimitationResult>> limitationsModel = new LoadableModel<>() {
            @Override
            protected List<StringLimitationResult> load() {
                if (finalValuePolicy != null && object != null) {
                    Task task = getPageBase().createSimpleTask("validation of password");
                    try {
                        ProtectedStringType newValue = !setPasswordInput ? new ProtectedStringType() : model.getObject();
                        return getPageBase().getModelInteractionService().validateValue(
                                newValue, finalValuePolicy.asObjectable(), object, task, task.getResult());
                    } catch (Exception e) {
                        LOGGER.error("Couldn't validate password security policy", e);
                    }
                }
                return new ArrayList<>();
            }
        };

        final WebMarkupContainer validationContainer = new WebMarkupContainer(ID_VALIDATION_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !limitationsModel.getObject().isEmpty();
            }
        };
        validationContainer.setOutputMarkupId(true);
        inputContainer.add(validationContainer);

        final WebMarkupContainer validationParentContainer = new WebMarkupContainer(ID_VALIDATION_PARENT_ITEMS) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !limitationsModel.getObject().isEmpty();
            }
        };
        validationParentContainer.setOutputMarkupId(true);
        validationContainer.add(validationParentContainer);

        ListView<StringLimitationResult> validationItems = new ListView<>(ID_VALIDATION_ITEMS, limitationsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<StringLimitationResult> item) {
                StringLimitationPanel limitationPanel = new StringLimitationPanel(ID_VALIDATION_ITEM, item.getModel());
                limitationPanel.setOutputMarkupId(true);
                item.add(limitationPanel);
                item.add(AttributeModifier.append("class", new IModel<String>() {
                    @Override
                    public String getObject() {
                        return Boolean.TRUE.equals(item.getModelObject().isSuccess()) ? "list-group-item-success" : "";
                    }
                }));
            }
        };
        validationItems.setOutputMarkupId(true);
        validationParentContainer.add(validationItems);

        final PasswordTextField password1 = new SecureModelPasswordTextField(ID_PASSWORD_ONE, new PasswordModel(model)) {
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
        password1.add(new AjaxFormComponentUpdatingBehavior("keyup") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                limitationsModel.reset();
                target.add(validationParentContainer);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.milliseconds(500), true));
                attributes.setChannel(new AjaxChannel("Drop", AjaxChannel.Type.DROP));
            }
        });
        password2.add
                (new PasswordValidator(password1));

        final WebMarkupContainer linkContainer = new WebMarkupContainer(ID_LINK_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !passwordInputVisible;
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
                setPasswordInput = false;
                onLinkClick(target);
            }

            @Override
            public boolean isVisible() {
                return !passwordInputVisible && model != null && model.getObject() != null;
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
                PageBase pageBase = getPageBase();
                if (pageBase == null) {
                    return false;
                }
                if (pageBase instanceof PageUserSelfProfile || pageBase instanceof PageOrgSelfProfile
                        || pageBase instanceof PageRoleSelfProfile || pageBase instanceof PageServiceSelfProfile) {
                    return false;
                }
                if (pageBase instanceof PageAdminFocus && !((PageAdminFocus) pageBase).isLoggedInFocusPage()
                        && model.getObject() != null) {
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

    private PageBase getPageBase() {
        return pageBase;
    }

    private void onLinkClick(AjaxRequestTarget target) {
        passwordInputVisible = true;
        target.add(this);
    }

    private void onRemovePassword(IModel<ProtectedStringType> model, AjaxRequestTarget target) {
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_SET).setVisible(false);
        get(ID_LINK_CONTAINER).get(ID_PASSWORD_REMOVE).setVisible(true);
        passwordInputVisible = false;
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

        private final PasswordTextField p1;

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
            if (clearPasswordInput) {
                clearPasswordInput = false;
                setPasswordInput = false;
                return;
            }
            setPasswordInput = true;
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

    protected void changePasswordPerformed() {
    }
}
