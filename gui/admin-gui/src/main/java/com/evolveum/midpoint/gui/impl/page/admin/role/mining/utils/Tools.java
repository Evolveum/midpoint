/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.jetbrains.annotations.NotNull;

public class Tools {

    public static String getColorClass(String density) {
        double densityValue = Double.parseDouble(density);

        if (densityValue >= 60) {
            return "bg-success text-center";
        } else if (densityValue > 30) {
            return "bg-info text-center";
        } else {
            return "bg-secondary text-center";
        }

    }

    public static long startTimer(String info) {
        System.out.println("Operation start: " + info);
        return System.currentTimeMillis();
    }

    public static void endTimer(long startTime, String info) {
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        double elapsedSeconds = elapsedTime / 1000.0;
        System.out.println("Operation end: " + info + ". Elapsed time: " + elapsedSeconds + " seconds.");
    }

    public static String getScaleScript() {
        return "let div = document.querySelector('#myTable');" +
                "let table = div.querySelector('table');" +
                "let scale = 0.5;" +
                "if (div && table) {" +
                "  div.onwheel = function(e) {" +
                "    e.preventDefault();" +
                "    let rectBefore = table.getBoundingClientRect();" +
                "    let x = (e.clientX - rectBefore.left) / rectBefore.width * 100;" +
                "    let y = (e.clientY - rectBefore.top) / rectBefore.height * 100;" +
                "    table.style.transformOrigin = 'left top';" +
                "    if (e.deltaY < 0) {" +
                "      console.log('Zooming in');" +
                "      scale += 0.03;" +
                "      let prevScale = scale - 0.1;" +
                "      let scaleFactor = scale / prevScale;" +
                "      let deltaX = (x / 100) * rectBefore.width * (scaleFactor - 1);" +
                "      let deltaY = (y / 100) * rectBefore.height * (scaleFactor - 1);" +
                "      table.style.transformOrigin = x + '%' + ' ' + y + '%';" +
                "      table.style.transition = 'transform 0.3s';" + // Add transition property
                "      table.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = table.getBoundingClientRect();" +
                "      div.scrollLeft += (rectAfter.left - rectBefore.left) + deltaX - (e.clientX - rectBefore.left) * (scaleFactor - 1);" +
                "      div.scrollTop += (rectAfter.top - rectBefore.top) + deltaY - (e.clientY - rectBefore.top) * (scaleFactor - 1);" +
                "    } else if (e.deltaY > 0) {" +
                "      console.log('Zooming out');" +
                "      scale -= 0.03;" +
                "      scale = Math.max(0.1, scale);" +
                "      table.style.transition = 'transform 0.3s';" + // Add transition property
                "      table.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = table.getBoundingClientRect();" +
                "      div.scrollLeft += (rectAfter.left - rectBefore.left);" +
                "      div.scrollTop += (rectAfter.top - rectBefore.top);" +
                "    }" +
                "  };" +
                "} else {" +
                "  console.error('Div or table not found');" +
                "}";
    }

    public static String getImageScaleScript() {
        return "let imageContainer = document.querySelector('#imageContainer');" +
                "let image = imageContainer.querySelector('img');" +
                "let scale = 1;" +
                "if (imageContainer && image) {" +
                "  imageContainer.onwheel = function(e) {" +
                "    e.preventDefault();" +
                "    let rectBefore = image.getBoundingClientRect();" +
                "    let x = (e.clientX - rectBefore.left) / rectBefore.width * 100;" +
                "    let y = (e.clientY - rectBefore.top) / rectBefore.height * 100;" +
                "    image.style.transformOrigin = 'left top';" +
                "    if (e.deltaY < 0) {" +
                "      console.log('Zooming in');" +
                "      scale += 0.03;" +
                "      let prevScale = scale - 0.1;" +
                "      let scaleFactor = scale / prevScale;" +
                "      let deltaX = (x / 100) * rectBefore.width * (scaleFactor - 1);" +
                "      let deltaY = (y / 100) * rectBefore.height * (scaleFactor - 1);" +
                "      image.style.transformOrigin = x + '%' + ' ' + y + '%';" +
                "      image.style.transition = 'transform 0.3s';" +
                "      image.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = image.getBoundingClientRect();" +
                "      imageContainer.scrollLeft += (rectAfter.left - rectBefore.left) + deltaX - (e.clientX - rectBefore.left) * (scaleFactor - 1);" +
                "      imageContainer.scrollTop += (rectAfter.top - rectBefore.top) + deltaY - (e.clientY - rectBefore.top) * (scaleFactor - 1);" +
                "    } else if (e.deltaY > 0) {" +
                "      console.log('Zooming out');" +
                "      scale -= 0.03;" +
                "      scale = Math.max(0.1, scale);" +
                "      image.style.transition = 'transform 0.3s';" +
                "      image.style.transform = 'scale(' + scale + ')';" +
                "      let rectAfter = image.getBoundingClientRect();" +
                "      imageContainer.scrollLeft += (rectAfter.left - rectBefore.left);" +
                "      imageContainer.scrollTop += (rectAfter.top - rectBefore.top);" +
                "    }" +
                "  };" +
                "} else {" +
                "  console.error('Image or container not found');" +
                "}";
    }

    public static AttributeAppender scaleModifier() {
        return AttributeModifier.append("style", "transform: scale(0.5); transform-origin: 0 0;");
    }

    public static void tableStyle(@NotNull Item<?> cellItem) {
        MarkupContainer parentContainer = cellItem.getParent().getParent();
        parentContainer.add(AttributeAppender.replace("class", "d-flex"));
        parentContainer.add(AttributeAppender.replace("style", "height:40px"));

        cellItem.add(AttributeAppender.append("style", "width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }

    public static void emptyCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new EmptyPanel(componentId));
    }

    public static void filledCell(@NotNull Item<?> cellItem, String componentId, String color) {
        cellItem.add(new AttributeAppender("class", color));
        cellItem.add(new EmptyPanel(componentId));
    }
}
