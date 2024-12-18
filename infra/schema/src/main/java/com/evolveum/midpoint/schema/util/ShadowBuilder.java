/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/** Used for easy creation of shadow objects (with the correct definition). */
public class ShadowBuilder {

    @NotNull private final ShadowType shadow;

    private ShadowBuilder(@NotNull ShadowType shadow) {
        this.shadow = shadow;
    }

    /**
     * Creates a new shadow builder with the provided object definition.
     *
     * Unlike {@link ResourceObjectDefinition#createBlankShadow()}, this method sets kind/intent if known.
     * Hence, please do not call it with a default-type definition for an object class if you don't want
     * to instantiate a shadow with kind/intent.
     */
    public static ShadowBuilder withDefinition(@NotNull ResourceObjectDefinition objectDefinition) {
        var blankShadow = objectDefinition.createBlankShadow();
        blankShadow.setObjectType(objectDefinition.getTypeIdentification());
        return new ShadowBuilder(blankShadow.getBean());
    }

    // TODO consider removing
    public ShadowBuilder onResource(@Nullable String oid) {
        shadow.setResourceRef(oid != null ? ObjectTypeUtil.createObjectRef(oid, ObjectTypes.RESOURCE) : null);
        return this;
    }

    public ShadowBuilder withSimpleAttribute(QName attrName, Object realValue) throws SchemaException {
        ShadowUtil.getOrCreateAttributesContainer(shadow)
                .addSimpleAttribute(attrName, realValue);
        return this;
    }

    public ShadowBuilder withReferenceAttribute(QName attrName, AbstractShadow referencedShadow)
            throws SchemaException {
        ShadowUtil
                .getOrCreateAttributesContainer(shadow)
                .addReferenceAttribute(attrName, referencedShadow);
        return this;
    }

    public ShadowBuilder withPassword(@NotNull String clearValue) {
        return withPassword(new ProtectedStringType().clearValue(clearValue));
    }

    public ShadowBuilder withPassword(@NotNull ProtectedStringType value) {
        ShadowUtil.setPassword(shadow, value);
        return this;
    }

    public PrismObject<ShadowType> asPrismObject() {
        return shadow.asPrismObject();
    }

    public ShadowType asObjectable() {
        return shadow;
    }

    public AbstractShadow asAbstractShadow() {
        return AbstractShadow.of(shadow);
    }
}
