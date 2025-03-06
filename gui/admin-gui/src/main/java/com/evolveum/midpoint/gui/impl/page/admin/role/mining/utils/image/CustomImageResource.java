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
import java.io.Serializable;
import java.util.List;
import javax.imageio.ImageIO;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.request.resource.DynamicImageResource;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * CustomImageResource generates images for role mining clusters based on a MiningOperationChunk.
 */
public class CustomImageResource extends DynamicImageResource implements Serializable {

    int width;
    int height;
    MiningOperationChunk miningOperationChunk;
    boolean isRoleMode;

    public static final Trace LOGGER = TraceManager.getTrace(CustomImageResource.class);

    public CustomImageResource(
            @NotNull PageBase pageBase,
            @NotNull RoleAnalysisClusterType cluster,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        PrismObject<RoleAnalysisSessionType> sessionPrismObject = roleAnalysisService
                .getSessionTypeObject(cluster.getRoleAnalysisSessionRef().getOid(), task, result);
        if (sessionPrismObject == null) {
            LOGGER.error("Couldn't find session object with oid {}", cluster.getRoleAnalysisSessionRef().getOid());
            return;
        }

        RoleAnalysisSessionType session = sessionPrismObject.asObjectable();

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        this.isRoleMode = RoleAnalysisProcessModeType.ROLE.equals(processMode);

        AbstractAnalysisSessionOptionType sessionOptions = processMode.equals(RoleAnalysisProcessModeType.ROLE) ?
                session.getRoleModeOptions() : session.getUserModeOptions();

        SearchFilterType userSearchFilter = sessionOptions.getUserSearchFilter();
        SearchFilterType roleSearchFilter = sessionOptions.getRoleSearchFilter();
        SearchFilterType assignmentSearchFilter = sessionOptions.getAssignmentSearchFilter();

        this.miningOperationChunk = roleAnalysisService.prepareExpandedMiningStructure(cluster,
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                true, processMode, result, task, null);
    }

    public String getColumnTitle() {
        return isRoleMode ? "CustomImageResource.title.roles" : "CustomImageResource.title.users";
    }

    public String getRowTitle() {
        return isRoleMode ? "CustomImageResource.title.users" : "CustomImageResource.title.roles";
    }

    public String getColumnIcon() {
        return isRoleMode ? GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED : GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED;
    }

    public String getRowIcon() {
        return isRoleMode ? GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED : GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED;
    }

    public CustomImageResource(MiningOperationChunk miningOperationChunk, @NotNull RoleAnalysisProcessModeType mode) {
        this.miningOperationChunk = miningOperationChunk;
        this.isRoleMode = mode.equals(RoleAnalysisProcessModeType.ROLE);
    }

    @Override
    protected byte[] getImageData(Attributes attributes) {

        BufferedImage image;
        Graphics2D graphics;

        if (isRoleMode) {
            List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                    RoleAnalysisSortMode.NONE);
            List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                    RoleAnalysisSortMode.JACCARD);
            width = miningRoleTypeChunks.size();
            height = miningUserTypeChunks.size();
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            graphics = image.createGraphics();

            fillGraphic(miningRoleTypeChunks, miningUserTypeChunks, graphics);

        } else {
            List<MiningRoleTypeChunk> miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(
                    RoleAnalysisSortMode.JACCARD);
            List<MiningUserTypeChunk> miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(
                    RoleAnalysisSortMode.NONE);
            width = miningUserTypeChunks.size();
            height = miningRoleTypeChunks.size();
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            graphics = image.createGraphics();

            fillGraphic(miningUserTypeChunks, miningRoleTypeChunks, graphics);
        }

        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            LOGGER.error("Couldn't write image to output stream.");
        }

        return outputStream.toByteArray();
    }

    private static void fillGraphic(
            @NotNull List<? extends MiningBaseTypeChunk> memberChunks,
            @NotNull List<? extends MiningBaseTypeChunk> propertiesChunk,
            @NotNull Graphics2D graphics) {
        for (int x = 0; x < memberChunks.size(); x++) {
            String point = memberChunks.get(x).getMembers().get(0);
            for (int y = 0; y < propertiesChunk.size(); y++) {
                if (propertiesChunk.get(y).getProperties().contains(point)) {
                    graphics.setColor(Color.BLACK);
                } else {
                    graphics.setColor(Color.WHITE);
                }

                graphics.fillRect(x, y, 1, 1);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    protected void setResponseHeaders(ResourceResponse response, Attributes attributes) {
        super.setResponseHeaders(response, attributes);
        response.setFileName("image.png");
    }

}
