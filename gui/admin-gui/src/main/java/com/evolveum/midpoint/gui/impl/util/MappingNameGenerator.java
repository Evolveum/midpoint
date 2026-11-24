/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.util;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;

/** Suggests unique names for resource, construction, object-template, and focus mappings. */
public final class MappingNameGenerator {

    private static final String INBOUND_PATTERN = "%s-into-%s";
    private static final String OUTBOUND_PATTERN = "%s-to-%s";
    private static final String SOURCES_PATTERN = "generate-%s-from-%s";

    /** Prevents instantiation. */
    private MappingNameGenerator() {
    }

    /**
     * Suggests a unique name when the input is empty or still matches a SYSTEM-marked model value.
     * Keeps names that already match the current base, including a numeric suffix.
     * Uses the enclosing object to check for duplicates, excluding this mapping's own name.
     */
    public static String suggestUpdatedName(
            PrismContainerValueWrapper<?> mapping, String currentName) throws SchemaException {
        MappingContext context = resolveContext(mapping);
        String newBase = computeBaseName(mapping);
        var nameProperty = mapping.<String>findProperty(MappingType.F_NAME);
        var nameValue = nameProperty != null ? nameProperty.getValue() : null;
        String ownName = nameValue != null ? nameValue.getRealValue() : null;
        boolean systemGenerated = nameValue != null
                && Objects.equals(currentName, ownName)
                && SmartMetadataUtil.isMarkedAsSystemProvided(nameValue.getNewValue());
        if (!shouldUpdateName(context, newBase, currentName, systemGenerated)) {
            return null;
        }
        PrismContainerWrapper<?> container = mapping.getParent();
        var objectValue = container.findObjectWrapper().getValue();
        return suggestUpdatedName(context, newBase, currentName, systemGenerated,
                name -> WebPrismUtil.getNumberOfSameMappingNames(objectValue, name)
                        > (StringUtils.equals(StringUtils.trim(name), StringUtils.trim(ownName)) ? 1 : 0));
    }

    /**
     * Chooses a unique name if automatic replacement is allowed.
     * The collision lookup must exclude the mapping being renamed.
     */
    static String suggestUpdatedName(
            MappingContext context, String newBase, String currentName, boolean systemGenerated, Predicate<String> isNameUsed) {
        return shouldUpdateName(context, newBase, currentName, systemGenerated)
                ? suggestUniqueName(newBase, isNameUsed) : null;
    }

    /** Allows generation for empty names and updates to SYSTEM-marked names when the base changes. */
    private static boolean shouldUpdateName(
            MappingContext context, String newBase, String currentName, boolean systemGenerated) {
        return context != MappingContext.UNSUPPORTED && newBase != null
                && (StringUtils.isEmpty(currentName)
                || (systemGenerated && !isGeneratedName(currentName, newBase)));
    }

    /**
     * Checks whether the name matches the base, optionally followed by a suffix such as {@code _2}.
     * This compares the text only; SYSTEM metadata determines whether the name was generated.
     */
    static boolean isGeneratedName(String name, String baseName) {
        return name != null && baseName != null && Pattern.matches(
                Pattern.quote(baseName) + "(?:_(?:[2-9]|[1-9][0-9]+))?", name);
    }

    /** Mapping locations with supported naming patterns. */
    enum MappingContext {
        INBOUND_ATTRIBUTE, OUTBOUND_ATTRIBUTE, OBJECT_TEMPLATE, FOCUS, UNSUPPORTED
    }

    /**
     * Suggests a base name for an unnamed mapping.
     * Returns {@code null} if the mapping is already named, unsupported, or missing required paths.
     */
    static String suggestBaseName(PrismContainerValueWrapper<?> mapping) throws SchemaException {
        if (!(mapping.getRealValue() instanceof MappingType currentMapping)
                || StringUtils.isNotEmpty(currentMapping.getName())) {
            return null;
        }
        return computeBaseName(mapping);
    }

    /**
     * Builds a base name from the current source, target, and ref paths, ignoring the existing name.
     * Leaves the model unchanged and returns {@code null} for unsupported (e.g. association, auto-assignment mappings)
     * or incomplete mappings.
     */
    public static String computeBaseName(PrismContainerValueWrapper<?> mapping) throws SchemaException {
        if (!(mapping.getRealValue() instanceof MappingType realMapping)) {
            return null;
        }
        MappingType currentMapping = realMapping.clone();
        currentMapping.setName(null);
        return switch (resolveContext(mapping)) {
            case INBOUND_ATTRIBUTE -> suggestName(currentMapping, resolveItemRef(mapping), true);
            case OUTBOUND_ATTRIBUTE -> suggestName(currentMapping, resolveItemRef(mapping), false);
            case OBJECT_TEMPLATE, FOCUS -> suggestSourcesName(currentMapping);
            case UNSUPPORTED -> null;
        };
    }

    /**
     * Selects the naming pattern from the mapping type and its enclosing containers.
     * Association and auto-assignment mappings are not supported yet.
     */
    private static MappingContext resolveContext(PrismContainerValueWrapper<?> mapping) {
        if (!(mapping.getRealValue() instanceof MappingType currentMapping)
                || currentMapping instanceof AutoassignMappingType) {
            return MappingContext.UNSUPPORTED;
        }
        PrismContainerWrapper<?> container = mapping.getParent();
        var parent = container != null ? container.getParent() : null;
        if (parent == null) {
            return MappingContext.UNSUPPORTED;
        }
        if (currentMapping instanceof ObjectTemplateMappingType
                && (parent.getRealValue() instanceof ObjectTemplateType
                || parent.getRealValue() instanceof ObjectTemplateItemDefinitionType)) {
            return MappingContext.OBJECT_TEMPLATE;
        }
        if (parent.getRealValue() instanceof MappingsType) {
            PrismContainerWrapper<?> mappings = parent.getParent();
            if (mappings != null && AssignmentType.F_FOCUS_MAPPINGS.equivalent(mappings.getItemName())
                    && mappings.getParent() != null
                    && mappings.getParent().getRealValue() instanceof AssignmentType) {
                return MappingContext.FOCUS;
            }
        }
        if (parent.getRealValue() instanceof ResourceAttributeDefinitionType) {
            if (ResourceAttributeDefinitionType.F_INBOUND.equivalent(container.getItemName())) {
                return MappingContext.INBOUND_ATTRIBUTE;
            }
            if (ResourceAttributeDefinitionType.F_OUTBOUND.equivalent(container.getItemName())) {
                return MappingContext.OUTBOUND_ATTRIBUTE;
            }
        }
        return MappingContext.UNSUPPORTED;
    }

    /**
     * Reads the ref path through the wrapper so virtual refs on attribute mappings are included.
     * Returns {@code null} if the wrapper, property, or ref path is absent.
     */
    private static ItemPath resolveItemRef(PrismContainerValueWrapper<?> wrapper) throws SchemaException {
        if (wrapper == null) {
            return null;
        }
        var ref = wrapper.<ItemPathType>findProperty(ItemRefinedDefinitionType.F_REF);
        ItemPathType value = ref != null ? ref.getValue().getRealValue() : null;
        return value != null ? value.getItemPath() : null;
    }

    /**
     * Builds a {@code generate-target-from-source-source} name for an unnamed template or focus mapping.
     * Requires a target and at least one valid source path; otherwise returns {@code null}.
     */
    public static String suggestSourcesName(MappingType mapping) {
        if (StringUtils.isNotEmpty(mapping.getName())) {
            return null;
        }

        ItemPath targetPath = resolveTargetPath(mapping);
        if (targetPath == null) {
            return null;
        }

        String sources = collectSourceNames(mapping);
        return sources.isEmpty() ? null : SOURCES_PATTERN.formatted(targetPath, sources);
    }

    /**
     * Joins source paths with hyphens in their configured order, removing variable prefixes.
     * Skips missing bindings, missing paths, and paths without an item name.
     */
    private static String collectSourceNames(MappingType mapping) {
        return mapping.getSource().stream()
                .filter(Objects::nonNull)
                .map(VariableBindingDefinitionType::getPath)
                .filter(Objects::nonNull)
                .map(ItemPathType::getItemPath)
                .map(ItemPath::stripVariableSegment)
                .filter(path -> !path.isEmpty() && path.lastName() != null)
                .map(Object::toString)
                .collect(Collectors.joining("-"));
    }

    /** Keeps an unused base name or adds the first available suffix, starting with {@code _2}. */
    static String suggestUniqueName(String baseName, Predicate<String> isNameUsed) {

        String candidate = baseName;
        int suffix = 2;
        while (isNameUsed.test(candidate)) {
            candidate = baseName + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * Builds an inbound {@code ref-into-target} or outbound {@code source-source-to-ref} name.
     * Outbound names include all valid sources in their configured order.
     * Returns {@code null} if the mapping already has a name or required paths are missing.
     */
    public static String suggestName(MappingType mapping, ItemPath attributePath, boolean inbound) {

        if (StringUtils.isNotEmpty(mapping.getName()) || attributePath == null || attributePath.lastName() == null) {
            return null;
        }
        String attributeName = attributePath.lastName().getLocalPart();
        if (!inbound) {
            String sources = collectSourceNames(mapping);
            return sources.isEmpty() ? null : OUTBOUND_PATTERN.formatted(sources, attributeName);
        }
        ItemPath targetPath = resolveTargetPath(mapping);
        return targetPath != null ? INBOUND_PATTERN.formatted(attributeName, targetPath) : null;
    }

    /**
     * Reads the target path and removes its variable prefix.
     * Returns {@code null} if the target is missing or the remaining path has no item name.
     */
    private static ItemPath resolveTargetPath(MappingType mapping) {
        var target = mapping.getTarget();
        if (target == null || target.getPath() == null) {
            return null;
        }

        ItemPath path = target.getPath().getItemPath();
        path = path.stripVariableSegment();
        return !path.isEmpty() && path.lastName() != null ? path : null;
    }
}
