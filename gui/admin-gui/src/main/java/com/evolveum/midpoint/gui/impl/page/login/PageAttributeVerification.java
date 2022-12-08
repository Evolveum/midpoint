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
import com.evolveum.midpoint.web.security.util.SecurityQuestionDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
        @Url(mountUrl = "/verification/attributes", matchUrlForSecurity = "/verification/attributes")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION)
public class PageAttributeVerification extends PageAuthenticationBase {
    private static final long serialVersionUID = 1L;


    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";
    private static final String ID_SUBMIT_BUTTON = "submit";
    private static final String ID_BACK_BUTTON = "back";

    LoadableDetachableModel<List<ItemPathType>> attributesPathModel;
    private LoadableDetachableModel<UserType> userModel;
    private HashMap<ItemPathType, String> attributeValuesMap;

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
                UserType user = userModel.getObject();
                if (user == null) {
                    getSession().error(getString("User not found"));
                    throw new RestartResponseException(PageAttributeVerification.class);
                }
                SecurityPolicyType securityPolicy = resolveSecurityPolicy(((UserType) user).asPrismObject());
                if (securityPolicy == null) {
                    getSession().error(getString("Security policy not found"));
                    throw new RestartResponseException(PageAttributeVerification.class);
                }
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof MidpointAuthentication) {
                    MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
                    ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
                    if (moduleAuthentication != null
                            && AuthenticationModuleNameConstants.ATTRIBUTE_VERIFICATION.equals(moduleAuthentication.getModuleTypeName())) {

                    }
                }
                return new ArrayList<>();
            }
        };
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_MAIN_FORM);
        add(form);

        initAttributesLayout(form);

        initButtons(form);

    }

    private void initAttributesLayout(MidpointForm<?> form) {
        ListView<ItemPathType> attributesPanel = new ListView<ItemPathType>(ID_ATTRIBUTES, attributesPathModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ItemPathType> item) {
                Label attributeNameLabel = new Label(ID_ATTRIBUTE_NAME, new PropertyModel<String>(
                        item.getModelObject().getItemPath(), "questionText"));
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

        AjaxButton back = new AjaxButton(ID_BACK_BUTTON) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                userModel.detach();
                attributesPathModel.detach();
                target.add();
            }
        };
        form.add(back);

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
