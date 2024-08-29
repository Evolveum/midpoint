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
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.jetbrains.annotations.NotNull;

public enum AnalysisCategory implements TileEnum {

    BALANCED_COVERAGE("fa fa-balance-scale",
            "RoleAnalysisCategoryType.BALANCED_COVERAGE.description",
            null),
    EXACT_ACCESS_SIMILARITY("fa fa-key",
            "RoleAnalysisCategoryType.EXACT_ACCESS_SIMILARITY.description",
            null),
    DEPARTMENT("fa fa-building",
            "RoleAnalysisCategoryType.DEPARTMENT.description",
            RoleAnalysisProcessModeType.USER),

    ADVANCED("fa fa-sliders-h",
            "RoleAnalysisCategoryType.ADVANCED.description",
            null),
    OUTLIER("fa fa-wrench",
            "RoleAnalysisCategoryType.OUTLIER.description",
            RoleAnalysisProcessModeType.USER),
    BIRTHRIGHT_ROLE("fa fa-briefcase",
            "RoleAnalysisCategoryType.BIRTHRIGHT_ROLE.description",
            RoleAnalysisProcessModeType.ROLE);

    private final String iconClass;
    private final String descriptionKey;
    private final RoleAnalysisProcessModeType requiredProcessModeConfiguration;

    AnalysisCategory(String iconClass, String descriptionKey, RoleAnalysisProcessModeType requairedProcessModeConfiguration) {
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
            case ADVANCED -> new AdvancedModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case OUTLIER -> new OutlierModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case BIRTHRIGHT_ROLE -> new BirthrightCoverageModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
        }
    }

    public static void generateConfiguration(
            @NotNull RoleAnalysisService service,
            RoleAnalysisCategoryType category,
            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapper,
            @NotNull Task task,
            @NotNull OperationResult result) {
        switch (category) {
            case BALANCED -> new BalancedCoverageModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case EXACT -> new ExactSimilarityModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case DEPARTMENT -> new DepartmentModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case ADVANCED -> new AdvancedModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case OUTLIERS -> new OutlierModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case BIRTHRIGHT -> new BirthrightCoverageModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
        }
    }

    public RoleAnalysisCategoryType resolveCategoryMode() {
        return switch (this) {
            case BIRTHRIGHT_ROLE -> RoleAnalysisCategoryType.BIRTHRIGHT;
            case BALANCED_COVERAGE -> RoleAnalysisCategoryType.BALANCED;
            case EXACT_ACCESS_SIMILARITY -> RoleAnalysisCategoryType.EXACT;
            case DEPARTMENT -> RoleAnalysisCategoryType.DEPARTMENT;
            case ADVANCED -> RoleAnalysisCategoryType.ADVANCED;
            case OUTLIER -> RoleAnalysisCategoryType.OUTLIERS;
        };
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public RoleAnalysisProcessModeType getRequiredProcessModeConfiguration() {
        return requiredProcessModeConfiguration;
    }

}
