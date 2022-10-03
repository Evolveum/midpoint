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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.input.AutoCompleteDisplayableValueConverter;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.convert.IConverter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author katkav
 */
@Component
public class AssociationAttributePanelFactory extends DropDownChoicePanelFactory implements Serializable{

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return ResourceObjectAssociationType.F_ASSOCIATION_ATTRIBUTE.equals(wrapper.getItemName())
                || ResourceObjectAssociationType.F_VALUE_ATTRIBUTE.equals(wrapper.getItemName())
                || ResourceObjectAssociationType.F_SHORTCUT_ASSOCIATION_ATTRIBUTE.equals(wrapper.getItemName())
                || ResourceObjectAssociationType.F_SHORTCUT_VALUE_ATTRIBUTE.equals(wrapper.getItemName())
                || DOMUtil.XSD_QNAME.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        PrismObjectWrapper<ResourceType> objectWrapper = panelCtx.unwrapWrapperModel().findObjectWrapper();
        if (objectWrapper != null) {

            IModel<List<DisplayableValue<QName>>> values = new LoadableDetachableModel<>() {
                @Override
                protected List<DisplayableValue<QName>> load() {
                    return getAllAttributes(panelCtx.getValueWrapperModel(), panelCtx.getPageBase());
                }
            };


                IAutoCompleteRenderer<QName> renderer = new AbstractAutoCompleteRenderer<>() {
                    @Override
                    protected void renderChoice(QName itemName, Response response, String s) {
                        response.write(getTextValue(itemName));
                    }

                    @Override
                    protected String getTextValue(QName itemName) {
                        return values.getObject().stream()
                                .filter(attr -> QNameUtil.match(attr.getValue(), itemName))
                                .findFirst()
                                .get().getLabel();
                    }
                };

                AutoCompleteTextPanel panel = new AutoCompleteTextPanel<>(
                        panelCtx.getComponentId(), panelCtx.getRealValueModel(), panelCtx.getTypeClass(), renderer) {
                    @Override
                    public Iterator<QName> getIterator(String input) {
                        List<DisplayableValue<QName>> choices = new ArrayList<>(values.getObject());
                        if (StringUtils.isNotEmpty(input)) {
                            String partOfInput;
                            if (input.contains(":")) {
                                partOfInput = input.substring(input.indexOf(":") + 1);
                            } else {
                                partOfInput = input;
                            }
                            choices = choices.stream()
                                    .filter(v -> v.getLabel().contains(partOfInput))
                                    .collect(Collectors.toList());
                        }
                        return choices.stream()
                                .map(v -> v.getValue())
                                .collect(Collectors.toList())
                                .iterator();
                    }

                    @Override
                    protected <C> IConverter<C> getAutoCompleteConverter(Class<C> type, IConverter<C> originConverter) {
                        return (IConverter<C>) new AutoCompleteDisplayableValueConverter<QName>(values, false) {
                            @Override
                            protected QName valueToObject(String value) {
                                if (value.contains(":")) {
                                    int index = value.indexOf(":");
                                    return new QName(null, value.substring(index + 1), value.substring(0, index));
                                }
                                return new QName(value);
                            }

                            @Override
                            protected String keyToString(QName key) {
                                return key.getPrefix() != null ? key.getPrefix() + ":" + key.getLocalPart() : key.getLocalPart();
                            }
                        };
                    }
                };
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                return panel;
            }
        return new TextPanel<>(panelCtx.getComponentId(),
                panelCtx.getRealValueModel(), panelCtx.getTypeClass(), false);
    }

    private List<DisplayableValue<QName>> getAllAttributes(
            IModel<? extends PrismValueWrapper<QName>> propertyWrapperModel, PageBase pageBase) {

        List<DisplayableValue<QName>> allAttributes = new ArrayList<>();

        PrismValueWrapper<QName> propertyWrapper = propertyWrapperModel.getObject();

        ResourceSchema schema = ResourceDetailsModel.getResourceSchema(
                propertyWrapper.getParent().findObjectWrapper(), pageBase);

        if (schema == null) {
            return allAttributes;
        }

        ResourceObjectTypeDefinitionType objectType = getResourceObjectType(propertyWrapper);
        WebPrismUtil.searchAttributeDefinitions(schema, objectType)
                .forEach(attr -> allAttributes.add(new AssociationDisplayableValue(attr)));
        return allAttributes;
    }

    private ResourceObjectTypeDefinitionType getResourceObjectType(PrismValueWrapper<QName> propertyWrapper) {
        if (propertyWrapper.getParent() != null
                && propertyWrapper.getParent().getParent() != null
                && propertyWrapper.getParent().getParent().getRealValue() instanceof ResourceObjectAssociationType) {
            ResourceObjectAssociationType association =
                    (ResourceObjectAssociationType) propertyWrapper.getParent().getParent().getRealValue();
            ResourceObjectTypeDefinitionType resourceObjectType = null;
            if ((ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT.equals(association.getDirection())
                    && (ResourceObjectAssociationType.F_ASSOCIATION_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())
                    || ResourceObjectAssociationType.F_SHORTCUT_ASSOCIATION_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())))
                    || (ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT.equals(association.getDirection())
                    && (ResourceObjectAssociationType.F_VALUE_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())
                    || ResourceObjectAssociationType.F_SHORTCUT_VALUE_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())))) {
                if (propertyWrapper.getParent().getParent().getParent() != null
                        && propertyWrapper.getParent().getParent().getParent().getParent() != null )
                resourceObjectType =
                        (ResourceObjectTypeDefinitionType) propertyWrapper.getParent().getParent().getParent().getParent().getRealValue();
            } else if ((ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT.equals(association.getDirection())
                    && (ResourceObjectAssociationType.F_ASSOCIATION_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())
                    || ResourceObjectAssociationType.F_SHORTCUT_ASSOCIATION_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())))
                    || (ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT.equals(association.getDirection())
                    && (ResourceObjectAssociationType.F_VALUE_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())
                    || ResourceObjectAssociationType.F_SHORTCUT_VALUE_ATTRIBUTE.equals(propertyWrapper.getParent().getItemName())))) {
                ShadowKindType kind = association.getKind();
                String intent = association.getIntent().isEmpty() ? null : association.getIntent().iterator().next();
                if (kind == null) {
                    return null;
                }
                if (propertyWrapper.getParent().getParent().getParent() != null
                        && propertyWrapper.getParent().getParent().getParent().getParent() != null
                        && propertyWrapper.getParent().getParent().getParent().getParent().getParent() != null) {
                    PrismContainerWrapper<ResourceObjectTypeDefinitionType> objectTypesWrapper =
                            (PrismContainerWrapper<ResourceObjectTypeDefinitionType>) propertyWrapper.getParent().getParent().getParent().getParent().getParent();
                    ResourceObjectTypeDefinitionType objectTypeDefaultForKind = null;
                    for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType : objectTypesWrapper.getValues()) {
                        if (objectType.getRealValue() != null) {
                            ResourceObjectTypeDefinitionType objectTypeRealValue = objectType.getRealValue();
                            if (kind == objectTypeRealValue.getKind()) {
                                if (Boolean.TRUE.equals(objectTypeRealValue.isDefaultForKind())
                                        || Boolean.TRUE.equals(objectTypeRealValue.isDefault())) {
                                    objectTypeDefaultForKind = objectTypeRealValue;
                                }
                                if (StringUtils.isNotEmpty(intent) && intent.equals(objectTypeRealValue.getIntent())) {
                                    resourceObjectType = objectTypeRealValue;
                                }
                            }
                        }
                    }
                    if (resourceObjectType == null) {
                        return objectTypeDefaultForKind;
                    }
                    return resourceObjectType;
                }
            }
            return resourceObjectType;
        }
        return null;
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    private class AssociationDisplayableValue implements DisplayableValue<QName>, Serializable{

        private final String displayName;
        private final String help;
        private final QName value;

        private AssociationDisplayableValue(ResourceAttributeDefinition attributeDefinition) {
            this.displayName = attributeDefinition.getDisplayName() == null ?
                    attributeDefinition.getItemName().getLocalPart() : attributeDefinition.getDisplayName();
            this.help = attributeDefinition.getHelp();
            this.value = attributeDefinition.getItemName();
        }

        @Override
        public QName getValue() {
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
