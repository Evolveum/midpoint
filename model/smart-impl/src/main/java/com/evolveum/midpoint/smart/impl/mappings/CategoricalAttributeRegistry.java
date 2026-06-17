/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.mappings;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.EnumerationTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Registry of midPoint focus property paths that are categorical (enum-valued).
 *
 * Each entry maps a focus property path to its known set of allowed string values.
 * Initially focused on activation status attributes but designed to be generic.
 */
@Component
public class CategoricalAttributeRegistry {

    /**
     * Returns the categorical attribute definition for the given focus property path, if any.
     * A property is categorical if its type is an enumeration type in the Prism schema.
     */
    public Optional<List<String>> find(
            ItemPath focusPropertyPath,
            Class<? extends FocusType> focusClass) {

        // Hardcoded lifecycleState values from SchemaConstants
        if (focusPropertyPath.equivalent(ItemPath.create("lifecycleState"))) {
            var lifecycleStateValues = List.of(
                    SchemaConstants.LIFECYCLE_DRAFT,
                    SchemaConstants.LIFECYCLE_PROPOSED,
                    SchemaConstants.LIFECYCLE_ACTIVE,
                    SchemaConstants.LIFECYCLE_SUSPENDED,
                    SchemaConstants.LIFECYCLE_DEPRECATED,
                    SchemaConstants.LIFECYCLE_ARCHIVED,
                    SchemaConstants.LIFECYCLE_FAILED
            );
            return Optional.of(lifecycleStateValues);
        }

        var schemaRegistry = PrismContext.get().getSchemaRegistry();
        var focusTypeDef = schemaRegistry.findComplexTypeDefinitionByCompileTimeClass(focusClass);
        if (focusTypeDef == null) {
            return Optional.empty();
        }
        var propertyDef = focusTypeDef.findItemDefinition(focusPropertyPath, PrismPropertyDefinition.class);
        if (propertyDef == null) {
            return Optional.empty();
        }
        var enumTypeDef = schemaRegistry.findTypeDefinitionByType(
                propertyDef.getTypeName(), EnumerationTypeDefinition.class);
        if (enumTypeDef == null) {
            return Optional.empty();
        }
        var enumValues = enumTypeDef.getValues().stream()
                .map(EnumerationTypeDefinition.ValueDefinition::getValue)
                .toList();
        return Optional.of(enumValues);
    }
}
