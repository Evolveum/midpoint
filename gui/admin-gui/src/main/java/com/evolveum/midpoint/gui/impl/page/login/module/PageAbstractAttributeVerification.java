/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;


import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.model.PropertyModel;

public abstract class PageAbstractAttributeVerification<MA extends ModuleAuthentication> extends PageAbstractAuthenticationModule<MA> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAbstractAttributeVerification.class);
    private static final String DOT_CLASS = PageAbstractAttributeVerification.class.getName() + ".";
    protected static final String OPERATION_CREATE_ITEM_WRAPPER = DOT_CLASS + "createItemWrapper";
    private static final String ID_ATTRIBUTE_VALUES = "attributeValues";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";
    private static final String ID_ATTRIBUTE_PANEL = "attributePanel";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";

    private LoadableModel<List<VerificationAttributeDto>> attributePathModel;
    private final IModel<String> attrValuesModel = Model.of();

    public PageAbstractAttributeVerification() {
        super();
        initModels();
    }

    protected void initModels() {
        attributePathModel = new LoadableModel<>(false) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<VerificationAttributeDto> load() {
                return loadAttrbuteVerificationDtoList();
            }
        };
    }

    protected abstract List<VerificationAttributeDto> loadAttrbuteVerificationDtoList();

    @Override
    protected void initModuleLayout(MidpointForm form) {
        HiddenField<String> verified = new HiddenField<>(ID_ATTRIBUTE_VALUES, attrValuesModel);
        verified.setOutputMarkupId(true);
        form.add(verified);

        initAttributesLayout(form);
    }

    private void initAttributesLayout(MidpointForm<?> form) {
        ListView<VerificationAttributeDto> attributesPanel = new ListView<>(ID_ATTRIBUTES, attributePathModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<VerificationAttributeDto> item) {
                createAttributePanel(item);
            }
        };
        attributesPanel.setOutputMarkupId(true);
        form.add(attributesPanel);
    }

    private void createAttributePanel(ListItem<VerificationAttributeDto> item) {
        Label attributeNameLabel = new Label(ID_ATTRIBUTE_NAME, resolveAttributeLabel(item.getModelObject()));
        item.add(attributeNameLabel);

        var itemWrapper = item.getModelObject().getItemWrapper();
        var valuePanel = new PrismPropertyValuePanel<PrismPropertyValueWrapper<?>>(ID_ATTRIBUTE_VALUE,
                new PropertyModel<>(itemWrapper, "value"),
                new ItemPanelSettingsBuilder().build()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AjaxEventBehavior createEventBehavior() {
                return new AjaxFormComponentUpdatingBehavior("change") {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        attrValuesModel.setObject(generateAttributeValuesString());
                        target.add(getVerifiedField());
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, RuntimeException e) {
                        target.add(getFeedback());
                    }
                };
            }
        };
        item.add(valuePanel);
    }

    private String resolveAttributeLabel(VerificationAttributeDto attribute) {
        if (attribute == null) {
            return "";
        }
        var path = attribute.getItemPath();
        if (path == null) {
            return "";
        }
        ItemDefinition<?> def = new UserType().asPrismObject().getDefinition().findItemDefinition(path);
        var label = WebComponentUtil.getItemDefinitionDisplayNameOrName(def);
        return StringUtils.isEmpty(label) ? path.toString() : label;
    }

    protected ItemWrapper<?, ?> createItemWrapper(ItemPath itemPath) {
        try {
            var itemDefinition = resolveAttributeDefinition(itemPath);
            var wrapperContext = createWrapperContext();
            var itemWrapper = (PrismPropertyWrapper<?>) createItemWrapper(itemDefinition.instantiate(), ItemStatus.ADDED,
                    wrapperContext);
            return itemWrapper;
        } catch (SchemaException e) {
            LOGGER.debug("Unable to create item wrapper for path {}", itemPath);
        }
        return null;
    }

    private ItemDefinition<?> resolveAttributeDefinition(ItemPath itemPath) {
        return new UserType().asPrismObject().getDefinition().findItemDefinition(itemPath);
    }

    private WrapperContext createWrapperContext() {
        Task task = createAnonymousTask(OPERATION_CREATE_ITEM_WRAPPER);
        WrapperContext ctx = new WrapperContext(task, task.getResult());
//        ctx.setCreateIfEmpty(true);
        return ctx;
    }

    private String generateAttributeValuesString() {
        JSONArray attrValues = new JSONArray();
        attributePathModel.getObject().forEach(entry -> {
            PrismPropertyValueWrapper value = (PrismPropertyValueWrapper) entry.getValue();
            if (value == null || value.getRealValue() == null) {
                return;
            }
            JSONObject json = new JSONObject();
            json.put(AuthConstants.ATTR_VERIFICATION_J_PATH, entry.getItemPath());
            json.put(AuthConstants.ATTR_VERIFICATION_J_VALUE, value.getRealValue());
            attrValues.put(json);
        });
        if (attrValues.length() == 0) {
            return null;
        }
        return attrValues.toString();
    }

    private Component getVerifiedField() {
        return getForm().get(ID_ATTRIBUTE_VALUES);
    }
}
