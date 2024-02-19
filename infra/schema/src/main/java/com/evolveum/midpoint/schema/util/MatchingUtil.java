/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

/**
 * TEMPORARY implementation! This class contains various hacking util methods helping during development of ID Match correlation.
 */
public class MatchingUtil {

    private static final Trace LOGGER = TraceManager.getTrace(MatchingUtil.class);

    /**
     * Extracts properties suitable for matching (single-valued).
     */
    public static List<PrismProperty<?>> getSingleValuedProperties(@NotNull ObjectType object) {
        List<PrismProperty<?>> properties = new ArrayList<>();
        //noinspection unchecked
        object.asPrismObject().accept(visitable -> {
            if (visitable instanceof PrismProperty<?> property) {
                if (property.size() > 1) {
                    LOGGER.trace("getSingleValuedProperties: Ignoring property because of multiple values: {}", property);
                } else {
                    LOGGER.trace("getSingleValuedProperties: Using property {}", property);
                    properties.add(property);
                }
            }
        });
        return properties;
    }

    @SuppressWarnings("unused") // Used from scripts
    public static Set<String> getValuesForPath(ObjectType object, Object... pathComponents) {
        return getValuesForPath(object.asPrismObject(), ItemPath.create(pathComponents));
    }

    public static Set<String> getValuesForPath(PrismObject<?> object, ItemPath path) {
        return object.getAllValues(path).stream()
                .filter(Objects::nonNull)
                .map(PrismValue::getRealValue)
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }

    public static Set<?> getRealValuesForPath(Containerable containerable, ItemPath path) {
        return getRealValuesForPath(containerable.asPrismContainerValue(), path);
    }

    private static Set<?> getRealValuesForPath(PrismContainerValue<?> pcv, ItemPath path) {
        return pcv.getAllValues(path).stream()
                .filter(Objects::nonNull)
                .map(PrismValue::getRealValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Finds a property even if the path was reduced by removing all container IDs.
     *
     * Returns the first property found. (Later we may combine all properties into single one.)
     */
    public static @Nullable PrismProperty<?> findProperty(ObjectType object, ItemPath path) {
        List<PrismProperty<?>> matching = new ArrayList<>();
        //noinspection unchecked
        object.asPrismObject().accept(visitable -> {
            if (visitable instanceof PrismProperty<?>) {
                PrismProperty<?> property = (PrismProperty<?>) visitable;
                if (property.getPath().namedSegmentsOnly().equivalent(path)) {
                    matching.add(property);
                }
            }
        });
        if (matching.isEmpty()) {
            return null;
        } else {
            return matching.get(0);
        }
    }

    /**
     * Copies attributes into focus object. Candidate items are looked for in the root container
     * and in the extension. They are matched using the item name. Type conversion (e.g. polystring <-> string)
     * is attempted as well.
     */
    public static void copyAttributes(FocusType preFocus, ShadowType resourceObject) throws SchemaException {
        ShadowAttributesType attributes = resourceObject.getAttributes();
        if (attributes != null) {
            //noinspection unchecked
            for (Item<?, ?> attribute : (Collection<Item<?, ?>>) attributes.asPrismContainerValue().getItems()) {
                putIntoFocus(preFocus, attribute);
            }
        }
    }

    private static void putIntoFocus(FocusType preFocus, Item<?, ?> attributeItem) throws SchemaException {
        LOGGER.debug("Converting {}", attributeItem);
        if (!(attributeItem instanceof PrismProperty<?>)) {
            LOGGER.trace("Not a property: {}", attributeItem);
            return;
        }
        PrismProperty<?> attribute = (PrismProperty<?>) attributeItem;

        PrismObject<? extends FocusType> preFocusObject = preFocus.asPrismObject();

        PrismObjectDefinition<? extends FocusType> def =
                Objects.requireNonNull(
                        preFocusObject.getDefinition(),
                        () -> "no definition for pre-focus: " + preFocus);

        String localName = attribute.getElementName().getLocalPart();
        ItemName directPath = new ItemName("", localName);
        PrismPropertyDefinition<?> directDef = def.findPropertyDefinition(directPath);
        if (directDef != null && isCompatible(attribute, directDef)) {
            if (preFocusObject.findItem(directPath) == null) {
                preFocusObject.add(
                        createPropertyClone(attribute, directDef));
            }
            return;
        }

        ItemPath extensionPath = ItemPath.create(ObjectType.F_EXTENSION, directPath);
        PrismPropertyDefinition<Object> extensionDef = def.findPropertyDefinition(extensionPath);
        if (extensionDef != null && isCompatible(attribute, extensionDef)) {
            if (preFocusObject.findItem(extensionPath) == null) {
                preFocusObject.getOrCreateExtension().getValue()
                        .add(createPropertyClone(attribute, extensionDef));
            }
            return;
        }

        LOGGER.trace("{} has no definition in focus", localName);
    }

    /** Can we copy the attribute to the target (knowing its definition)? */
    private static boolean isCompatible(PrismProperty<?> attribute, PrismPropertyDefinition<?> targetDef) {
        Class<?> targetType = targetDef.getTypeClass();
        if (targetType == null) {
            return true; // most probably ok
        }
        for (Object realValue : attribute.getRealValues()) {
            if (!targetType.isAssignableFrom(realValue.getClass())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @NotNull private static PrismProperty<?> createPropertyClone(
            PrismProperty<?> attribute, PrismPropertyDefinition<?> directDef) throws SchemaException {
        PrismProperty<Object> property = (PrismProperty<Object>) directDef.instantiate();
        Class<?> targetType = directDef.getTypeClass();
        if (targetType == null) {
            //noinspection unchecked,rawtypes
            property.addAll((Collection) CloneUtil.cloneCollectionMembers(attribute.getValues()));
        } else {
            for (Object realValue : attribute.getRealValues()) {
                property.addRealValue(
                        JavaTypeConverter.convert(targetType, realValue, false));
            }
        }
        return property;
    }
}
