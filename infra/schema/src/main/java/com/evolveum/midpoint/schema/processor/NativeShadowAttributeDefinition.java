/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.ShortDumpable;

import javax.xml.namespace.QName;

/**
 * NOTE: Never try to determine type (simple/reference) by querying the interfaces. The default implementation implements
 * both interfaces. Use {@link #isSimple()} and {@link #isReference()} methods instead.
 */
public interface NativeShadowAttributeDefinition extends
        Cloneable, Freezable, Serializable, ShortDumpable,
        PrismItemBasicDefinition,
        PrismItemAccessDefinition,
        PrismItemMiscDefinition,
        PrismPresentationDefinition,
        ShadowAttributeUcfDefinition {

    @Nullable
    ShadowReferenceParticipantRole getReferenceParticipantRoleIfPresent();

    @NotNull
    ShadowReferenceParticipantRole getReferenceParticipantRole();

    QName getReferencedObjectClassName();

    boolean isComplexAttribute();

    NativeShadowAttributeDefinition clone();

    NativeShadowAttributeDefinition cloneWithNewCardinality(int newMinOccurs, int maxOccurs);

    boolean isSimple();

    boolean isReference();

    @NotNull NativeShadowSimpleAttributeDefinition<?> asSimple();

    @NotNull NativeShadowReferenceAttributeDefinition asReference();

    interface NativeShadowAttributeDefinitionBuilder extends ItemDefinition.ItemDefinitionLikeBuilder {

        void setNativeAttributeName(String value);
        void setFrameworkAttributeName(String value);
        void setReturnedByDefault(Boolean value);
        void setReferenceParticipantRole(ShadowReferenceParticipantRole value);
        void setReferencedObjectClassName(QName value);
        void setNativeDescription(String s);
        void setComplexAttribute(boolean value);
    }
}
