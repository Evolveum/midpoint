/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

/** This is intended to help with refined resource definition parsing process. */
public class SchemaHandlingConfigItem
        extends ConfigurationItem<SchemaHandlingType> {

    @SuppressWarnings("unused") // called dynamically
    public SchemaHandlingConfigItem(@NotNull ConfigurationItem<SchemaHandlingType> original) {
        super(original);
    }

    public List<ResourceObjectClassDefinitionConfigItem> getObjectClasses() {
        return children(
                value().getObjectClass(),
                ResourceObjectClassDefinitionConfigItem.class,
                SchemaHandlingType.F_OBJECT_CLASS);
    }

    private List<ResourceObjectTypeDefinitionConfigItem> getObjectTypes() {
        return children(
                value().getObjectType(),
                ResourceObjectTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_OBJECT_TYPE);
    }

    /** These are currently embedded in "association type" definitions. */
    private List<ResourceObjectTypeDefinitionConfigItem> getAssociatedObjectTypes() {
        return getAssociationTypes().stream()
                .map(atDef -> atDef.getAssociationObject())
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ComplexAttributeTypeDefinitionConfigItem> getComplexAttributeTypes() {
        return children(
                value().getComplexAttributeType(),
                ComplexAttributeTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_COMPLEX_ATTRIBUTE_TYPE);
    }

    public List<ResourceDataTypeDefinitionConfigItem<?>> getAllObjectTypes() {
        List<ResourceDataTypeDefinitionConfigItem<?>> all = new ArrayList<>();
        all.addAll(getObjectTypes());
        all.addAll(getAssociatedObjectTypes());
        all.addAll(getComplexAttributeTypes());
        return all;
    }

    public List<ShadowAssociationTypeDefinitionConfigItem> getAssociationTypes() {
        return children(
                value().getAssociationType(),
                ShadowAssociationTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_ASSOCIATION_TYPE);
    }

    /**
     * Checks that all attribute names (`ref` elements) are valid - before even starting to parse the schema.
     * Otherwise, the parsing may fail at unusual places, generating confusing error messages. See also MID-8162.
     *
     * Maybe we can remove this method, as we migrated the beans to config items here.
     */
    public void checkSyntaxOfAttributeNames() throws ConfigurationException {
        for (var objectClassDefCI : getObjectClasses()) {
            objectClassDefCI.checkSyntaxOfAttributeNames();
        }
        for (var objectTypeDefCI : getAllObjectTypes()) {
            objectTypeDefCI.checkSyntaxOfAttributeNames();
        }
    }

    @Override
    public @NotNull String localDescription() {
        return "schema handling";
    }

    /** Returns association type definition beans for associations based on the specified reference attribute in given subject. */
    public @NotNull Collection<ShadowAssociationTypeDefinitionConfigItem> getAssociationTypesFor(
            @NotNull ResourceObjectTypeIdentification subjectTypeId, @NotNull ItemName refAttrName)
            throws ConfigurationException {
        List<ShadowAssociationTypeDefinitionConfigItem> matching = new ArrayList<>();
        for (ShadowAssociationTypeDefinitionConfigItem at : getAssociationTypes()) {
            var subject = at.getSubject();
            if (subject.getTypeIdentifiers().contains(subjectTypeId)
                    && subject.isBasedOnReferenceAttribute(refAttrName)) {
                matching.add(at);
            }
        }
        return matching;
    }
}
