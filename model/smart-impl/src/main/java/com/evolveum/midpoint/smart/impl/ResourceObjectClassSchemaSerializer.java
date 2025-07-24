package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiObjectSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/** Serializes {@link ResourceObjectClassDefinition} into {@link SiObjectSchemaType}. */
class ResourceObjectClassSchemaSerializer {

    private final ResourceObjectClassDefinition objectClassDef;

    private ResourceObjectClassSchemaSerializer(ResourceObjectClassDefinition objectClassDef) {
        this.objectClassDef = objectClassDef;
    }

    static SiObjectSchemaType serialize(ResourceObjectClassDefinition objectClassDef) {
        return new ResourceObjectClassSchemaSerializer(objectClassDef).serialize();
    }

    // TODO: complex attributes
    // TODO: non-attribute items, like activation or credentials
    public SiObjectSchemaType serialize() {
        var schema = new SiObjectSchemaType()
                .name(objectClassDef.getObjectClassName())
                .description(objectClassDef.getDescription()); // TODO change to native description
        for (ShadowAttributeDefinition<?, ?, ?, ?> attributeDefinition : objectClassDef.getAttributeDefinitions()) {
            schema.getAttribute().add(
                    new SiAttributeDefinitionType()
                            .name(new ItemPathType(attributeDefinition.getItemName()))
                            .type(attributeDefinition.getTypeName())
                            .description(attributeDefinition.getDescription())); // TODO change to native description
        }
        return schema;
    }
}
