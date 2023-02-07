/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.executor;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemChangeApplicationModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.schema.util.ItemRefinedDefinitionTypeUtil.*;

public class ItemChangeApplicationModeConfiguration {

    /** Modes for individual items. We do not support item overlapping (e.g. activation vs activation/administrativeStatus)! */
    @NotNull private final PathKeyedMap<ItemChangeApplicationModeType> modeMap;

    /** Items to be reported on. */
    @NotNull private final PathSet reportOnlyItems = new PathSet();

    /** Items to be ignored. */
    @NotNull private final PathSet ignoredItems = new PathSet();

    /** Items that are NOT to be touched (i.e. reported or ignored). */
    @NotNull private final PathSet noApplicationItems = new PathSet();

    private ItemChangeApplicationModeConfiguration(@NotNull PathKeyedMap<ItemChangeApplicationModeType> modeMap) {
        this.modeMap = modeMap;
        for (Map.Entry<ItemPath, ItemChangeApplicationModeType> entry : modeMap.entrySet()) {
            switch (entry.getValue()) {
                case REPORT:
                    reportOnlyItems.add(entry.getKey());
                    noApplicationItems.add(entry.getKey());
                    break;
                case IGNORE:
                    ignoredItems.add(entry.getKey());
                    noApplicationItems.add(entry.getKey());
            }
        }
    }

    public static ItemChangeApplicationModeConfiguration of(ObjectTemplateType template) throws ConfigurationException {
        PathKeyedMap<ItemChangeApplicationModeType> modeMap = new PathKeyedMap<>();
        if (template != null) {
            for (ObjectTemplateItemDefinitionType itemDef : template.getItem()) {
                ItemChangeApplicationModeType mode = itemDef.getChangeApplicationMode();
                if (mode != null) {
                    modeMap.put(getRef(itemDef), mode);
                }
            }
        }
        return new ItemChangeApplicationModeConfiguration(modeMap);
    }

    public static ItemChangeApplicationModeConfiguration of(ResourceObjectDefinition objectDefinition) {
        PathKeyedMap<ItemChangeApplicationModeType> modeMap = new PathKeyedMap<>();
        if (objectDefinition != null) {
            for (ResourceAttributeDefinition<?> attrDef : objectDefinition.getAttributeDefinitions()) {
                ItemChangeApplicationModeType mode = attrDef.getChangeApplicationMode();
                if (mode != null) {
                    modeMap.put(ShadowType.F_ATTRIBUTES.append(attrDef.getItemName()), mode);
                }
            }
        }
        return new ItemChangeApplicationModeConfiguration(modeMap);
    }

    @NotNull ItemChangeApplicationModeType getMode(@NotNull ItemPath itemPath) {
        return Objects.requireNonNullElse(
                modeMap.get(itemPath),
                ItemChangeApplicationModeType.APPLY);
    }

    @NotNull PathSet getReportOnlyItems() {
        return reportOnlyItems;
    }

    @NotNull PathSet getIgnoredItems() {
        return ignoredItems;
    }

    boolean isAllToApply() {
        return noApplicationItems.isEmpty();
    }
}
