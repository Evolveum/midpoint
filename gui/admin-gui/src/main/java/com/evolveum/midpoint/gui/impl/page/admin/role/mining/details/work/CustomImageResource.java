/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningOperationChunk;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;

import org.apache.wicket.request.resource.DynamicImageResource;

public class CustomImageResource extends DynamicImageResource {

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    int width;
    int height;
    MiningOperationChunk miningOperationChunk;
    String mode;

    public CustomImageResource(MiningOperationChunk miningOperationChunk, String mode) {
        this.miningOperationChunk = miningOperationChunk;
        this.mode = mode;
    }

    @Override
    protected byte[] getImageData(Attributes attributes) {

        List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks();
        List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks();
        width = miningRoleTypeChunks.size();
        height = miningUserTypeChunks.size();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        for (int x = 0; x < miningRoleTypeChunks.size(); x++) {
            String point = miningRoleTypeChunks.get(x).getRoles().get(0);
            for (int y = 0; y < miningUserTypeChunks.size(); y++) {
                if (miningUserTypeChunks.get(y).getRoles().contains(point)) {
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
