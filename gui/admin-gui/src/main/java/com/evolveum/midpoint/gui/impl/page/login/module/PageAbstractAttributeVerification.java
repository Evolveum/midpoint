/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.login.dto.VerificationAttributeDto;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public abstract class PageAbstractAttributeVerification<MA extends ModuleAuthentication> extends PageAbstractAuthenticationModule<MA> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageAbstractAttributeVerification.class);
    private static final String DOT_CLASS = PageAbstractAttributeVerification.class.getName() + ".";
    protected static final String OPERATION_CREATE_ITEM_WRAPPER = DOT_CLASS + "createItemWrapper";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE_NAME = "attributeName";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";
    private static final String ID_PARAMETER_VALUE = "parameterValue";

    private LoadableModel<List<VerificationAttributeDto>> attributePathModel;

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
        PrismPropertyWrapper<?> itemWrapper = item.getModelObject().getItemWrapper();

        PrismPropertyHeaderPanel<?> attributeNameLabel = new PrismPropertyHeaderPanel(ID_ATTRIBUTE_NAME, Model.of(itemWrapper)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isRequired() {
                if (areAllItemsMandatory(itemWrapper)) {
                    return true;
                }
                return super.isRequired();
            }
        };
        attributeNameLabel.setOutputMarkupId(true);
        item.add(attributeNameLabel);

        if (QNameUtil.match(DOMUtil.XSD_STRING, itemWrapper.getTypeName()) ||
                QNameUtil.match(PolyStringType.COMPLEX_TYPE, itemWrapper.getTypeName())) {
            createTextPanelComponent(itemWrapper, item, attributeNameLabel.createLabelModel().getObject());
        } else {
            createGenericPanelComponent(itemWrapper, item, attributeNameLabel.createLabelModel().getObject());
        }
    }

    private void createTextPanelComponent(PrismPropertyWrapper<?> itemWrapper, ListItem<VerificationAttributeDto> item, String headerLabel) {
        PropertyModel<String> valueModel = new PropertyModel<>(itemWrapper, "value.realValue");
        TextPanel<String> valuePanel = new TextPanel<>(ID_ATTRIBUTE_VALUE, valueModel);
        valuePanel.getBaseFormComponent().add(AttributeAppender.append("aria-label", headerLabel));
        addNameAttribute(valuePanel.getBaseFormComponent(), item);
        item.add(valuePanel);

        HiddenField<?> parameterValue = new HiddenField<>(ID_PARAMETER_VALUE, valueModel);
        parameterValue.setOutputMarkupId(true);
        item.add(parameterValue);
    }

    private void createGenericPanelComponent(PrismPropertyWrapper<?> itemWrapper, ListItem<VerificationAttributeDto> item, String headerLabel) {
        IModel<String> hiddenFieldModel = Model.of();

        PropertyModel<PrismPropertyValueWrapper> valueModel = new PropertyModel<>(itemWrapper, "value");
        var valuePanel = new PrismPropertyValuePanel(ID_ATTRIBUTE_VALUE,
                valueModel, createItemPanelSettings()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AjaxEventBehavior createEventBehavior() {
                return new AjaxFormComponentUpdatingBehavior("change") {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        try {
                            hiddenFieldModel.setObject(itemWrapper.getValue().getRealValue().toString());
                        } catch (Exception e) {
                            hiddenFieldModel.setObject(null);
                        }
                        target.add(item.get("parameterValue"));
                    }
                };
            }

            @Override
            protected Map<String, String> getAttributeValuesMap() {
                return Map.of("aria-label", headerLabel);
            }

            @Override
            protected void remove(PrismPropertyValueWrapper valueToRemove, AjaxRequestTarget target)
                    throws SchemaException {
                try {
                    OperationResult result = new OperationResult("removeAttributeValue");
                    PrismObject<UserType> administrator = getAdministratorPrivileged(result);
                    runAsChecked(
                            (lResult) -> {
                                if (valueToRemove != null) {
                                    itemWrapper.remove(valueToRemove, PageAbstractAttributeVerification.this);
                                    target.add(getValuePanel());
                                }
                                return null;
                            }, administrator, result);
                } catch (CommonException e) {
                    LOGGER.error("Unable to remove attribute value.");
                }
            }
        };
        item.add(valuePanel);

        HiddenField<?> parameterValue = new HiddenField<>(ID_PARAMETER_VALUE, hiddenFieldModel);
        addNameAttribute(parameterValue, item);
        parameterValue.setOutputMarkupId(true);
        item.add(parameterValue);
    }

    private void addNameAttribute(FormComponent component, ListItem<VerificationAttributeDto> item) {
        component.add(AttributeAppender.replace("name", AuthConstants.ATTR_VERIFICATION_PARAMETER_START
                + item.getModelObject().getItemPath()));
    }

    protected PrismPropertyWrapper<?> createItemWrapper(ItemPath itemPath) {
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
        return new WrapperContext(task, task.getResult());
    }

    private ItemPanelSettings createItemPanelSettings() {
        return new ItemPanelSettingsBuilder()
                .mandatoryHandler(this::areAllItemsMandatory)
                .build();
    }

    protected boolean areAllItemsMandatory(ItemWrapper<?,?> itemWrapper) {
        return false;
    }

}
