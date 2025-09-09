/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.impl.schema.features.DefinitionFeatures.XsdSerializers;
import com.evolveum.midpoint.prism.impl.schema.features.DefinitionFeatures.XsomParsers;
import com.evolveum.midpoint.prism.schema.DefinitionFeature;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition.NativeObjectClassDefinitionBuilder;
import com.evolveum.midpoint.schema.processor.NativeShadowAttributeDefinition.NativeShadowAttributeDefinitionBuilder;

import com.sun.xml.xsom.XSComplexType;

import javax.xml.namespace.QName;

/**
 * Features specific to [native] resource object classes and attributes.
 */
class ResourceDefinitionFeatures {

    static class ForClass {

        static final DefinitionFeature<String, NativeObjectClassDefinitionBuilder, Object, ?> DF_NATIVE_OBJECT_CLASS_NAME =
                DefinitionFeature.of(
                        String.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setNativeObjectClassName,
                        XsomParsers.string(MidPointConstants.RA_NATIVE_OBJECT_CLASS),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::getNativeObjectClassName,
                        XsdSerializers.string(MidPointConstants.RA_NATIVE_OBJECT_CLASS));

        static final DefinitionFeature<Boolean, NativeObjectClassDefinitionBuilder, Object, ?> DF_DEFAULT_ACCOUNT_DEFINITION =
                DefinitionFeature.of(
                        Boolean.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setDefaultAccountDefinition,
                        XsomParsers.marker(MidPointConstants.RA_DEFAULT),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::isDefaultAccountDefinition,
                        XsdSerializers.aBoolean(MidPointConstants.RA_DEFAULT));

        static final DefinitionFeature<Boolean, NativeObjectClassDefinitionBuilder, Object, ?> DF_AUXILIARY =
                DefinitionFeature.of(
                        Boolean.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setAuxiliary,
                        XsomParsers.marker(MidPointConstants.RA_AUXILIARY),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::isAuxiliary,
                        XsdSerializers.aBoolean(MidPointConstants.RA_AUXILIARY));

        static final DefinitionFeature<Boolean, NativeObjectClassDefinitionBuilder, Object, ?> DF_EMBEDDED =
                DefinitionFeature.of(
                        Boolean.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setEmbedded,
                        XsomParsers.marker(MidPointConstants.RA_EMBEDDED),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::isEmbedded,
                        XsdSerializers.aBoolean(MidPointConstants.RA_EMBEDDED));

        static final DefinitionFeature<QName, NativeObjectClassDefinitionBuilder, Object, ?> DF_NAMING_ATTRIBUTE_NAME =
                DefinitionFeature.of(
                        QName.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setNamingAttributeName,
                        XsomParsers.qName(MidPointConstants.RA_NAMING_ATTRIBUTE),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::getNamingAttributeName,
                        XsdSerializers.qName(MidPointConstants.RA_NAMING_ATTRIBUTE));

        static final DefinitionFeature<QName, NativeObjectClassDefinitionBuilder, Object, ?> DF_DISPLAY_NAME_ATTRIBUTE_NAME =
                DefinitionFeature.of(
                        QName.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setDisplayNameAttributeName,
                        XsomParsers.qName(MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::getDisplayNameAttributeName,
                        XsdSerializers.qName(MidPointConstants.RA_DISPLAY_NAME_ATTRIBUTE));

        static final DefinitionFeature<QName, NativeObjectClassDefinitionBuilder, Object, ?> DF_DESCRIPTION_ATTRIBUTE_NAME =
                DefinitionFeature.of(
                        QName.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setDescriptionAttributeName,
                        XsomParsers.qName(MidPointConstants.RA_DESCRIPTION_ATTRIBUTE),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::getDescriptionAttributeName,
                        XsdSerializers.qName(MidPointConstants.RA_DESCRIPTION_ATTRIBUTE));

        static final DefinitionFeature<QName, NativeObjectClassDefinitionBuilder, Object, ?> DF_PRIMARY_IDENTIFIER_NAME =
                DefinitionFeature.of(
                        QName.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setPrimaryIdentifierName,
                        XsomParsers.qName(MidPointConstants.RA_IDENTIFIER),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::getPrimaryIdentifierName,
                        XsdSerializers.qName(MidPointConstants.RA_IDENTIFIER));

        static final DefinitionFeature<QName, NativeObjectClassDefinitionBuilder, Object, ?> DF_SECONDARY_IDENTIFIER_NAME =
                DefinitionFeature.of(
                        QName.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setSecondaryIdentifierName,
                        XsomParsers.qName(MidPointConstants.RA_SECONDARY_IDENTIFIER),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::getSecondaryIdentifierName,
                        XsdSerializers.qName(MidPointConstants.RA_SECONDARY_IDENTIFIER));

        /**
         * `true` denotes resource object class, `false` denotes resource reference type. Not very nice but practical.
         */
        static final DefinitionFeature<Boolean, NativeObjectClassDefinitionBuilder, XSComplexType, ?> DF_RESOURCE_OBJECT =
                DefinitionFeature.of(
                        Boolean.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setResourceObject,
                        XsomParsers.markerForComplexType(MidPointConstants.RA_RESOURCE_OBJECT),
                        NativeComplexTypeDefinitionImpl.class,
                        NativeComplexTypeDefinitionImpl::isResourceObjectClass,
                        XsdSerializers.aBoolean(MidPointConstants.RA_RESOURCE_OBJECT));

        static final DefinitionFeature<String, NativeObjectClassDefinitionBuilder, Object, ?> DF_DESCRIPTION_NAME =
                DefinitionFeature.of(
                        String.class,
                        NativeObjectClassDefinitionBuilder.class,
                        NativeObjectClassDefinitionBuilder::setDescription,
                        XsomParsers.string(MidPointConstants.RA_DESCRIPTION),
                        NativeObjectClassUcfDefinition.class,
                        NativeObjectClassUcfDefinition::getDescription,
                        XsdSerializers.string(MidPointConstants.RA_DESCRIPTION));
    }

    static class ForItem {

        static final DefinitionFeature<String, NativeShadowAttributeDefinitionBuilder, Object, ?> DF_NATIVE_ATTRIBUTE_NAME =
                DefinitionFeature.of(
                        String.class,
                        NativeShadowAttributeDefinitionBuilder.class,
                        NativeShadowAttributeDefinitionBuilder::setNativeAttributeName,
                        XsomParsers.string(MidPointConstants.RA_NATIVE_ATTRIBUTE_NAME),
                        NativeShadowAttributeDefinition.class,
                        NativeShadowAttributeDefinition::getNativeAttributeName,
                        XsdSerializers.string(MidPointConstants.RA_NATIVE_ATTRIBUTE_NAME));

        static final DefinitionFeature<String, NativeShadowAttributeDefinitionBuilder, Object, ?> DF_FRAMEWORK_ATTRIBUTE_NAME =
                DefinitionFeature.of(
                        String.class,
                        NativeShadowAttributeDefinitionBuilder.class,
                        NativeShadowAttributeDefinitionBuilder::setFrameworkAttributeName,
                        XsomParsers.string(MidPointConstants.RA_FRAMEWORK_ATTRIBUTE_NAME),
                        NativeShadowAttributeDefinition.class,
                        NativeShadowAttributeDefinition::getFrameworkAttributeName,
                        XsdSerializers.string(MidPointConstants.RA_FRAMEWORK_ATTRIBUTE_NAME));

        static final DefinitionFeature<Boolean, NativeShadowAttributeDefinitionBuilder, Object, ?> DF_RETURNED_BY_DEFAULT =
                DefinitionFeature.of(
                        Boolean.class,
                        NativeShadowAttributeDefinitionBuilder.class,
                        NativeShadowAttributeDefinitionBuilder::setReturnedByDefault,
                        XsomParsers.marker(MidPointConstants.RA_RETURNED_BY_DEFAULT_NAME),
                        NativeShadowAttributeDefinition.class,
                        NativeShadowAttributeDefinition::getReturnedByDefault,
                        XsdSerializers.aBoolean(MidPointConstants.RA_RETURNED_BY_DEFAULT_NAME));

        static final DefinitionFeature<ShadowReferenceParticipantRole, NativeShadowAttributeDefinitionBuilder, Object, ?> DF_ROLE_IN_REFERENCE =
                DefinitionFeature.of(
                        ShadowReferenceParticipantRole.class,
                        NativeShadowAttributeDefinitionBuilder.class,
                        NativeShadowAttributeDefinitionBuilder::setReferenceParticipantRole,
                        XsomParsers.enumBased(ShadowReferenceParticipantRole.class, MidPointConstants.RA_ROLE_IN_REFERENCE, ShadowReferenceParticipantRole::getValue),
                        NativeShadowAttributeDefinition.class,
                        NativeShadowAttributeDefinition::getReferenceParticipantRoleIfPresent,
                        XsdSerializers.enumBased(ShadowReferenceParticipantRole.class, MidPointConstants.RA_ROLE_IN_REFERENCE, ShadowReferenceParticipantRole::getValue));

        static final DefinitionFeature<QName, NativeShadowAttributeDefinitionBuilder, Object, ?> DF_REFERENCED_OBJECT_CLASS_NAME =
                DefinitionFeature.of(
                        QName.class,
                        NativeShadowAttributeDefinitionBuilder.class,
                        NativeShadowAttributeDefinitionBuilder::setReferencedObjectClassName,
                        XsomParsers.qName(MidPointConstants.RA_REFERENCED_OBJECT_CLASS_NAME),
                        NativeShadowAttributeDefinition.class,
                        NativeShadowAttributeDefinition::getReferencedObjectClassName,
                        XsdSerializers.qName(MidPointConstants.RA_REFERENCED_OBJECT_CLASS_NAME));

        static final DefinitionFeature<Boolean, NativeShadowAttributeDefinitionBuilder, Object, ?> DF_COMPLEX_ATTRIBUTE =
                DefinitionFeature.of(
                        Boolean.class,
                        NativeShadowAttributeDefinitionBuilder.class,
                        NativeShadowAttributeDefinitionBuilder::setComplexAttribute,
                        XsomParsers.marker(MidPointConstants.RA_COMPLEX_ATTRIBUTE),
                        NativeShadowAttributeDefinition.class,
                        NativeShadowAttributeDefinition::isComplexAttribute,
                        XsdSerializers.aBooleanDefaultFalse(MidPointConstants.RA_COMPLEX_ATTRIBUTE));

        static final DefinitionFeature<String, NativeShadowAttributeDefinitionBuilder, Object, ?> DF_DESCRIPTION_NAME =
                DefinitionFeature.of(
                        String.class,
                        NativeShadowAttributeDefinitionBuilder.class,
                        NativeShadowAttributeDefinitionBuilder::setNativeDescription,
                        XsomParsers.string(MidPointConstants.RA_DESCRIPTION),
                        NativeShadowAttributeDefinition.class,
                        NativeShadowAttributeDefinition::getNativeDescription,
                        XsdSerializers.string(MidPointConstants.RA_DESCRIPTION));
    }
}
