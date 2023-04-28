/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.error;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import static com.evolveum.midpoint.util.QNameUtil.getLocalPart;

/**
 * This class should provide user-understandable messages related to configuration errors, i.e. something that is (clearly)
 * wrong with the configuration. An example: missing "ref" property in a template or resource item definition.
 *
 * It relies on parent-child relationship between prism items and their values.
 *
 * LIMITED and TEMPORARY. In the future, we will probably drop this relationship from the prism structures.
 * Instead, some context object will be passed when getting data from those configuration objects. This will complicate
 * the code a bit.
 *
 * Maybe the serious solution should check those configuration errors in a controlled way, e.g. when an object is imported
 * or "tested"; the runtime code could report these errors in a simpler, less user-friendly way. This would reduce the overhead
 * of carrying the whole operation context. On the other hand, configuration objects can be modified unexpectedly, e.g. by
 * executing arbitrary deltas at any given time. This would need to be limited somehow.
 */
@Experimental
public class ConfigErrorReporter {

    public static String describe(@NotNull ItemRefinedDefinitionType itemRefinedDefinition) {
        PrismContainerValue<?> pcv = itemRefinedDefinition.asPrismContainerValue();
        Objectable top = PrismValueUtil.getRootObject(pcv);
        if (top instanceof ResourceType) {
            return String.format(
                    "%s definition with ID %d%s in %s",
                    getItemDefinitionDescription(itemRefinedDefinition),
                    itemRefinedDefinition.getId(),
                    getClassOrTypeDescription(pcv), top);
        } else if (top != null) {
            return "item definition with ID " + itemRefinedDefinition.getId() + " in " + top;
        } else {
            return "item definition " + itemRefinedDefinition;
        }
    }

    private static String getItemDefinitionDescription(@NotNull ItemRefinedDefinitionType itemRefinedDefinition) {
        if (itemRefinedDefinition instanceof ResourceAttributeDefinitionType) {
            return "attribute";
        } else if (itemRefinedDefinition instanceof ResourceObjectAssociationType) {
            return "association";
        } else {
            return "a";
        }
    }

    private static String getClassOrTypeDescription(PrismValue value) {
        var typeDef = PrismValueUtil.getNearestValueOfType(value, ResourceObjectTypeDefinitionType.class);
        if (typeDef == null) {
            return "";
        }
        var parent = typeDef.asPrismContainerValue().getParent();
        if (!(parent instanceof Item)) {
            return "";
        }
        ItemName defItemName = parent.getElementName();
        if (QNameUtil.match(defItemName, SchemaHandlingType.F_OBJECT_TYPE)) {
            return " in the definition of type " + ResourceObjectTypeIdentification.of(typeDef);
        } else if (QNameUtil.match(defItemName, SchemaHandlingType.F_OBJECT_CLASS)) {
            return " in the refined definition of class " + getLocalPart(typeDef.getObjectClass());
        } else {
            return "";
        }
    }
}
