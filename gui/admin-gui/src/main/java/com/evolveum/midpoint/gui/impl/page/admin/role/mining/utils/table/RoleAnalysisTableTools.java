/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.jetbrains.annotations.NotNull;

public class RoleAnalysisTableTools {

    public static String densityBasedColor(String density) {
        double densityValue = Double.parseDouble(density);

        if (densityValue >= 60) {
            return "bg-success text-center";
        } else if (densityValue > 30) {
            return "bg-info text-center";
        } else {
            return "bg-secondary text-center";
        }

    }

    public static String applyTableScaleScript() {
        return "MidPointTheme.initScaleResize('#tableScaleContainer');";
    }

    public static String applyImageScaleScript() {
        return "MidPointTheme.initScaleResize('#imageScaleContainer');";
    }

    public static void applySquareTableCell(@NotNull Item<?> cellItem) {
        MarkupContainer parentContainer = cellItem.getParent().getParent();
        parentContainer.add(AttributeAppender.replace("class", "d-flex"));
        parentContainer.add(AttributeAppender.replace("style", "height:40px"));

        cellItem.add(AttributeAppender.append("style", "width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    public static void emptyPanelCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new EmptyPanel(componentId));
    }

    public static void filledCell(@NotNull Item<?> cellItem, String componentId, String color) {
        cellItem.add(new AttributeAppender("class", color));
        cellItem.add(new EmptyPanel(componentId));
    }
}
