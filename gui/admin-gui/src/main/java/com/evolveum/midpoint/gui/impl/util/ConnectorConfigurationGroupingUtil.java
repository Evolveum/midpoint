/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Groups container items (e.g. connector configuration properties) by the presentation
 * {@code group} annotation, so the GUI can render them in separate labeled sections.
 *
 * <p>For ConnId connectors the group annotation is populated from
 * {@code @ConfigurationProperty(groupMessageKey)} when midPoint generates the connector schema.</p>
 */
public final class ConnectorConfigurationGroupingUtil {

    private ConnectorConfigurationGroupingUtil() {
    }

    /**
     * A group of container items sharing the same {@code group} annotation value.
     */
    public static final class Group implements Serializable {

        @Serial private static final long serialVersionUID = 1L;

        private final String label;
        private final List<String> itemNames;

        public Group(String label, List<String> itemNames) {
            this.label = label;
            this.itemNames = List.copyOf(itemNames);
        }

        public String getLabel() {
            return label;
        }

        public List<String> getItemNames() {
            return itemNames;
        }

        public boolean contains(ItemWrapper<?, ?> itemWrapper) {
            return itemWrapper != null && itemNames.contains(itemWrapper.getItemName().getLocalPart());
        }
    }

    /**
     * Groups the items of the given container definition by the presentation {@code group}
     * annotation value, preserving the order in which the items appear.
     *
     * @return ordered list of groups, or {@code null} when no item has a group
     */
    public static List<Group> getConfigurationGroups(PrismContainerDefinition<?> containerDefinition) {
        if (containerDefinition == null) {
            return null;
        }

        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (ItemDefinition<?> itemDefinition : containerDefinition.getDefinitions()) {
            String group = itemDefinition.getExternalGroup();
            if (group == null || group.isEmpty()) {
                continue;
            }
            groups.computeIfAbsent(group, g -> new ArrayList<>()).add(itemDefinition.getItemName().getLocalPart());
        }

        if (groups.isEmpty()) {
            return null;
        }

        List<Group> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            result.add(new Group(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Identifier of the virtual container holding the ungrouped (main) items.
     */
    public static final String UNGROUPED_IDENTIFIER = "ungrouped";

    private static final String UNGROUPED_LABEL = "Configuration";

    /**
     * Builds a {@link ContainerPanelConfigurationType} that renders the items of the given container
     * (e.g. a connector configuration) as separate "virtual containers" (collapsible sections): one per
     * presentation {@code group} plus a trailing section for the ungrouped items.
     *
     * <p>Intended to be combined with {@code WrapperContext#forceCreateVirtualContainer}, so the GUI can
     * render the items as collapsible cards while keeping every item present (empty ones hidden by the
     * normal "show empty fields" behavior).</p>
     *
     * @param itemPathPrefix prefix of the generated item paths (path of the wrapped container, relative to
     *        the object root), so that the virtual container items can be resolved against the object;
     *        may be null, in which case bare item names are used
     */
    public static ContainerPanelConfigurationType createContainerPanelConfiguration(
            PrismContainerDefinition<?> containerDefinition, List<Group> groups, ItemPath itemPathPrefix) {
        Map<String, ItemDefinition<?>> byLocalPart = new LinkedHashMap<>();
        for (ItemDefinition<?> def : containerDefinition.getDefinitions()) {
            byLocalPart.putIfAbsent(def.getItemName().getLocalPart(), def);
        }

        Set<String> grouped = new LinkedHashSet<>();
        for (Group group : groups) {
            grouped.addAll(group.getItemNames());
        }

        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        for (Group group : groups) {
            VirtualContainersSpecificationType virtualContainer = new VirtualContainersSpecificationType()
                    .identifier(sanitizeId(group.getLabel()))
                    .display(GuiDisplayTypeUtil.createDisplayTypeWithLabel(group.getLabel(), null));
            for (String itemName : group.getItemNames()) {
                ItemDefinition<?> def = byLocalPart.get(itemName);
                if (def != null) {
                    virtualContainer.item(createItemSpec(def, itemPathPrefix));
                }
            }
            config.container(virtualContainer);
        }

        List<ItemDefinition<?>> ungrouped = new ArrayList<>();
        for (ItemDefinition<?> def : containerDefinition.getDefinitions()) {
            if (!grouped.contains(def.getItemName().getLocalPart())) {
                ungrouped.add(def);
            }
        }
        if (!ungrouped.isEmpty()) {
            VirtualContainersSpecificationType virtualContainer = new VirtualContainersSpecificationType()
                    .identifier(UNGROUPED_IDENTIFIER)
                    .display(GuiDisplayTypeUtil.createDisplayTypeWithLabel(UNGROUPED_LABEL));
            for (ItemDefinition<?> def : ungrouped) {
                virtualContainer.item(createItemSpec(def, itemPathPrefix));
            }
            config.container(virtualContainer);
        }

        return config;
    }

    private static VirtualContainerItemSpecificationType createItemSpec(ItemDefinition<?> def, ItemPath itemPathPrefix) {
        ItemPath path = createNamespaceAgnosticPath(def, itemPathPrefix);
        return new VirtualContainerItemSpecificationType()
                .path(new ItemPathType(path))
                .visibility(UserInterfaceElementVisibilityType.AUTOMATIC)
                .mandatory(def.isMandatory());
    }

    /**
     * Builds the virtual-item path using only the local parts of each segment (namespace-agnostic).
     *
     * <p>The fully-qualified segment names carried by the model (e.g. {@code connectorConfiguration}
     * in the {@code common-3} namespace vs. the connector-bundle namespace of the actual container)
     * do not always match, which would prevent the GUI from resolving the item when the virtual
     * containers are materialized at the object level. Building the path from bare local parts makes
     * resolution match by local name, which is what {@code findItem} does for namespace-less segments.</p>
     */
    private static ItemPath createNamespaceAgnosticPath(ItemDefinition<?> def, ItemPath itemPathPrefix) {
        List<String> segments = new ArrayList<>();
        if (itemPathPrefix != null) {
            ItemPath rest = itemPathPrefix;
            while (!rest.isEmpty()) {
                segments.add(ItemPath.toName(rest.first()).getLocalPart());
                rest = rest.rest();
            }
        }
        segments.add(def.getItemName().getLocalPart());
        return ItemPath.create(segments.toArray());
    }

    /**
     * Creates a visibility handler that shows only the items belonging to the given group,
     * or only the ungrouped items when the group is {@code null} (the main section).
     *
     * @param groups all known groups
     * @param group the group of the section, or {@code null} for the ungrouped items
     * @param base base visibility handler applied to the items of the section, may be null
     */
    public static ItemVisibilityHandler createVisibilityHandler(List<Group> groups, Group group, ItemVisibilityHandler base) {
        return itemWrapper -> {
            if (!inSection(groups, group, itemWrapper)) {
                return ItemVisibility.HIDDEN;
            }
            return base != null ? base.isVisible(itemWrapper) : ItemVisibility.AUTO;
        };
    }

    /**
     * Converts the given string into a string that can safely be used as a Wicket component ID.
     */
    public static String sanitizeId(String value) {
        return value == null ? "group" : value.replaceAll("[^a-zA-Z0-9]", "-");
    }

    /**
     * Checks whether the given section (group, or {@code null} for the ungrouped items)
     * contains at least one visible item.
     */
    public static boolean hasVisibleItems(List<Group> groups, Group group, PrismContainerValueWrapper<?> valueWrapper) {
        if (valueWrapper == null) {
            return false;
        }
        ItemVisibilityHandler visibilityHandler = createVisibilityHandler(groups, group, null);
        for (ItemWrapper<?, ?> item : valueWrapper.getItems()) {
            if (item.isVisible(valueWrapper, visibilityHandler)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inSection(List<Group> groups, Group group, ItemWrapper<?, ?> itemWrapper) {
        if (itemWrapper == null) {
            return false;
        }
        if (group != null) {
            return group.contains(itemWrapper);
        }
        for (Group g : groups) {
            if (g.contains(itemWrapper)) {
                return false;
            }
        }
        return true;
    }
}
