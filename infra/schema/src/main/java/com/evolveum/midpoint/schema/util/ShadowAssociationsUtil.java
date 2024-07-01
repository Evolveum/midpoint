/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowReferenceAttributesType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Covers working with the "new" shadow associations (introduced in 4.9).
 *
 * Only methods that have to work with the raw values (like {@link ShadowAssociationValueType}) should belong here.
 * Pretty much everything that works with native types like {@link ShadowAssociationValue} should be attached to these types.
 */
public class ShadowAssociationsUtil {

    /**
     * @see ShadowAssociationValue#getSingleObjectRefRelaxed()
     */
    public static ObjectReferenceType getSingleObjectRefRelaxed(@NotNull ShadowAssociationValueType assocValueBean) {
        var objects = assocValueBean.getObjects();
        if (objects == null) {
            return null;
        }
        var items = objects.asPrismContainerValue().getItems();
        if (items.size() != 1) {
            return null;
        }
        var referenceValue = ((PrismReference) items.iterator().next()).getValue();
        return ObjectTypeUtil.createObjectRef(referenceValue);
    }

    /**
     * See {@link ShadowAssociationValue#getSingleObjectRefRequired()}
     */
    public static @NotNull ObjectReferenceType getSingleObjectRefRequired(ShadowAssociationValueType assocValueBean) {
        return stateNonNull(
                getSingleObjectRefRelaxed(assocValueBean),
                () -> "No object reference in " + assocValueBean);
    }

    public static @NotNull ComplexTypeDefinition getValueCtd() {
        return Objects.requireNonNull(
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(
                        ShadowAssociationValueType.class));
    }

    public static @NotNull PrismContainer<ShadowReferenceAttributesType> instantiateObjectsContainer() {
        try {
            return getValueCtd()
                    .<ShadowReferenceAttributesType>findContainerDefinition(ShadowAssociationValueType.F_OBJECTS)
                    .instantiate();
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    /**
     * Creates a trivial (single-object-ref) association value as the raw (definition-less) bean.
     *
     * @see #createValueFromDefaultObject(AbstractShadow)
     */
    public static @NotNull ShadowAssociationValueType createSingleRefRawValue(
            @NotNull ItemName refName, @NotNull ShadowType shadow) {
        return createSingleRefRawValue(refName, ObjectTypeUtil.createObjectRef(shadow));
    }

    public static @NotNull ShadowAssociationValueType createSingleRefRawValue(
            @NotNull ItemName refName, @NotNull String shadowOid) {
        return createSingleRefRawValue(refName, ObjectTypeUtil.createObjectRef(shadowOid, ObjectTypes.SHADOW));
    }

    public static @NotNull ShadowAssociationValueType createSingleRefRawValue(
            @NotNull ItemName refName, @NotNull ObjectReferenceType refValue) {
        try {
            var objectRef = PrismContext.get().itemFactory().createReference(refName);
            objectRef.add(refValue.asReferenceValue().clone());

            var objectsContainer = instantiateObjectsContainer();
            objectsContainer.add(objectRef);

            var newValue = new ShadowAssociationValueType();
            //noinspection unchecked
            newValue.asPrismContainerValue().add(objectsContainer);

            return newValue;
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }
}
