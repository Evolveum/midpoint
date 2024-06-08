/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeIdentificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Nullable;

public class AssociationChildWrapperUtil {

    private static final Trace LOGGER = TraceManager.getTrace(DetailsPageUtil.class);

    public static ShadowReferenceAttributeDefinition getShadowReferenceAttribute(PrismValueWrapper propertyWrapper, PageAdminLTE pageBase) {
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
        return getShadowReferenceAttribute(schema, propertyWrapper);
    }

    public static ShadowReferenceAttributeDefinition getShadowReferenceAttribute(ResourceSchema schema, PrismValueWrapper propertyWrapper) {
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

    public static ResourceObjectTypeIdentificationType getObjectTypeOfSubject(PrismValueWrapper propertyWrapper) {
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

    private static ItemName getRef(PrismValueWrapper<ItemPathType> propertyWrapper) {
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
}
