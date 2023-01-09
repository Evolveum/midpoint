/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@PageDescriptor(urls = {
        @Url(mountUrl = "/attributeVerification", matchUrlForSecurity = "/attributeVerification")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION)
public class PageAttributeVerification extends PageAuthenticationBase {
    private static final long serialVersionUID = 1L;


    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";
    private static final String ID_SUBMIT_BUTTON = "submit";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_CSRF_FIELD = "csrfField";

    LoadableDetachableModel<List<ItemPathType>> attributesPathModel;
    private LoadableDetachableModel<UserType> userModel;
    private HashMap<ItemPathType, String> attributeValuesMap = new HashMap<>();

    public PageAttributeVerification() {
    }

    @Override
    protected void initModels() {
        userModel = new LoadableDetachableModel<>() {
            @Override
            protected UserType load() {
                MidPointPrincipal principal = AuthUtil.getPrincipalUser();
                return principal != null ? (UserType) principal.getFocus() : null;
            }
        };
        attributesPathModel = new LoadableDetachableModel<List<ItemPathType>>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ItemPathType> load() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (!(authentication instanceof MidpointAuthentication)) {
                    getSession().error(getString("No midPoint authentication is found"));
                    throw new RestartResponseException(PageError.class);
                }
                MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
                ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
                if (moduleAuthentication == null
                        && !AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION.equals(moduleAuthentication.getModuleTypeName())) {
                    getSession().error(getString("No authentication module is found"));
                    throw new RestartResponseException(PageError.class);
                }
                if (StringUtils.isEmpty(moduleAuthentication.getModuleIdentifier())) {
                    getSession().error(getString("No module identifier is defined"));
                    throw new RestartResponseException(PageError.class);
                }
                AttributeVerificationAuthenticationModuleType module = getModuleByIdentifier(moduleAuthentication.getModuleIdentifier());
                if (module == null) {
                    getSession().error(getString("No module with identifier \"" + moduleAuthentication.getModuleIdentifier() + "\" is found"));
                    throw new RestartResponseException(PageError.class);
                }
                return module.getPath();
            }
        };
    }

    private AttributeVerificationAuthenticationModuleType getModuleByIdentifier(String moduleIdentifier) {
        if (StringUtils.isEmpty(moduleIdentifier)) {
            return null;
        }
        UserType user = userModel.getObject();
        if (user == null) {
            getSession().error(getString("User not found"));
            throw new RestartResponseException(PageError.class);
        }
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(((UserType) user).asPrismObject());
        if (securityPolicy == null || securityPolicy.getAuthentication() == null) {
            getSession().error(getString("Security policy not found"));
            throw new RestartResponseException(PageError.class);
        }
        return securityPolicy.getAuthentication().getModules().getAttributeVerification()
                .stream()
                .filter(m -> moduleIdentifier.equals(m.getIdentifier()) || moduleIdentifier.equals(m.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);

        initAttributesLayout(form);

        initButtons(form);
    }

    private void initAttributesLayout(MidpointForm<?> form) {
        ListView<ItemPathType> attributesPanel = new ListView<ItemPathType>(ID_ATTRIBUTES, attributesPathModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ItemPathType> item) {
                Label attributeNameLabel = new Label(ID_ATTRIBUTE_NAME, Model.of(item.getModelObject().getItemPath()));
                item.add(attributeNameLabel);

                RequiredTextField<String> attributeValue = new RequiredTextField<>(ID_ATTRIBUTE_VALUE, Model.of());
                attributeValue.setOutputMarkupId(true);
                attributeValue.add(new AjaxFormComponentUpdatingBehavior("blur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateAttributeValue(item.getModelObject(), attributeValue.getValue());
                    }
                });
                item.add(attributeValue);
            }
        };
        attributesPanel.setOutputMarkupId(true);
        form.add(attributesPanel);
    }

    private void updateAttributeValue(ItemPathType path, String value) {
        if (attributeValuesMap.containsKey(path)) {
            attributeValuesMap.replace(path, value);
        } else {
            attributeValuesMap.put(path, value);
        }
    }

    private void initButtons(MidpointForm form) {
        AjaxSubmitButton submit = new AjaxSubmitButton(ID_SUBMIT_BUTTON, createStringResource("PageAttributeVerification.verifyAttributeButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
//                verifyAttributes(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

        };
        form.add(submit);
        form.add(createBackButton(ID_BACK_BUTTON));
    }

    @Override
    protected ObjectQuery createStaticFormQuery() {
        String username = "";
        return getPrismContext().queryFor(UserType.class).item(UserType.F_NAME)
                .eqPoly(username).matchingNorm().build();
    }

    @Override
    protected DynamicFormPanel<UserType> getDynamicForm() {
        return null;
    }

}
