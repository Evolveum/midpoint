/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationStatusCapabilityType;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author katkav
 */
@Component
public class QNameAttributePanelFactory extends AbstractQNameWithChoicesPanelFactory implements Serializable{

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!DOMUtil.XSD_QNAME.equals(wrapper.getTypeName())) {
            return false;
        }

        if (ResourceObjectAssociationType.F_ASSOCIATION_ATTRIBUTE.equals(wrapper.getItemName())
                || ResourceObjectAssociationType.F_VALUE_ATTRIBUTE.equals(wrapper.getItemName())
                || ResourceObjectAssociationType.F_SHORTCUT_ASSOCIATION_ATTRIBUTE.equals(wrapper.getItemName())
                || ResourceObjectAssociationType.F_SHORTCUT_VALUE_ATTRIBUTE.equals(wrapper.getItemName())
                || ActivationStatusCapabilityType.F_ATTRIBUTE.equivalent(wrapper.getItemName())
                || PagedSearchCapabilityType.F_DEFAULT_SORT_FIELD.equivalent(wrapper.getItemName())) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean applyAutocomplete(PrismPropertyPanelContext<QName> panelCtx) {
        PrismObjectWrapper<ObjectType> objectWrapper = panelCtx.unwrapWrapperModel().findObjectWrapper();
        return objectWrapper != null && ResourceType.class.isAssignableFrom(objectWrapper.getTypeClass());
    }

    @Override
    protected List<DisplayableValue<QName>> createValues(PrismPropertyPanelContext<QName> panelCtx) {
        return getAllAttributes(panelCtx.getValueWrapperModel(), panelCtx.getPageBase());
    }

    private List<DisplayableValue<QName>> getAllAttributes(
            IModel<? extends PrismValueWrapper<QName>> propertyWrapperModel, PageBase pageBase) {

        List<DisplayableValue<QName>> allAttributes = new ArrayList<>();

        PrismValueWrapper<QName> propertyWrapper = propertyWrapperModel.getObject();

        getShadowAttributeDefinitions(propertyWrapper, pageBase)
                .forEach(attr -> allAttributes.add(new AssociationDisplayableValue(attr)));
        return allAttributes;
    }

    protected List<? extends ShadowAttributeDefinition> getShadowAttributeDefinitions(
            PrismValueWrapper<QName> propertyWrapper, PageBase pageBase) {
        ResourceSchema schema = ResourceDetailsModel.getResourceSchema(
                propertyWrapper.getParent().findObjectWrapper(), pageBase);

        if (schema == null) {
            return Collections.emptyList();
        }

        ResourceObjectTypeDefinitionType objectType = getResourceObjectType(propertyWrapper);
        return WebPrismUtil.searchAttributeDefinitions(schema, objectType);
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

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentValue =
                propertyWrapper.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        if (parentValue != null) {
            return parentValue.getRealValue();
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

        private AssociationDisplayableValue(ShadowAttributeDefinition attributeDefinition) {
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
