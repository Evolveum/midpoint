/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.FocusIdentificationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModuleItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@PageDescriptor(urls = {
        @Url(mountUrl = "/focusIdentification", matchUrlForSecurity = "/focusIdentification")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.FOCUS_IDENTIFICATION)
public class PageFocusIdentification extends PageAbstractAuthenticationModule<FocusIdentificationModuleAuthentication> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_ATTRIBUTE_VALUES = "attributeValues";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";

    LoadableModel<List<ItemPathType>> attributesPathModel;
    private LoadableDetachableModel<UserType> userModel;
    IModel<String> attrValuesModel;

    public PageFocusIdentification() {
        initModels();
    }

    protected void initModels() {
        attrValuesModel = Model.of();
        userModel = new LoadableDetachableModel<>() {
            @Override
            protected UserType load() {
                return new UserType();
            }
        };
        attributesPathModel = new LoadableModel<>(false) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ItemPathType> load() {
                FocusIdentificationModuleAuthentication module = getAuthenticationModuleConfiguration();
                List<ModuleItemConfigurationType> itemConfigs = module.getModuleConfiguration();
                return itemConfigs.stream()
                        .map(ModuleItemConfigurationType::getPath)
                        .collect(Collectors.toList());
            }
        };
    }

    @Override
    protected void initModuleLayout(MidpointForm form) {
        HiddenField<String> verified = new HiddenField<>(ID_ATTRIBUTE_VALUES, attrValuesModel);
        verified.setOutputMarkupId(true);
        form.add(verified);

        initAttributesLayout(form);
    }

    private void initAttributesLayout(MidpointForm<?> form) {

        Label attributeNameLabel = new Label(ID_ATTRIBUTE_NAME, resolveAttributeLabel(attributesPathModel));
        attributeNameLabel.setOutputMarkupId(true);

        RequiredTextField<String> attributeValue = new RequiredTextField<>(ID_ATTRIBUTE_VALUE, Model.of());
        attributeValue.setOutputMarkupId(true);
        attributeValue.add(AttributeAppender.append("aria-labelledby", attributeNameLabel.getMarkupId()));
        attributeNameLabel.add(AttributeAppender.append("for", attributeValue.getMarkupId()));
        form.add(attributeNameLabel);

        attributeValue.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                updateAttributeValues(ajaxRequestTarget);
            }
        });
        attributeValue.add(WebComponentUtil.getBlurOnEnterKeyDownBehavior());
        form.add(attributeValue);
    }

    private void updateAttributeValues(AjaxRequestTarget ajaxRequestTarget) {
        attrValuesModel.setObject(generateAttributeValuesString());
        ajaxRequestTarget.add(getHiddenField());
    }

    private String resolveAttributeLabel(IModel<List<ItemPathType>> path) {
        if (path == null) {
            return "";
        }
        List<ItemPathType> itemPaths = path.getObject();
        return itemPaths.stream()
                .map(this::translateAttribute)
                .collect(Collectors.joining(" or "));
    }

    private String translateAttribute(ItemPathType itemPath) {
        ItemDefinition<?> def = userModel.getObject().asPrismObject().getDefinition().findItemDefinition(itemPath.getItemPath());
        return WebComponentUtil.getItemDefinitionDisplayName(def);
    }

    private Component getVerifiedField() {
        return getForm().get(ID_ATTRIBUTE_VALUE);
    }

    private Component getHiddenField() {
        return getForm().get(ID_ATTRIBUTE_VALUES);
    }

    private String generateAttributeValuesString() {
        JSONArray attrValues = new JSONArray();
        attributesPathModel.getObject().forEach(entry -> {
            JSONObject json  = new JSONObject();
            json.put(AuthConstants.ATTR_VERIFICATION_J_PATH, entry.toString());
            json.put(AuthConstants.ATTR_VERIFICATION_J_VALUE, getVerifiedField().getDefaultModelObjectAsString());
            attrValues.put(json);
        });
        if (attrValues.length() == 0) {
            return null;
        }
        return attrValues.toString();
    }

    @Override
    protected IModel<String> getDefaultLoginPanelTitleModel() {
        return createStringResource("PageFocusIdentification.title");
    }

    @Override
    protected IModel<String> getDefaultLoginPanelDescriptionModel() {
        return createStringResource("PageFocusIdentification.description");
    }

}
