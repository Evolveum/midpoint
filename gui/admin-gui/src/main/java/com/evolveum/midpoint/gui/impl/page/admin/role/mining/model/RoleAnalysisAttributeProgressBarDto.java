/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class RoleAnalysisAttributeProgressBarDto extends RoleAnalysisProgressBarDto implements Serializable {

    public static final String F_BAR_TOOLTIP = "barToolTip";
    public static final String F_HELP_TOOLTIP = "helpTooltip";

    boolean isLinkTitle = false;
    boolean isUnusual = false;
    String helpTooltip = "";

    private String barToolTip;

    transient List<RoleAnalysisAttributeStatistics> attributeStats;

    private transient List<PrismObject<FocusType>> focusObjects = new ArrayList<>();

    public RoleAnalysisAttributeProgressBarDto(PageBase pageBase, double actualValue,
                                               @Nullable String progressColor,
                                               @NotNull List<RoleAnalysisAttributeStatistics> attributeStats) {
        loadActualValue(actualValue);

        if (progressColor != null) {
            this.progressColor = progressColor;
        }

        this.attributeStats = attributeStats;
        resolveHelpTooltip(attributeStats);
        extractFocusObjectsFromAttributeAnalysis(pageBase);
    }

    private void loadActualValue(double actualValue) {
        BigDecimal bd = new BigDecimal(actualValue);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        this.actualValue = bd.doubleValue();
    }

    public String getBarToolTip() {
        return barToolTip;
    }

    public void setBarToolTip(String barToolTip) {
        this.barToolTip = barToolTip;
    }

    public boolean isLinkTitle() {
        return isLinkTitle;
    }

    /**
     * Extracts a list of {@link PrismObject} instances of type {@link FocusType} from the given attribute
     * analysis results (if attributeValue reflect to PrismObject).
     *
     * <p>This method processes a list of {@link RoleAnalysisAttributeStatistics}, extracting attribute values and resolving
     * them to {@link FocusType} objects. If valid UUIDs are found in the attribute values, the corresponding
     * {@link PrismObject} instances are loaded and added to the result list. Additionally, the method updates the
     * {@code barTitle} based on the results.</p>
     */
    private void extractFocusObjectsFromAttributeAnalysis(
            @NotNull PageBase pageBase) {

        if (attributeStats == null || attributeStats.isEmpty()) {
            return;
        }

        Task task = pageBase.createSimpleTask("resolveTitleLabel");
        OperationResult result = task.getResult();

        for (RoleAnalysisAttributeStatistics attributeStatsItem : attributeStats) {
            String attributeValue = attributeStatsItem.getAttributeValue();
            if (isValidUUID(attributeValue)) {
                @Nullable PrismObject<FocusType> focusObject = WebModelServiceUtils.loadObject(
                        FocusType.class, attributeValue, pageBase, task, result);
                if (focusObject != null) {
                    focusObjects.add(focusObject);
                }
            }
        }

        updateBarTitle(focusObjects, attributeStats);
    }

    private void updateBarTitle(
            @Nullable List<PrismObject<FocusType>> focusObjects,
            @Nullable List<RoleAnalysisAttributeStatistics> roleAnalysisAttributeResult) {
        if ((focusObjects == null || focusObjects.isEmpty())
                && (roleAnalysisAttributeResult != null && !roleAnalysisAttributeResult.isEmpty())) {
            setBarTitleForEmptyFocusObjects(roleAnalysisAttributeResult);
        } else if (focusObjects != null && !focusObjects.isEmpty()) {
            setBarTitleForNonEmptyFocusObjects(focusObjects);
        }
    }

    private void setBarTitleForEmptyFocusObjects(@NotNull List<RoleAnalysisAttributeStatistics> roleAnalysisAttributeResult) {
        if (roleAnalysisAttributeResult.size() == 1) {
            this.barTitle = roleAnalysisAttributeResult.get(0).getAttributeValue();
        } else if (roleAnalysisAttributeResult.size() > 1) {
            this.barTitle = "(" + roleAnalysisAttributeResult.size() + ") values";
        }
    }

    private void setBarTitleForNonEmptyFocusObjects(@NotNull List<PrismObject<FocusType>> focusObjects) {
        this.isLinkTitle = true;
        if (focusObjects.size() == 1) {
            PolyString name = focusObjects.get(0).getName();
            this.barTitle = name != null && name.getOrig() != null ? name.getOrig() : this.barTitle;
        } else {
            this.barTitle = "(" + focusObjects.size() + ") objects";
        }
    }

    private void resolveHelpTooltip(List<RoleAnalysisAttributeStatistics> attributeStats) {
        if (attributeStats == null || attributeStats.isEmpty()) {
            return;
        }

        Integer inGroup = attributeStats.get(0).getInGroup();
        Integer inRepo = attributeStats.get(0).getInRepo();

        Boolean isUnusual = attributeStats.get(0).getIsUnusual();
        if (isUnusual != null) {
            this.isUnusual = isUnusual;
        }

        if (inGroup != null && inRepo != null) {
            this.helpTooltip = " (in-group=" + inGroup
                    + ", in-repo=" + inGroup + ", "
                    + "unusual=" + isUnusual() + ")";
        }
    }

    private boolean isValidUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isUnusual() {
        return isUnusual;
    }

    public String getHelpTooltip() {
        return helpTooltip;
    }

    public List<PrismObject<FocusType>> getFocusObjects() {
        return focusObjects;
    }
}

