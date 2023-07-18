/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

public abstract class PageAbstractAttributeVerification extends PageAuthenticationBase {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ATTRIBUTE_VALUES = "attributeValues";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_CSRF_FIELD = "csrfField";


    private LoadableDetachableModel<List<VerificationAttributeDto>> attributePathModel;
    private LoadableDetachableModel<UserType> userModel;
    IModel<String> attrValuesModel = Model.of();

    public PageAbstractAttributeVerification() {
    }

    protected void initModels() {
        attributePathModel = new LoadableDetachableModel<List<VerificationAttributeDto>>() {
            private static final long serialVersionUID = 1L;
            @Override
            protected List<VerificationAttributeDto> load() {
                return loadAttrbuteVerificationDtoList();
            }
        };
    }

    protected abstract List<VerificationAttributeDto> loadAttrbuteVerificationDtoList();

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

    protected abstract String getUrlProcessingLogin();

    private void initAttributesLayout(MidpointForm<?> form) {
        ListView<VerificationAttributeDto> attributesPanel = new ListView<>(ID_ATTRIBUTES, attributePathModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<VerificationAttributeDto> item) {
                Label attributeNameLabel = new Label(ID_ATTRIBUTE_NAME, resolveAttributeLabel(item.getModelObject()));
                item.add(attributeNameLabel);

                RequiredTextField<String> attributeValue = new RequiredTextField<>(ID_ATTRIBUTE_VALUE, new PropertyModel<>(item.getModel(), VerificationAttributeDto.F_VALUE));
                attributeValue.setOutputMarkupId(true);
                attributeValue.add(new AjaxFormComponentUpdatingBehavior("blur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        attrValuesModel.setObject(generateAttributeValuesString());
                        target.add(getVerifiedField());
                    }
                });
                item.add(attributeValue);
            }
        };
        attributesPanel.setOutputMarkupId(true);
        form.add(attributesPanel);
    }

    private String resolveAttributeLabel(VerificationAttributeDto attribute) {
        if (attribute == null) {
            return "";
        }
        ItemPathType path = attribute.getItemPath();
        if (path == null) {
            return "";
        }
        ItemDefinition<?> def = new UserType().asPrismObject().getDefinition().findItemDefinition(path.getItemPath());
        return WebComponentUtil.getItemDefinitionDisplayName(def);
    }

    private void initButtons(MidpointForm form) {
        form.add(createBackButton(ID_BACK_BUTTON));
    }

    private Component getVerifiedField() {
        return  get(ID_MAIN_FORM).get(ID_ATTRIBUTE_VALUES);
    }

    private String generateAttributeValuesString() {
        JSONArray attrValues = new JSONArray();
        attributePathModel.getObject().forEach(entry -> {
            JSONObject json  = new JSONObject();
            json.put(AuthConstants.ATTR_VERIFICATION_J_PATH, entry.getItemPath());
            json.put(AuthConstants.ATTR_VERIFICATION_J_VALUE, entry.getValue());
            attrValues.put(json);
        });
        if (attrValues.length() == 0) {
            return null;
        }
        return attrValues.toString();
    }


}
