/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisObjectDto implements Serializable {

    private static final String DOT_CLASS = RoleAnalysisObjectDto.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    public static final String F_DISPLAY_VALUE_OPTION = "displayValueOption";

    //TODO maybe cache the whole cluster?
    private MiningOperationChunk miiningOperationChunk;
    private boolean isRoleMode;
    private boolean isOutlierDetection;
    private DisplayValueOption displayValueOption;

    private List<RoleAnalysisAttributeDef> userAnalysisAttributes;
    private List<RoleAnalysisAttributeDef> roleAnalysisAttributes;

    private RoleAnalysisClusterType cluster;

    public RoleAnalysisObjectDto(RoleAnalysisClusterType cluster, List<DetectedPattern> detectedPatterns, Integer parameterTableSetting, PageBase pageBase) {
        loadObject(cluster, detectedPatterns, parameterTableSetting, pageBase);
    }

    private RoleAnalysisObjectDto loadObject(RoleAnalysisClusterType cluster, List<DetectedPattern> detectedPatterns, Integer parameterTableSetting, PageBase pageBase) {
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask(OP_PREPARE_OBJECTS);
        OperationResult result = task.getResult();

        this.cluster = cluster;

        RoleAnalysisProcessModeType mode;
        PrismObject<RoleAnalysisSessionType> getParent = roleAnalysisService.
                getSessionTypeObject(cluster.getRoleAnalysisSessionRef().getOid(), task, result);
        if (getParent != null) {
            RoleAnalysisSessionType session = getParent.asObjectable();
            RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
            RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
            if (RoleAnalysisCategoryType.OUTLIERS.equals(analysisCategory)) {
                this.isOutlierDetection = true;
            }
            mode = analysisOption.getProcessMode();
            this.isRoleMode = mode.equals(RoleAnalysisProcessModeType.ROLE);

            userAnalysisAttributes = roleAnalysisService
                    .resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
            roleAnalysisAttributes = roleAnalysisService
                    .resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);

        }

        this.displayValueOption = loadDispayValueOption(cluster, parameterTableSetting);

        //chunk mode
        this.miiningOperationChunk = roleAnalysisService.prepareMiningStructure(
                cluster,
                displayValueOption,
                isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER,
                detectedPatterns,
                result, task);

        return this;

    }

    private DisplayValueOption loadDispayValueOption(RoleAnalysisClusterType cluster, Integer parameterTableSetting) {
//        RoleAnalysisClusterType cluster = getModelObject().asObjectable();
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        DisplayValueOption displayValueOption = new DisplayValueOption();
        displayValueOption.setChunkMode(RoleAnalysisChunkMode.COMPRESS);

        displayValueOption.setProcessMode(isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER);

//        Integer parameterTableSetting = getParameterTableSetting();
        if (parameterTableSetting != null && parameterTableSetting == 1) {
            displayValueOption.setFullPage(true);
        }

        Integer rolesCount = clusterStatistics.getRolesCount();
        Integer usersCount = clusterStatistics.getUsersCount();

        if (rolesCount == null || usersCount == null) {
            displayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
        } else {

            int maxRoles;
            int maxUsers;

            if (isRoleMode) {
                maxRoles = 20;
                maxUsers = 13;
            } else {
                maxRoles = 13;
                maxUsers = 20;
            }
            int max = Math.max(rolesCount, usersCount);

            if (max <= 500) {
                displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
            } else {
                displayValueOption.setSortMode(RoleAnalysisSortMode.FREQUENCY);
            }

            if (rolesCount > maxRoles && usersCount > maxUsers) {
                displayValueOption.setChunkMode(RoleAnalysisChunkMode.COMPRESS);
            } else if (rolesCount > maxRoles) {
                displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND_USER);
            } else if (usersCount > maxUsers) {
                displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND_ROLE);
            } else {
                displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
            }
        }

        return displayValueOption;
    }

    public <B extends MiningBaseTypeChunk> List<B> getMainMiningChunk() {
        return miiningOperationChunk.getMainMiningChunk();
    }

    public <A extends MiningBaseTypeChunk> List<A> getAdditionalMiningChunk() {
        return miiningOperationChunk.getAdditionalMiningChunk();
    }

    public double getMinFrequency() {
        return miiningOperationChunk.getMinFrequency();
    }

    public double getMaxFrequency() {
        return miiningOperationChunk.getMaxFrequency();
    }

    public RoleAnalysisSortMode getSortMode() {
        return displayValueOption.getSortMode();
    }

    public String getChunkModeValue() {
        return displayValueOption.getChunkMode().getValue();
    }

    public RoleAnalysisChunkAction getChunkAction() {
        return displayValueOption.getChunkAction();
    }

    public boolean isOutlierDetection() {
        return isOutlierDetection;
    }

    public void recomputeChunks() {

    }

    public MiningOperationChunk getMininingOperationChunk() {
        return miiningOperationChunk;
    }

    public List<RoleAnalysisAttributeDef> getRoleAnalysisAttributes() {
        return roleAnalysisAttributes;
    }

    public List<RoleAnalysisAttributeDef> getUserAnalysisAttributes() {
        return userAnalysisAttributes;
    }

    public DisplayValueOption getDisplayValueOption() {
        return displayValueOption;
    }

    public List<ObjectReferenceType> getResolvedPattern() {
        return cluster.getResolvedPattern();
    }

    public RoleAnalysisClusterType getCluster() {
        return cluster;
    }
}
