/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.Item;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for role analysis table tools and operations.
 * <p>
 * This class provides various utility methods for working with role analysis tables and related operations.
 */
public class RoleAnalysisTableTools {

    /**
     * Determine the background color class based on the density value.
     *
     * @param density The density value to determine the color for.
     * @return The CSS class representing the background color.
     */
    public static String densityBasedColor(double density) {

        if (density >= 60) {
            return "bg-success text-center";
        } else if (density > 30) {
            return "bg-info text-center";
        } else {
            return "bg-secondary text-center";
        }

    }

    /**
     * Generate a script for applying table scale adjustments.
     *
     * @return The JavaScript script for applying table scale adjustments.
     */
    public static String applyTableScaleScript() {
        return "MidPointTheme.initScaleResize('#tableScaleContainer');";
    }

    /**
     * Generate a script for applying image scale adjustments.
     *
     * @return The JavaScript script for applying image scale adjustments.
     */
    public static String applyImageScaleScript() {
        return "MidPointTheme.initScaleResize('#imageScaleContainer');";
    }

    /**
     * Apply square table cell styles to ensure a consistent appearance.
     *
     * @param cellItem The table cell item to which the styles should be applied.
     */
    public static void applySquareTableCell(@NotNull Item<?> cellItem) {
        MarkupContainer parentContainer = cellItem.getParent().getParent();
        parentContainer.add(AttributeAppender.replace("class", "d-flex"));
        parentContainer.add(AttributeAppender.replace("style", "height:40px"));

        cellItem.add(AttributeAppender.append("style", "width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

}
