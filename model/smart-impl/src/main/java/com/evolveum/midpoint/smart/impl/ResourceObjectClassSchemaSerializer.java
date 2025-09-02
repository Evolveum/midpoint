package com.evolveum.midpoint.smart.impl;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiObjectSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/** Serializes {@link ResourceObjectClassDefinition} into {@link SiObjectSchemaType}. */
class ResourceObjectClassSchemaSerializer extends SchemaSerializer {

    private final ResourceObjectClassDefinition objectClassDef;
    private final ResourceType resource;

    private ResourceObjectClassSchemaSerializer(ResourceObjectClassDefinition objectClassDef, ResourceType resource) {
        this.objectClassDef = objectClassDef;
        this.resource = resource;
    }

    static ResourceObjectClassSchemaSerializer create(ResourceObjectClassDefinition objectClassDef, ResourceType resource) {
        return new ResourceObjectClassSchemaSerializer(objectClassDef, resource);
    }

    static SiObjectSchemaType serialize(ResourceObjectClassDefinition objectClassDef, ResourceType resource) {
        return create(objectClassDef, resource).serialize();
    }

    // TODO: complex attributes
    public SiObjectSchemaType serialize() {
        var shadowDefinition = objectClassDef.getPrismObjectDefinition();
        var schema = new SiObjectSchemaType()
                .name(objectClassDef.getObjectClassName())
                .description(objectClassDef.getDescription()); // TODO change to native description
        for (ShadowAttributeDefinition<?, ?, ?, ?> attributeDefinition : objectClassDef.getAttributeDefinitions()) {
            schema.getAttribute().add(
                    new SiAttributeDefinitionType()
                            .name(DescriptiveItemPath.of(attributeDefinition.getStandardPath(), shadowDefinition).asString())
                            .type(fixTypeName(attributeDefinition.getTypeName()))
                            .description(attributeDefinition.getDescription()) // TODO change to native description
                            .minOccurs(attributeDefinition.getMinOccurs())
                            .maxOccurs(attributeDefinition.getMaxOccurs()));
        }
        var credentialsCapability = objectClassDef.getEnabledCapability(CredentialsCapabilityType.class, resource);
        if (credentialsCapability != null && CapabilityUtil.getEnabledPasswordCapability(credentialsCapability) != null) {
            schema.getAttribute().add(
                    new SiAttributeDefinitionType()
                            .name(DescriptiveItemPath.of(PATH_CREDENTIALS_PASSWORD_VALUE, shadowDefinition).asString())
                            .type(fixTypeName(ProtectedStringType.COMPLEX_TYPE))
                            .minOccurs(0)
                            .maxOccurs(1));
        }
        var activationCapability = objectClassDef.getEnabledCapability(ActivationCapabilityType.class, resource);
        if (activationCapability != null) {
            if (CapabilityUtil.getEnabledActivationStatus(activationCapability) != null) {
                schema.getAttribute().add(
                        new SiAttributeDefinitionType()
                                .name(DescriptiveItemPath.of(PATH_ACTIVATION_ADMINISTRATIVE_STATUS, shadowDefinition).asString())
                                .type(fixTypeName(C_ACTIVATION_STATUS_TYPE))
                                .minOccurs(0)
                                .maxOccurs(1));
            }
            if (CapabilityUtil.getEnabledActivationValidFrom(activationCapability) != null) {
                schema.getAttribute().add(
                        new SiAttributeDefinitionType()
                                .name(DescriptiveItemPath.of(PATH_ACTIVATION_VALID_FROM, shadowDefinition).asString())
                                .type(fixTypeName(DOMUtil.XSD_DATETIME))
                                .minOccurs(0)
                                .maxOccurs(1));
            }
            if (CapabilityUtil.getEnabledActivationValidTo(activationCapability) != null) {
                schema.getAttribute().add(
                        new SiAttributeDefinitionType()
                                .name(DescriptiveItemPath.of(PATH_ACTIVATION_VALID_TO, shadowDefinition).asString())
                                .type(fixTypeName(DOMUtil.XSD_DATETIME))
                                .minOccurs(0)
                                .maxOccurs(1));
            }
            // TODO what to do with lockout status?
        }
        return schema;
    }
}
