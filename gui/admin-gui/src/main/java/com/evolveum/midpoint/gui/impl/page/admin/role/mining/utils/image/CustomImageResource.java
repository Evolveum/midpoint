/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;

import org.apache.wicket.request.resource.DynamicImageResource;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

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
    RoleAnalysisProcessModeType mode;

    public CustomImageResource(MiningOperationChunk miningOperationChunk, RoleAnalysisProcessModeType mode) {
        this.miningOperationChunk = miningOperationChunk;
        this.mode = mode;
    }

    @Override
    protected byte[] getImageData(Attributes attributes) {

        BufferedImage image;
        Graphics2D graphics;

        if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
            List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);
            List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.JACCARD);
            width = miningRoleTypeChunks.size();
            height = miningUserTypeChunks.size();
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            graphics = image.createGraphics();

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
        } else {
            List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.JACCARD);
            List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
            width = miningUserTypeChunks.size();
            height = miningRoleTypeChunks.size();
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            graphics = image.createGraphics();

            for (int x = 0; x < miningUserTypeChunks.size(); x++) {
                String point = miningUserTypeChunks.get(x).getUsers().get(0);
                for (int y = 0; y < miningRoleTypeChunks.size(); y++) {
                    if (miningRoleTypeChunks.get(y).getUsers().contains(point)) {
                        graphics.setColor(Color.BLACK);
                    } else {
                        graphics.setColor(Color.WHITE);
                    }

                    graphics.fillRect(x, y, 1, 1);
                }
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
