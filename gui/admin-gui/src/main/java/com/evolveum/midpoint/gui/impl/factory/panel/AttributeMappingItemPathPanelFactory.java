/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.input.AutoCompleteDisplayableValueConverter;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.convert.IConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
@Component
public class AttributeMappingItemPathPanelFactory extends ItemPathPanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeMappingItemPathPanelFactory.class);

    private static final long serialVersionUID = 1L;

    @Autowired private transient GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return ItemPathType.COMPLEX_TYPE.equals(wrapper.getTypeName())
                && (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_REF))
                || isVirtualPropertyOfMapping(wrapper));
    }

    private <IW extends ItemWrapper<?, ?>> boolean isVirtualPropertyOfMapping(IW wrapper) {
        return QNameUtil.match(wrapper.getItemName(), ResourceAttributeDefinitionType.F_REF)
                && (wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_INBOUND))
                || wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_OUTBOUND)));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {

        PrismObjectWrapper<ResourceType> objectWrapper = panelCtx.unwrapWrapperModel().findObjectWrapper();
        if (objectWrapper != null) {

            IModel<List<DisplayableValue<ItemPathType>>> values = getChoices(panelCtx.getValueWrapperModel(), panelCtx.getPageBase());

            if (CollectionUtils.isNotEmpty(values.getObject())) {

                IAutoCompleteRenderer<ItemPathType> renderer = new AbstractAutoCompleteRenderer<>() {
                    @Override
                    protected void renderChoice(ItemPathType itemPathType, Response response, String s) {
                        response.write(getTextValue(itemPathType));
                    }

                    @Override
                    protected String getTextValue(ItemPathType itemPathType) {
                        return values.getObject().stream()
                                .filter(attr -> attr.getValue().equivalent(itemPathType))
                                .findFirst()
                                .get().getLabel();
                    }
                };

                AutoCompleteTextPanel panel = new AutoCompleteTextPanel<>(
                        panelCtx.getComponentId(), panelCtx.getRealValueModel(), panelCtx.getTypeClass(), renderer) {
                    @Override
                    public Iterator<ItemPathType> getIterator(String input) {
                        List<DisplayableValue<ItemPathType>> choices = new ArrayList<>(values.getObject());
                        if (StringUtils.isNotEmpty(input)) {
                            choices = choices.stream()
                                    .filter(v -> v.getLabel().toLowerCase().contains(input.toLowerCase()))
                                    .collect(Collectors.toList());
                        }
                        if (skipUsedAttributes(panelCtx)) {
                            choices = choices.stream()
                                    .filter(v -> notEquivalentWithValues(panelCtx, v))
                                    .collect(Collectors.toList());
                        }
                        return choices.stream()
                                .map(v -> v.getValue())
                                .collect(Collectors.toList())
                                .iterator();
                    }

                    @Override
                    protected <C> IConverter<C> getAutoCompleteConverter(Class<C> type, IConverter<C> originConverter) {
                        return (IConverter<C>) new AutoCompleteDisplayableValueConverter<>(values) {
                            @Override
                            protected boolean matchValues(ItemPathType key, ItemPathType value) {
                                return value.equivalent(key);
                            }
                        };
                    }
                };
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                return panel;
            }
        }

        return super.getPanel(panelCtx);

    }

    private boolean notEquivalentWithValues(PrismPropertyPanelContext<ItemPathType> panelCtx, DisplayableValue<ItemPathType> v) {

        ItemPathType realValue = panelCtx.getRealValueModel().getObject();
        if (v.getValue().equivalent(realValue)) {
            return true;
        }

        PrismContainerWrapper<ResourceAttributeDefinitionType> mapping =
                getAttributeMapping(panelCtx.getValueWrapperModel().getObject());

        if (mapping != null) {
            if (isVirtualPropertyOfMapping(panelCtx.unwrapWrapperModel())) {
                PrismContainerWrapper parentContainer = panelCtx.unwrapWrapperModel().getParent().getParent();
                @NotNull ItemName parentPath = parentContainer.getItemName();
                for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> value : mapping.getValues()) {
                    try {
                        PrismContainerWrapper<Containerable> valuesContainer = value.findContainer(parentPath);
                        for (PrismContainerValueWrapper<Containerable> valueContainer : valuesContainer.getValues()) {
                            PrismPropertyWrapper<ItemPathType> attributeRef = valueContainer.findProperty(ResourceAttributeDefinitionType.F_REF);
                            if (attributeRef != null && v.getValue().equivalent(attributeRef.getValue().getRealValue())) {
                                return false;
                            }
                        }
                    } catch (SchemaException e) {
                        // ignore it
                    }
                }
            }
        }
        return true;
    }

    private boolean skipUsedAttributes(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        if (isVirtualPropertyOfMapping(panelCtx.unwrapWrapperModel())) {
            return panelCtx.unwrapWrapperModel().getParent().getParent().isSingleValue();
        }
        return true;
    }

    private IModel<List<DisplayableValue<ItemPathType>>> getChoices(
            IModel<? extends PrismValueWrapper<ItemPathType>> propertyWrapper, PageBase pageBase) {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<DisplayableValue<ItemPathType>> load() {
                List<DisplayableValue<ItemPathType>> choices = getAllAttributes(propertyWrapper, pageBase);
                if (!choices.isEmpty()) {
                    PrismValueWrapper<ItemPathType> wrapper = propertyWrapper.getObject();
                    ItemWrapper parent = wrapper.getParent().getParent().getParent();
                    if (parent.isSingleValue()) {
                        ResourceObjectTypeDefinitionType objectType = getResourceObjectType(propertyWrapper.getObject());
                        if (objectType != null) {
                            List<ItemPathType> existingPaths = new ArrayList<>();
                            objectType.getAttribute().forEach(attributeMapping -> {

                                if (attributeMapping.getRef() == null
                                        || attributeMapping.getRef().equivalent(wrapper.getRealValue())) {
                                    return;
                                }

                                PrismContainer container =
                                        attributeMapping.asPrismContainerValue().findContainer(parent.getItemName());
                                container = container != null ? container.clone() : null;
                                WebPrismUtil.cleanupEmptyContainers(container);

                                if (container != null && container.isEmpty()) {
                                    return;
                                }

                                existingPaths.add(attributeMapping.getRef());
                            });
                            choices.removeIf(value -> {
                                for (ItemPathType existingPath : existingPaths) {
                                    if (existingPath.equivalent(value.getValue())) {
                                        return true;
                                    }
                                }
                                return false;
                            });
                        }
                    }
                }
                return choices;
            }
        };
    }

    private List<DisplayableValue<ItemPathType>> getAllAttributes(
            IModel<? extends PrismValueWrapper<ItemPathType>> propertyWrapperModel, PageBase pageBase) {

        List<DisplayableValue<ItemPathType>> allAttributes = new ArrayList<>();

        PrismValueWrapper<ItemPathType> propertyWrapper = propertyWrapperModel.getObject();

        ResourceSchema schema = null;
        try {
            schema = ResourceSchemaFactory.getCompleteSchema(
                    (ResourceType) propertyWrapper.getParent().findObjectWrapper().getObjectOld().asObjectable());
        } catch (Exception e) {
            LOGGER.debug("Couldn't get complete resource schema", e);
        }

        if (schema == null) {
            schema = ResourceDetailsModel.getResourceSchema(
                    propertyWrapper.getParent().findObjectWrapper(), pageBase);
        }

        if (schema == null) {
            return allAttributes;
        }

        ResourceObjectTypeDefinitionType objectType = getResourceObjectType(propertyWrapper);
        WebPrismUtil.searchAttributeDefinitions(schema, objectType)
                .forEach(attr -> allAttributes.add(new AttributeDisplayableValue(attr)));
        return allAttributes;
    }

    private ResourceObjectTypeDefinitionType getResourceObjectType(PrismValueWrapper<ItemPathType> propertyWrapper) {
        PrismContainerWrapper<ResourceAttributeDefinitionType> mapping = getAttributeMapping(propertyWrapper);
        if (mapping != null
                && mapping.getParent() != null
                && mapping.getParent().getRealValue() instanceof ResourceObjectTypeDefinitionType) {
            return (ResourceObjectTypeDefinitionType) mapping.getParent().getRealValue();
        }
        return null;
    }

    private PrismContainerWrapper<ResourceAttributeDefinitionType> getAttributeMapping(PrismValueWrapper<ItemPathType> propertyWrapper) {
        PrismContainerValueWrapper containerValue = null;

        if (propertyWrapper.getParent() != null) {

            if (!isVirtualPropertyOfMapping(propertyWrapper.getParent())) {
                containerValue = propertyWrapper.getParent().getParent();
            } else if (propertyWrapper.getParent().getParent() != null
                    && propertyWrapper.getParent().getParent().getParent() != null) {
                containerValue = propertyWrapper.getParent().getParent().getParent().getParent();
            }
        }
        if (containerValue != null && containerValue.getRealValue() instanceof ResourceAttributeDefinitionType) {
            return (PrismContainerWrapper<ResourceAttributeDefinitionType>) containerValue.getParent();
        }
        return null;
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE - 100;
    }

    private class AttributeDisplayableValue implements DisplayableValue<ItemPathType> {

        private final String displayName;
        private final String help;
        private final ItemPathType value;

        private AttributeDisplayableValue(ResourceAttributeDefinition attributeDefinition) {
            this.displayName = attributeDefinition.getDisplayName() == null ?
                    attributeDefinition.getItemName().getLocalPart() : attributeDefinition.getDisplayName();
            this.help = attributeDefinition.getHelp();
            this.value = new ItemPathType(ItemPath.create(attributeDefinition.getItemName()));
        }

        @Override
        public ItemPathType getValue() {
            return value;
        }

        @Override
        public String getLabel() {
            return displayName;
        }

        @Override
        public String getDescription() {
            return help;
        }
    }
}
