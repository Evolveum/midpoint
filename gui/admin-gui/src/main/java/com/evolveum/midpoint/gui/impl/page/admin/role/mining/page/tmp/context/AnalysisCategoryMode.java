/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.context;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.modes.*;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public enum AnalysisCategoryMode implements TileEnum {

    BIRTHRIGHT_ROLE("fa fa-briefcase",
            "RoleAnalysisCategoryType.BIRTHRIGHT_ROLE.description",
            RoleAnalysisProcessModeType.ROLE),
    ATTRIBUTE_BASED("fa fa-tags",
            "RoleAnalysisCategoryType.ATTRIBUTE_BASED.description",
            RoleAnalysisProcessModeType.USER),
    BALANCED_COVERAGE("fa fa-balance-scale",
            "RoleAnalysisCategoryType.BALANCED_COVERAGE.description",
            null),
    EXACT_ACCESS_SIMILARITY("fa fa-key",
            "RoleAnalysisCategoryType.EXACT_ACCESS_SIMILARITY.description",
            null),
    DEPARTMENT("fa fa-building",
            "RoleAnalysisCategoryType.DEPARTMENT.description",
            RoleAnalysisProcessModeType.USER),
    OUTLIERS_DEPARTMENT("fa fa-wrench",
            "RoleAnalysisCategoryType.OUTLIER_DEPARTMENT.description",
            RoleAnalysisProcessModeType.USER),
    ROLE_MINING_ADVANCED("fa fa-sliders-h",
            "RoleAnalysisCategoryType.ADVANCED.description",
            null),
    OUTLIER_DETECTION_ADVANCED("fa fa-wrench",
            "RoleAnalysisCategoryType.ADVANCED.description",
            RoleAnalysisProcessModeType.USER);

    private final String iconClass;
    private final String descriptionKey;
    private final RoleAnalysisProcessModeType requiredProcessModeConfiguration;

    AnalysisCategoryMode(String iconClass, String descriptionKey, RoleAnalysisProcessModeType requairedProcessModeConfiguration) {
        this.iconClass = iconClass;
        this.descriptionKey = descriptionKey;
        this.requiredProcessModeConfiguration = requairedProcessModeConfiguration;
    }

    @Override
    public String getIcon() {
        return iconClass;
    }

    public boolean requiresProcessModeConfiguration() {
        return this.requiredProcessModeConfiguration == null;
    }

    public boolean requiresAdditionalConfiguration() {
        return this == BIRTHRIGHT_ROLE;
    }

    public void generateConfiguration(
            @NotNull RoleAnalysisService service,
            RoleAnalysisSessionType session,
            @NotNull Task task,
            @NotNull OperationResult result) {
//        RoleAnalysisSessionType session = objectWrapper.getObject().getObject().asObjectable();
        switch (this) {
            case BALANCED_COVERAGE ->
                    new BalancedCoverageModeConfiguration(service, session, task, result).updateConfiguration();
            case EXACT_ACCESS_SIMILARITY ->
                    new ExactSimilarityModeConfiguration(service, session, task, result).updateConfiguration();
            case DEPARTMENT -> new DepartmentModeConfiguration(service, session, task, result).updateConfiguration();
            case ATTRIBUTE_BASED -> new AttributeBasedModeConfiguration(service, session, task, result).updateConfiguration();
            case ROLE_MINING_ADVANCED ->
                    new AdvancedModeConfiguration(service, session, task, result).updateConfiguration();
            case OUTLIER_DETECTION_ADVANCED ->
                    new OutlierModeConfiguration(service, session, task, result).updateConfiguration();
            case OUTLIERS_DEPARTMENT ->
                    new OutlierDepartmentModeConfiguration(service, session, task, result).updateConfiguration();
            case BIRTHRIGHT_ROLE ->
                    new BirthrightCoverageModeConfiguration(service, session, task, result).updateConfiguration();
        }
    }

//    public static void generateConfiguration(
//            @NotNull RoleAnalysisService service,
//            @NotNull RoleAnalysisCategoryType category,
//            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper,
//            @NotNull Task task,
//            @NotNull OperationResult result) {
////        RoleAnalysisProcedureType analysisProcedureType = getRoleAnalysisProcedureType(objectWrapper);
//        RoleAnalysisSessionType session = objectWrapper.getObject().getObject().asObjectable();
//        switch (category) {
//            case BALANCED -> new BalancedCoverageModeConfiguration(service, session, task, result).updateConfiguration();
//            case EXACT -> new ExactSimilarityModeConfiguration(service, session, task, result).updateConfiguration();
//            case DEPARTMENT -> new DepartmentModeConfiguration(service, session, task, result).updateConfiguration();
//            case ADVANCED -> {
//                if (analysisProcedureType == RoleAnalysisProcedureType.ROLE_MINING) {
//                    new AdvancedModeConfiguration(service, session, task, result).updateConfiguration();
//                } else {
//                    new OutlierModeConfiguration(service, session, task, result).updateConfiguration();
//                }
//            }
//            case OUTLIERS_DEPARTMENT ->
//                    new OutlierDepartmentModeConfiguration(service, session, task, result).updateConfiguration();
//            case BIRTHRIGHT ->
//                    new BirthrightCoverageModeConfiguration(service, session, task, result).updateConfiguration();
//        }
//    }

    private static RoleAnalysisProcedureType getRoleAnalysisProcedureType(
            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper) {
        RoleAnalysisSessionType session = objectWrapper.getObject().getObject().asObjectable();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        return analysisOption.getAnalysisProcedureType();
    }

    public RoleAnalysisCategoryType resolveCategoryMode() {
        return switch (this) {
            case BIRTHRIGHT_ROLE -> RoleAnalysisCategoryType.BIRTHRIGHT;
            case ATTRIBUTE_BASED -> RoleAnalysisCategoryType.ATTRIBUTE_BASED;
            case BALANCED_COVERAGE -> RoleAnalysisCategoryType.BALANCED;
            case EXACT_ACCESS_SIMILARITY -> RoleAnalysisCategoryType.EXACT;
            case DEPARTMENT -> RoleAnalysisCategoryType.DEPARTMENT;
            case ROLE_MINING_ADVANCED -> RoleAnalysisCategoryType.ADVANCED;
            case OUTLIER_DETECTION_ADVANCED -> RoleAnalysisCategoryType.ADVANCED;
            case OUTLIERS_DEPARTMENT -> RoleAnalysisCategoryType.OUTLIERS_DEPARTMENT;
        };
    }

    public static AnalysisCategoryMode resolveCategoryMode(RoleAnalysisSessionType session) {
        RoleAnalysisOptionType option = session.getAnalysisOption();
        RoleAnalysisCategoryType categoryType = option.getAnalysisCategory();
        RoleAnalysisProcedureType processMode = option.getAnalysisProcedureType();

        return switch (categoryType) {
            case BIRTHRIGHT -> BIRTHRIGHT_ROLE;
            case BALANCED -> BALANCED_COVERAGE;
            case EXACT -> EXACT_ACCESS_SIMILARITY;
            case EXPLORATION -> null;
            case DEPARTMENT -> DEPARTMENT;
            case ATTRIBUTE_BASED -> ATTRIBUTE_BASED;
            case ADVANCED -> processMode == RoleAnalysisProcedureType.ROLE_MINING ? ROLE_MINING_ADVANCED : OUTLIER_DETECTION_ADVANCED;
            case OUTLIERS_DEPARTMENT -> OUTLIERS_DEPARTMENT;
        };
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public RoleAnalysisProcessModeType getRequiredProcessModeConfiguration() {
        return requiredProcessModeConfiguration;
    }

}
