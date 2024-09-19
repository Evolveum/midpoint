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

    BALANCED_COVERAGE("fa fa-balance-scale",
            "RoleAnalysisCategoryType.BALANCED_COVERAGE.description",
            null),
    EXACT_ACCESS_SIMILARITY("fa fa-key",
            "RoleAnalysisCategoryType.EXACT_ACCESS_SIMILARITY.description",
            null),
    DEPARTMENT("fa fa-building",
            "RoleAnalysisCategoryType.DEPARTMENT.description",
            RoleAnalysisProcessModeType.USER),
    BIRTHRIGHT_ROLE("fa fa-briefcase",
            "RoleAnalysisCategoryType.BIRTHRIGHT_ROLE.description",
            RoleAnalysisProcessModeType.ROLE),
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
            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper,
            @NotNull Task task,
            @NotNull OperationResult result) {
        switch (this) {
            case BALANCED_COVERAGE ->
                    new BalancedCoverageModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case EXACT_ACCESS_SIMILARITY ->
                    new ExactSimilarityModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case DEPARTMENT -> new DepartmentModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case ROLE_MINING_ADVANCED ->
                    new AdvancedModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case OUTLIER_DETECTION_ADVANCED ->
                    new OutlierModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case OUTLIERS_DEPARTMENT ->
                    new OutlierDepartmentModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case BIRTHRIGHT_ROLE ->
                    new BirthrightCoverageModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
        }
    }

    public static void generateConfiguration(
            @NotNull RoleAnalysisService service,
            @NotNull RoleAnalysisCategoryType category,
            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisProcedureType analysisProcedureType = getRoleAnalysisProcedureType(objectWrapper);
        switch (category) {
            case BALANCED -> new BalancedCoverageModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case EXACT -> new ExactSimilarityModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case DEPARTMENT -> new DepartmentModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case ADVANCED -> {
                if (analysisProcedureType == RoleAnalysisProcedureType.ROLE_MINING) {
                    new AdvancedModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
                } else {
                    new OutlierModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
                }
            }
            case OUTLIERS_DEPARTMENT ->
                    new OutlierDepartmentModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case BIRTHRIGHT ->
                    new BirthrightCoverageModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
        }
    }

    private static RoleAnalysisProcedureType getRoleAnalysisProcedureType(
            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper) {
        RoleAnalysisSessionType session = objectWrapper.getObject().getObject().asObjectable();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        return analysisOption.getAnalysisProcedureType();
    }

    public RoleAnalysisCategoryType resolveCategoryMode() {
        return switch (this) {
            case BIRTHRIGHT_ROLE -> RoleAnalysisCategoryType.BIRTHRIGHT;
            case BALANCED_COVERAGE -> RoleAnalysisCategoryType.BALANCED;
            case EXACT_ACCESS_SIMILARITY -> RoleAnalysisCategoryType.EXACT;
            case DEPARTMENT -> RoleAnalysisCategoryType.DEPARTMENT;
            case ROLE_MINING_ADVANCED -> RoleAnalysisCategoryType.ADVANCED;
            case OUTLIER_DETECTION_ADVANCED -> RoleAnalysisCategoryType.ADVANCED;
            case OUTLIERS_DEPARTMENT -> RoleAnalysisCategoryType.OUTLIERS_DEPARTMENT;
        };
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public RoleAnalysisProcessModeType getRequiredProcessModeConfiguration() {
        return requiredProcessModeConfiguration;
    }

}
