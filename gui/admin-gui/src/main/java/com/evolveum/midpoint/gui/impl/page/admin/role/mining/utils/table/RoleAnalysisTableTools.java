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
            return "function initScaleResize() {\n" +
                    "    let containerId = '#tableScaleContainer';\n" +
                    "    let div = document.querySelector(containerId);\n" +
                    "    let scale = 0.5;\n" +
                    "    let component = null;\n" +
                    "\n" +
                    "    if (!div) {\n" +
                    "        console.error('Container not found');\n" +
                    "        return;\n" +
                    "    }\n" +
                    "\n" +
                    "    if (containerId === '#tableScaleContainer') {\n" +
                    "        component = div.querySelector('table');\n" +
                    "    } else if (containerId === '#imageScaleContainer') {\n" +
                    "        component = div.querySelector('img');\n" +
                    "    } else if (containerId === '#chartScaleContainer') {\n" +
                    "        component = div.querySelector('canvas');\n" +
                    "    }\n" +
                    "\n" +
                    "    if (component) {\n" +
                    "        div.addEventListener('wheel', handleZoom);\n" +
                    "    } else {\n" +
                    "        console.error('Component not found');\n" +
                    "    }\n" +
                    "\n" +
                    "    function handleZoom(e) {\n" +
                    "        e.preventDefault();\n" +
                    "        let rectBefore = component.getBoundingClientRect();\n" +
                    "        let x = (e.clientX - rectBefore.left) / rectBefore.width * 100;\n" +
                    "        let y = (e.clientY - rectBefore.top) / rectBefore.height * 100;\n" +
                    "\n" +
                    "        if (e.deltaY < 0) {\n" +
                    "            zoomIn(rectBefore, x, y);\n" +
                    "        } else if (e.deltaY > 0) {\n" +
                    "            zoomOut(rectBefore);\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    function zoomIn(rectBefore, x, y) {\n" +
                    "        console.log('Zooming in');\n" +
                    "        scale += 0.01;\n" +
                    "\n" +
                    "        let prevScale = scale - 0.01;\n" +
                    "        let scaleFactor = scale / prevScale;\n" +
                    "\n" +
                    "        if (scale > 1) {\n" +
                    "            let deltaX = (x / 100) * rectBefore.width * (scaleFactor - 1);\n" +
                    "            let deltaY = (y / 100) * rectBefore.height * (scaleFactor - 1);\n" +
                    "            setTransform(x, y, scale, rectBefore, deltaX, deltaY, scaleFactor);\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    function zoomOut(rectBefore, x, y) {\n" +
                    "        console.log('Zooming out');\n" +
                    "        scale -= 0.01;\n" +
                    "        scale = Math.max(0.1, scale);\n" +
                    "\n" +
                    "        setTransform(0, 0, scale, rectBefore, 0, 0, 1);\n" +
                    "    }\n" +
                    "\n" +
                    "    function setTransform(x, y, scale, rectBefore, deltaX, deltaY, scaleFactor) {\n" +
                    "        component.style.transformOrigin = `${x}% ${y}%`;\n" +
                    "        component.style.transition = 'transform 0.3s';\n" +
                    "        component.style.transform = `scale(${scale})`;\n" +
                    "\n" +
                    "        let rectAfter = component.getBoundingClientRect();\n" +
                    "        div.scrollLeft += (rectAfter.left - rectBefore.left) + deltaX - (e.clientX - rectBefore.left) * (scaleFactor - 1);\n" +
                    "        div.scrollTop += (rectAfter.top - rectBefore.top) + deltaY - (e.clientY - rectBefore.top) * (scaleFactor - 1);\n" +
                    "    }\n" +
                    "}";

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
    public static void applySquareTableCell(@NotNull Item<?> cellItem, StyleResolution headerWidth, StyleResolution headerHeight) {
        MarkupContainer parentContainer = cellItem.getParent().getParent();
        parentContainer.add(AttributeAppender.replace("class", "d-flex"));
        parentContainer.add(AttributeAppender.replace("style", "height:" + headerHeight.getSizeInPixels()));

        cellItem.add(AttributeAppender.append("style",
                "width:" + headerWidth.getSizeInPixels()
                        + "; height:" + headerHeight.getSizeInPixels()
                        + "; border: 1px solid #f4f4f4;"));

        cellItem.add(AttributeAppender.replace("class", "p-2 d-flex align-items-center justify-content-center"));
    }

    public static void applySquareTableCell(@NotNull Item<?> cellItem) {
        MarkupContainer parentContainer = cellItem.getParent().getParent();
        parentContainer.add(AttributeAppender.replace("class", "d-flex"));
        parentContainer.add(AttributeAppender.replace("style", "height:40px"));

        cellItem.add(AttributeAppender.append("style", "width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    public enum StyleResolution {
        LEVEL_1("40px"),
        LEVEL_2("55px"),
        LEVEL_3("70px"),
        LEVEL_4("85px"),
        LEVEL_5("100px");

        private final String sizeInPixels;

        StyleResolution(String sizeInPixels) {
            this.sizeInPixels = sizeInPixels;
        }

        public static @NotNull String resolveSizeLevel(@NotNull StyleResolution styleResolution) {
            if (styleResolution.equals(LEVEL_2)) {
                return "level-2";
            } else if (styleResolution.equals(LEVEL_3)) {
                return "level-3";
            } else if (styleResolution.equals(LEVEL_4)) {
                return "level-4";
            } else if (styleResolution.equals(LEVEL_5)) {
                return "level-5";
            }
            return "level-1";
        }

        public static StyleResolution resolveSize(int objectCount) {
            if (objectCount > 1 && objectCount <= 10) {
                return StyleResolution.LEVEL_2;
            } else if (objectCount > 10 && objectCount <= 50) {
                return StyleResolution.LEVEL_3;
            } else if (objectCount > 50 && objectCount <= 100) {
                return StyleResolution.LEVEL_4;
            } else if (objectCount > 100) {
                return StyleResolution.LEVEL_5;
            }
            return LEVEL_1;
        }

        public String getSizeInPixels() {
            return sizeInPixels;
        }

        public static StyleResolution getLevelByWidth(String sizeInPixels) {
            for (StyleResolution level : values()) {
                if (level.sizeInPixels.equals(sizeInPixels)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("No such level with size: " + sizeInPixels);
        }
    }

}
