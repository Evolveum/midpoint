/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.component.input.AutoCompleteDisplayableValueConverter;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.search.DisplayableRenderer;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
@Component
public class AttributeMappingItemPathPanelFactory extends ItemPathPanelFactory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired private transient GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return super.match(wrapper)
                && (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_REF))
                || isVirtualPropertyOfMapping(wrapper));
    }

    private <IW extends ItemWrapper<?, ?>> boolean isVirtualPropertyOfMapping(IW wrapper) {
        return QNameUtil.match(wrapper.getItemName(), ResourceAttributeDefinitionType.F_REF)
                && wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_INBOUND));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {

        PrismObjectWrapper<ResourceType> objectWrapper = panelCtx.unwrapWrapperModel().findObjectWrapper();
        if (objectWrapper != null) {

            ResourceSchema schema = ResourceDetailsModel.getResourceSchema(objectWrapper, panelCtx.getPageBase());
            if (schema != null) {

                IModel<List<DisplayableValue<ItemPathType>>> values = getChoices(panelCtx.getValueWrapperModel(), schema);

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

                    Iterator<ItemPathType> choices = values.getObject().stream()
                            .map(disValue -> disValue.getValue())
                            .collect(Collectors.toList()).iterator();

                    AutoCompleteTextPanel panel = new AutoCompleteTextPanel<>(
                            panelCtx.getComponentId(), panelCtx.getRealValueModel(), panelCtx.getTypeClass(), renderer) {
                        @Override
                        public Iterator<ItemPathType> getIterator(String input) {
                            if (StringUtils.isBlank(input)) {
                                return choices;
                            }
                            return values.getObject().stream()
                                    .filter(v -> v.getLabel().contains(input))
                                    .map(v -> v.getValue())
                                    .collect(Collectors.toList())
                                    .iterator();
                        }

                        @Override
                        protected <C> IConverter<C> getAutoCompleteConverter(Class<C> type, IConverter<C> originConverter) {
                            return (IConverter<C>) new AutoCompleteDisplayableValueConverter<ItemPathType>(values);
                        }
                    };
                    panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                    return panel;
                }
            }
        }

        return super.getPanel(panelCtx);

    }

    private IModel<List<DisplayableValue<ItemPathType>>> getChoices(IModel<? extends PrismValueWrapper<ItemPathType>> propertyWrapper, ResourceSchema schema) {
        return new LoadableDetachableModel<>() {
            @Override
            protected List<DisplayableValue<ItemPathType>> load() {
                return getAllAttributes(propertyWrapper, schema);
            }
        };
    }

    private List<DisplayableValue<ItemPathType>> getAllAttributes(
            IModel<? extends PrismValueWrapper<ItemPathType>> propertyWrapperModel, ResourceSchema schema) {

        List<DisplayableValue<ItemPathType>> allAttributes = new ArrayList<>();

        PrismValueWrapper<ItemPathType> propertyWrapper = propertyWrapperModel.getObject();
        ResourceObjectTypeDefinitionType objectType = getResourceObjectType(propertyWrapper);
        if (objectType != null) {
            @Nullable ResourceObjectTypeDefinition objectTypeDef = null;
            if (objectType.getKind() != null && objectType.getIntent() != null) {

                @NotNull ResourceObjectTypeIdentification identifier =
                        ResourceObjectTypeIdentification.of(objectType.getKind(), objectType.getIntent());
                objectTypeDef = schema.getObjectTypeDefinition(identifier);

                if (objectTypeDef != null) {
                    objectTypeDef.getAttributeDefinitions()
                            .forEach(attr -> allAttributes.add(new AttributeDisplayableValue(attr)));
                }
            }
            if (objectTypeDef == null && objectType.getDelineation() != null && objectType.getDelineation().getObjectClass() != null) {

                @NotNull Collection<ResourceObjectClassDefinition> defs = schema.getObjectClassDefinitions();
                Optional<ResourceObjectClassDefinition> objectClassDef = defs.stream()
                        .filter(d -> QNameUtil.match(d.getTypeName(), objectType.getDelineation().getObjectClass()))
                        .findFirst();

                if (!objectClassDef.isEmpty()) {
                    objectClassDef.get().getAttributeDefinitions().forEach(attr -> allAttributes.add(new AttributeDisplayableValue(attr)));
                    defs.stream()
                            .filter(d -> {
                                for (QName auxClass : objectType.getDelineation().getAuxiliaryObjectClass()) {
                                    if (QNameUtil.match(d.getTypeName(), auxClass)) {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .forEach(d -> d.getAttributeDefinitions()
                                    .forEach(attr -> allAttributes.add(new AttributeDisplayableValue(attr))));
                }
            }
        }
        return allAttributes;
    }

    private ResourceObjectTypeDefinitionType getResourceObjectType(PrismValueWrapper<ItemPathType> propertyWrapper) {
        PrismContainerValueWrapper containerValue = null;

        if (propertyWrapper.getParent() != null
                && propertyWrapper.getParent().getParent() != null
                && propertyWrapper.getParent().getParent().getParent() != null) {

            if (!isVirtualPropertyOfMapping(propertyWrapper.getParent())) {
                containerValue = propertyWrapper.getParent().getParent().getParent().getParent();
            } else if (propertyWrapper.getParent().getParent().getParent().getParent() != null
                    && propertyWrapper.getParent().getParent().getParent().getParent().getParent() != null) {
                containerValue = propertyWrapper.getParent().getParent().getParent().getParent().getParent().getParent();
            }
        }
        if (containerValue != null && containerValue.getRealValue() instanceof ResourceObjectTypeDefinitionType) {
            return (ResourceObjectTypeDefinitionType) containerValue.getRealValue();
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
