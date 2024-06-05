/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.itempath;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author lskublik
 */
@Component
public class AssociationAttributeMappingItemPathPanelFactory extends AttributeMappingItemPathPanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationAttributeMappingItemPathPanelFactory.class);

    private static final long serialVersionUID = 1L;

    @Autowired private transient GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return ItemPathType.COMPLEX_TYPE.equals(wrapper.getTypeName())
                && (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                getItemNameForContainerOfAttributes(),
                ResourceAttributeDefinitionType.F_REF))
                || isVirtualPropertyOfMapping(wrapper));
    }

    protected ItemName getItemNameForContainerOfAttributes() {
        return ShadowAssociationDefinitionType.F_ATTRIBUTE;
    }

    private <IW extends ItemWrapper<?, ?>> boolean isVirtualPropertyOfMapping(IW wrapper) {
        return QNameUtil.match(wrapper.getItemName(), ResourceAttributeDefinitionType.F_REF)
                && (wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                getItemNameForContainerOfAttributes(),
                ResourceAttributeDefinitionType.F_INBOUND))
                || wrapper.getParent().getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                getItemNameForContainerOfAttributes(),
                ResourceAttributeDefinitionType.F_OUTBOUND)));
    }

    @Override
    protected List<DisplayableValue<ItemPathType>> getAttributes(ResourceSchema schema, PrismValueWrapper<ItemPathType> propertyWrapper) {
        ShadowReferenceAttributeDefinition refAttribute = getShadowReferenceAttribute(schema, propertyWrapper);
        if (refAttribute == null) {
            return Collections.emptyList();
        }

        List<DisplayableValue<ItemPathType>> attributes = new ArrayList<>();
        refAttribute.getRepresentativeTargetObjectDefinition().getAttributeDefinitions()
                .forEach(attribute -> attributes.add(createDisplayValue(attribute)));
        attributes.add(createDisplayValue(refAttribute));
        return attributes;
    }

    protected ShadowReferenceAttributeDefinition getShadowReferenceAttribute(ResourceSchema schema, PrismValueWrapper<ItemPathType> propertyWrapper) {
        if (schema == null) {
            return null;
        }

        ResourceObjectTypeIdentificationType objectTypeOfSubject = getObjectTypeOfSubject(propertyWrapper);
        if (objectTypeOfSubject == null) {
            return null;
        }

        @Nullable ResourceObjectTypeDefinition objectTypeDef = schema.getObjectTypeDefinition(objectTypeOfSubject.getKind(), objectTypeOfSubject.getIntent());
        if (objectTypeDef == null) {
            return null;
        }

        ItemName ref = getRef(propertyWrapper);
        if (ref == null) {
            return null;
        }

        return objectTypeDef.findReferenceAttributeDefinition(ref);
    }

    protected ResourceObjectTypeIdentificationType getObjectTypeOfSubject(PrismValueWrapper<ItemPathType> propertyWrapper) {
        PrismContainerValueWrapper<ShadowAssociationTypeSubjectDefinitionType> subject =
                propertyWrapper.getParentContainerValue(ShadowAssociationTypeSubjectDefinitionType.class);
        if (subject == null) {
            return null;
        }

        PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectType = null;
        try {
            objectType =
                    subject.findContainer(ShadowAssociationTypeSubjectDefinitionType.F_OBJECT_TYPE);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find child container object type in " + subject);
        }

        if (objectType == null || objectType.getValues().isEmpty()) {
            return null;
        }

        PrismContainerValueWrapper<ResourceObjectTypeIdentificationType> objectTypeValue = objectType.getValues().get(0);
        return objectTypeValue.getRealValue();
    }

    private ItemName getRef(PrismValueWrapper<ItemPathType> propertyWrapper) {
        PrismContainerValueWrapper<ShadowAssociationDefinitionType> associationTypeContainer =
                propertyWrapper.getParentContainerValue(ShadowAssociationDefinitionType.class);
        if (associationTypeContainer == null) {
            return null;
        }

        PrismPropertyWrapper<ItemPathType> refProperty = null;
        try {
            refProperty = associationTypeContainer.findProperty(ShadowAssociationDefinitionType.F_REF);
            if (refProperty == null || refProperty.getValue() == null) {
                return null;
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find property ref object in " + associationTypeContainer);
        }

        try {
            ItemPathType refBean = refProperty.getValue().getRealValue();
            if (refBean == null) {
                return null;
            }
            return refBean.getItemPath().firstName();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find value for property ref in " + associationTypeContainer);
        }

        return null;
    }

    @Override
    public Integer getOrder() {
        return 990;
    }
}
