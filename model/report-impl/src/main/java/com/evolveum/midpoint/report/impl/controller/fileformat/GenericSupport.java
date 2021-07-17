/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import org.apache.commons.lang3.StringUtils;

/**
 * Generic support methods, useful in all contexts (e.g. both export and import of reports).
 *
 * TODO better name
 */
class GenericSupport {

    static String getColumnLabel(GuiObjectColumnType column, PrismContainerDefinition objectDefinition,
            LocalizationService localizationService) {
        ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();

        DisplayType columnDisplay = column.getDisplay();
        String label;
        if (columnDisplay != null && columnDisplay.getLabel() != null) {
            label = FileFormatController.getMessage(localizationService, columnDisplay.getLabel().getOrig());
        } else {

            String name = column.getName();
            String displayName = null;
            if (path != null) {
                ItemDefinition def = objectDefinition.findItemDefinition(path);
                if (def == null) {
                    throw new IllegalArgumentException("Could'n find item for path " + path);
                }
                displayName = def.getDisplayName();

            }
            if (StringUtils.isNotEmpty(displayName)) {
                label = FileFormatController.getMessage(localizationService, displayName);
            } else {
                label = name;
            }
        }
        return label;
    }
}
