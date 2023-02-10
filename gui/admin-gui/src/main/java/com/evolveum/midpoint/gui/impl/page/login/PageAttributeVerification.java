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
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeVerificationAuthenticationModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;

@PageDescriptor(urls = {
        @Url(mountUrl = "/attributeVerification", matchUrlForSecurity = "/attributeVerification")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION)
public class PageAttributeVerification extends PageAuthenticationBase {
    private static final long serialVersionUID = 1L;


    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ATTRIBUTE_VALUES = "attributeValues";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";
    private static final String ID_SUBMIT_BUTTON = "submit";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_CSRF_FIELD = "csrfField";

    LoadableDetachableModel<List<ItemPathType>> attributesPathModel;
    private LoadableDetachableModel<UserType> userModel;
    private final HashMap<ItemPathType, String> attributeValuesMap = new HashMap<>();
    IModel<String> attrValuesModel;

    public PageAttributeVerification() {
    }

    @Override
    protected void initModels() {
        attrValuesModel = Model.of();
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
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(user.asPrismObject());
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
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);

        HiddenField<String> verified = new HiddenField<>(ID_ATTRIBUTE_VALUES, attrValuesModel);
        verified.setOutputMarkupId(true);
        form.add(verified);

        initAttributesLayout(form);

        initButtons(form);
    }

    private void initAttributesLayout(MidpointForm<?> form) {
        ListView<ItemPathType> attributesPanel = new ListView<ItemPathType>(ID_ATTRIBUTES, attributesPathModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ItemPathType> item) {
                Label attributeNameLabel = new Label(ID_ATTRIBUTE_NAME, resolveAttributeLabel(item.getModelObject()));
                item.add(attributeNameLabel);

                RequiredTextField<String> attributeValue = new RequiredTextField<>(ID_ATTRIBUTE_VALUE, Model.of());
                attributeValue.setOutputMarkupId(true);
                attributeValue.add(new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateAttributeValue(item.getModelObject(), attributeValue.getValue());
                        updateAttributeValuesModel();
                        target.add(getVerifiedField());
                    }
                });
                item.add(attributeValue);
            }
        };
        attributesPanel.setOutputMarkupId(true);
        form.add(attributesPanel);
    }

    private String resolveAttributeLabel(ItemPathType path) {
        if (path == null) {
            return "";
        }
        ItemDefinition<?> def = userModel.getObject().asPrismObject().getDefinition().findItemDefinition(path.getItemPath());
        return WebComponentUtil.getItemDefinitionDisplayName(def);
    }

    private void updateAttributeValue(ItemPathType path, String value) {
        if (attributeValuesMap.containsKey(path)) {
            attributeValuesMap.replace(path, value);
        } else {
            attributeValuesMap.put(path, value);
        }
    }

    private void initButtons(MidpointForm form) {
        form.add(createBackButton(ID_BACK_BUTTON));
    }

    private void updateAttributeValuesModel() {
        attrValuesModel.setObject(generateAttributeValuesString());
    }

    private Component getVerifiedField() {
        return  get(ID_MAIN_FORM).get(ID_ATTRIBUTE_VALUES);
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

    private String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null
                    && AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION.equals(moduleAuthentication.getModuleTypeName())){
                String prefix = moduleAuthentication.getPrefix();
                return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
            }
        }

        String key = "web.security.flexAuth.unsupported.auth.type";
        error(getString(key));
        return "/midpoint/spring_security_login";
    }

    private String generateAttributeValuesString() {
        JSONArray attrValues = new JSONArray();
        attributeValuesMap.entrySet().forEach(entry -> {
            JSONObject json  = new JSONObject();
            json.put(AuthConstants.ATTR_VERIFICATION_J_PATH, entry.getKey());
            json.put(AuthConstants.ATTR_VERIFICATION_J_VALUE, entry.getValue());
            attrValues.put(json);
        });
        if (attrValues.length() == 0) {
            return null;
        }
        return attrValues.toString();
    }

}
