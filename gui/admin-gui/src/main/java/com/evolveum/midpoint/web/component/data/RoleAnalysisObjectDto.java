/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.*;
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

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.refreshCells;

public class RoleAnalysisObjectDto implements Serializable {

    private static final String DOT_CLASS = RoleAnalysisObjectDto.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    public static final String F_DISPLAY_VALUE_OPTION = "displayValueOption";

    private MiningOperationChunk miningOperationChunk;
    private boolean isRoleMode;
    private boolean isOutlierDetection;
    private DisplayValueOption displayValueOption;

    private List<RoleAnalysisAttributeDef> userAnalysisAttributes;
    private List<RoleAnalysisAttributeDef> roleAnalysisAttributes;

    private RoleAnalysisClusterType cluster;

    private Set<String> markedUsers = new HashSet<>();

    private SearchFilterType userSearchFilter = null;
    private SearchFilterType roleSearchFilter = null;
    private SearchFilterType assignmentSearchFilter = null;

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
            RoleAnalysisProcedureType procedureType = analysisOption.getAnalysisProcedureType();
            if (procedureType == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
                this.isOutlierDetection = true;
                collectMarkedUsers(roleAnalysisService, task, result);
            }
            mode = analysisOption.getProcessMode();
            this.isRoleMode = mode.equals(RoleAnalysisProcessModeType.ROLE);

            userAnalysisAttributes = roleAnalysisService
                    .resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
            roleAnalysisAttributes = roleAnalysisService
                    .resolveAnalysisAttributes(session, RoleType.COMPLEX_TYPE);
            if (mode.equals(RoleAnalysisProcessModeType.ROLE)) {
                RoleAnalysisSessionOptionType roleModeOptions = session.getRoleModeOptions();
                if (roleModeOptions != null) {
                    this.userSearchFilter = roleModeOptions.getUserSearchFilter();
                    this.roleSearchFilter = roleModeOptions.getRoleSearchFilter();
                    this.assignmentSearchFilter = roleModeOptions.getAssignmentSearchFilter();
                }
            } else if (mode.equals(RoleAnalysisProcessModeType.USER)) {
                UserAnalysisSessionOptionType userModeOptions = session.getUserModeOptions();
                if (userModeOptions != null) {
                    this.userSearchFilter = userModeOptions.getUserSearchFilter();
                    this.roleSearchFilter = userModeOptions.getRoleSearchFilter();
                    this.assignmentSearchFilter = userModeOptions.getAssignmentSearchFilter();
                }
            }

        }

        this.displayValueOption = loadDisplayValueOption(cluster, parameterTableSetting);

        //chunk mode
        this.miningOperationChunk = roleAnalysisService.prepareMiningStructure(
                cluster,
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                displayValueOption,
                isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER,
                detectedPatterns,
                result, task);

        return this;

    }

    private void collectMarkedUsers(RoleAnalysisService roleAnalysisService, Task task, OperationResult result) {
        markedUsers = new HashSet<>();
        List<RoleAnalysisOutlierType> searchResultList = roleAnalysisService.findClusterOutliers(
                cluster, null, task, result);
        if (searchResultList != null && !searchResultList.isEmpty()) {
            for (RoleAnalysisOutlierType outlier : searchResultList) {
                ObjectReferenceType targetObjectRef = outlier.getTargetObjectRef();
                if (targetObjectRef != null && targetObjectRef.getOid() != null) {
                    markedUsers.add(targetObjectRef.getOid());
                }
            }
        }
    }

    private @NotNull DisplayValueOption loadDisplayValueOption(@NotNull RoleAnalysisClusterType cluster, Integer parameterTableSetting) {
        AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();

        DisplayValueOption chunkDisplayValueOption = new DisplayValueOption();
        chunkDisplayValueOption.setChunkMode(RoleAnalysisChunkMode.COMPRESS);

        chunkDisplayValueOption.setProcessMode(isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER);

//        Integer parameterTableSetting = getParameterTableSetting();
        if (parameterTableSetting != null && parameterTableSetting == 1) {
            chunkDisplayValueOption.setFullPage(true);
        }

        Integer rolesCount = clusterStatistics.getRolesCount();
        Integer usersCount = clusterStatistics.getUsersCount();

        if (rolesCount == null || usersCount == null) {
            chunkDisplayValueOption.setSortMode(RoleAnalysisSortMode.NONE);
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

            resolveDefaultSortMode(max, chunkDisplayValueOption);

            resolveDefaultChunkStructure(rolesCount, maxRoles, usersCount, maxUsers, chunkDisplayValueOption);
        }

        return chunkDisplayValueOption;
    }

    private static void resolveDefaultSortMode(int max, DisplayValueOption displayValueOption) {
        if (max <= 500) {
            displayValueOption.setSortMode(RoleAnalysisSortMode.JACCARD);
        } else {
            displayValueOption.setSortMode(RoleAnalysisSortMode.FREQUENCY);
        }
    }

    private static void resolveDefaultChunkStructure(Integer rolesCount, int maxRoles, Integer usersCount, int maxUsers, DisplayValueOption displayValueOption) {
        if (rolesCount > maxRoles || usersCount > maxUsers) {
            displayValueOption.setChunkMode(RoleAnalysisChunkMode.COMPRESS);
//            } else if (rolesCount > maxRoles) {
//                displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND_USER);
//            } else if (usersCount > maxUsers) {
//                displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND_ROLE);
        } else {
            displayValueOption.setChunkMode(RoleAnalysisChunkMode.EXPAND);
        }
    }

    public <B extends MiningBaseTypeChunk> List<B> getMainMiningChunk() {
        return miningOperationChunk.getMainMiningChunk();
    }

    public <A extends MiningBaseTypeChunk> List<A> getAdditionalMiningChunk() {
        return miningOperationChunk.getAdditionalMiningChunk();
    }

    public double getMinFrequency() {
        return miningOperationChunk.getMinFrequency();
    }

    public double getMaxFrequency() {
        return miningOperationChunk.getMaxFrequency();
    }

    public RoleAnalysisSortMode getSortMode() {
        return displayValueOption.getSortMode();
    }

    public RoleAnalysisChunkMode getChunkModeValue() {
        return displayValueOption.getChunkMode();
    }

    public RoleAnalysisChunkAction getChunkAction() {
        return displayValueOption.getChunkAction();
    }

    public boolean isOutlierDetection() {
        return isOutlierDetection;
    }

    //TODO check for optimization
    public void recomputeChunks(List<DetectedPattern> selectedPatterns, @NotNull PageBase pageBase) {
        Task task = pageBase.createSimpleTask("recompute chunks");
        OperationResult result = task.getResult();
        this.miningOperationChunk = pageBase.getRoleAnalysisService().prepareMiningStructure(
                cluster,
                userSearchFilter, roleSearchFilter, assignmentSearchFilter,
                displayValueOption,
                isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER,
                selectedPatterns,
                result,
                task);
    }

    public void updateWithPatterns(List<DetectedPattern> selectedPatterns, PageBase pageBase) {
        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks();
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks();
        RoleAnalysisProcessModeType processMode = miningOperationChunk.getProcessMode();
        refreshCells(processMode, users, roles, miningOperationChunk.getMinFrequency(), miningOperationChunk.getMaxFrequency());

        if (CollectionUtils.isNotEmpty(selectedPatterns)) {
            Task task = pageBase.createSimpleTask("InitPattern"); //TODO task name
            OperationResult result = task.getResult();
            pageBase.getRoleAnalysisService().updateChunkWithPatterns(miningOperationChunk, processMode, selectedPatterns, task, result);
        }
    }

    public MiningOperationChunk getMininingOperationChunk() {
        return miningOperationChunk;
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

    public Set<String> getMarkedUsers() {
        return markedUsers;
    }
}
