/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.wicket.request.resource.DynamicImageResource;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.ClusteringObjectMapped;

public class CustomImageResource extends DynamicImageResource {

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    int width;
    int height;
    List<ClusteringObjectMapped> clusteringObjectMapped;
    List<String> clusterPoints;

    public CustomImageResource(List<ClusteringObjectMapped> clusteringObjectMapped, List<String> clusterPoints) {
        this.clusteringObjectMapped = clusteringObjectMapped;
        this.clusterPoints = clusterPoints;
    }

    @Override
    protected byte[] getImageData(Attributes attributes) {
        width = clusterPoints.size();
        height = clusteringObjectMapped.size();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        for (int x = 0; x < clusterPoints.size(); x++) {
            String point = clusterPoints.get(x);
            for (int y = 0; y < clusteringObjectMapped.size(); y++) {
                if (clusteringObjectMapped.get(y).getPoints().contains(point)) {
                    graphics.setColor(Color.BLACK);
                } else {
                    graphics.setColor(Color.WHITE);
                }

                graphics.fillRect(x, y, 1, 1);
            }
        }

        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    @Override
    protected void setResponseHeaders(ResourceResponse response, Attributes attributes) {
        super.setResponseHeaders(response, attributes);
        response.setFileName("image.png");
    }

}
