/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//FIXME serializable?
@Component
public class AssociationDefinitionRefPanelFactory
        extends AssociationAttributeMappingItemPathPanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationDefinitionRefPanelFactory.class);

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper.getParentContainerValue(ShadowAssociationDefinitionType.class) != null
                && ShadowAssociationDefinitionType.F_REF.equivalent(wrapper.getItemName())) {
            return true;
        }
        return false;
    }

    @Override
    protected List<DisplayableValue<ItemPathType>> getAttributes(ResourceSchema schema, PrismValueWrapper<ItemPathType> propertyWrapper) {
        if (schema == null) {
            return Collections.emptyList();
        }

        ResourceObjectTypeIdentificationType objectTypeOfSubject = getObjectTypeOfSubject(propertyWrapper);
        if (objectTypeOfSubject == null) {
            return Collections.emptyList();
        }

        QName objectClassOfObject = getObjectClassOfObject(propertyWrapper, schema);
        if (objectClassOfObject == null) {
            return Collections.emptyList();
        }

        @Nullable ResourceObjectTypeDefinition objectTypeDef = schema.getObjectTypeDefinition(objectTypeOfSubject.getKind(), objectTypeOfSubject.getIntent());
        if (objectTypeDef == null) {
            return Collections.emptyList();
        }

        List<? extends ShadowReferenceAttributeDefinition> refAttributes = objectTypeDef.getReferenceAttributeDefinitions().stream()
                .filter(referenceAttributeDef ->
                        QNameUtil.match(objectClassOfObject, referenceAttributeDef.getRepresentativeTargetObjectDefinition().getObjectClassName()))
                .toList();

        List<DisplayableValue<ItemPathType>> attributes = new ArrayList<>();
        refAttributes.forEach(attribute -> attributes.add(createDisplayValue(attribute)));
        return attributes;
    }


    private QName getObjectClassOfObject(PrismValueWrapper<ItemPathType> propertyWrapper, ResourceSchema schema) {
        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> associationTypeContainer =
                propertyWrapper.getParentContainerValue(ShadowAssociationTypeDefinitionType.class);
        if (associationTypeContainer == null) {
            return null;
        }

        PrismContainerWrapper<ShadowAssociationTypeObjectDefinitionType> objectContainer = null;
        try {
            objectContainer =
                    associationTypeContainer.findContainer(ShadowAssociationTypeDefinitionType.F_OBJECT);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find child container object in " + associationTypeContainer);
        }

        if (objectContainer == null || objectContainer.getValues().isEmpty()) {
            return null;
        }

        PrismContainerWrapper<ResourceObjectTypeIdentificationType> objectType = null;
        try {
            objectType =
                    objectContainer.getValues().iterator().next().findContainer(ShadowAssociationTypeObjectDefinitionType.F_OBJECT_TYPE);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find child container object type in " + associationTypeContainer);
        }

        if (objectType == null || objectType.getValues().isEmpty()) {
            return null;
        }

        ResourceObjectTypeIdentificationType objectTypeBean = objectType.getValues().get(0).getRealValue();
        if (objectTypeBean == null) {
            return null;
        }

        @Nullable ResourceObjectTypeDefinition objectTypeDef = schema.getObjectTypeDefinition(objectTypeBean.getKind(), objectTypeBean.getIntent());
        if (objectTypeDef == null) {
            return null;
        }

        return objectTypeDef.getObjectClassName();
    }
}
