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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.jetbrains.annotations.NotNull;

public enum AnalysisCategory implements TileEnum {

//    STANDARD("fa fa-cogs"),
    BALANCED_COVERAGE("fa fa-balance-scale","RoleAnalysisCategoryType.BALANCED_COVERAGE.description"),
    EXACT_ACCESS_SIMILARITY("fa fa-key", "RoleAnalysisCategoryType.EXACT_ACCESS_SIMILARITY.description"),
    DEPARTMENT("fa fa-building", "RoleAnalysisCategoryType.DEPARTMENT.description"),
    ADVANCED("fa fa-sliders-h", "RoleAnalysisCategoryType.ADVANCED.description"),
    OUTLIER("fa fa-wrench", "RoleAnalysisCategoryType.OUTLIER.description");

    private final String iconClass;
    private final String descriptionKey;

    AnalysisCategory(String iconClass, String descriptionKey) {
        this.iconClass = iconClass;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public String getIcon() {
        return iconClass;
    }

    public void generateConfiguration(
            @NotNull RoleAnalysisService service,
            RoleAnalysisSessionType object,
            @NotNull Task task,
            @NotNull OperationResult result) {
        switch (this) {
//            case STANDARD -> new StandardModeConfiguration(service, objectWrapper, task, result).updateConfiguration();
            case BALANCED_COVERAGE -> new BalancedCoverageModeConfiguration(service, object, task, result).updateConfiguration();
            case EXACT_ACCESS_SIMILARITY -> new ExactSimilarityModeConfiguration(service, object, task, result).updateConfiguration();
            case DEPARTMENT -> new DepartmentModeConfiguration(service, object, task, result).updateConfiguration();
            case ADVANCED -> new AdvancedModeConfiguration(service, object, task, result).updateConfiguration();
            case OUTLIER -> new OutlierModeConfiguration(service, object, task, result).updateConfiguration();
        }
    }

    public RoleAnalysisCategoryType resolveCategoryMode() {
        return switch (this) {
//            case STANDARD -> RoleAnalysisCategoryType.STANDARD;
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

}
