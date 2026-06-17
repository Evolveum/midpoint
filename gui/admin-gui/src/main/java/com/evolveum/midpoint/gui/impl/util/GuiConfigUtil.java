package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainerItemSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuiConfigUtil {

    /**
     * Finds an item specification in {@link ContainerPanelConfigurationType} that matches the provided item path.
     */
    public static VirtualContainerItemSpecificationType findItemSpecForPath(
            ContainerPanelConfigurationType config,
            ItemPath path) {

        if (config == null || path == null || config.getContainer() == null) {
            return null;
        }
        var namedSegmentsOnlyPath = path.namedSegmentsOnly();
        for (VirtualContainersSpecificationType container : config.getContainer()) {
            VirtualContainerItemSpecificationType found = findInContainerRecursive(container, namedSegmentsOnlyPath);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static VirtualContainerItemSpecificationType findInContainerRecursive(
            VirtualContainersSpecificationType container,
            ItemPath path) {

        if (container == null) {
            return null;
        }

        List<VirtualContainerItemSpecificationType> items = container.getItem();
        if (items != null) {
            for (VirtualContainerItemSpecificationType item : items) {
                if (item != null && item.getPath() != null) {
                    if (isPathEquivalent(path, item)) {
                        return item;
                    }
                }
            }
        }

        return null;
    }

    public static @Nullable PolyStringType extractLabelFromItemSpec(VirtualContainerItemSpecificationType item) {
        if (item == null || item.getDisplay() == null) {
            return null;
        }
        DisplayType display = item.getDisplay();
        return display.getLabel();
    }

    private static boolean isPathEquivalent(ItemPath path, @NotNull VirtualContainerItemSpecificationType item) {
        return item.getPath().getItemPath().equivalent(path);
    }
}
