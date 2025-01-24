/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class ProgressBarDto implements Serializable {

    public static final String F_ACTUAL_VALUE = "actualValue";
    public static final String F_PROGRESS_COLOR = "progressColor";
    public static final String F_BAR_TITLE = "barTitle";
    public static final String F_MIN_VALUE = "minValue";
    public static final String F_MAX_VALUE = "maxValue";
    public static final String F_BAR_TOOLTIP = "barToolTip";
    public static final String F_HELP_TOOLTIP = "helpTooltip";

    private double minValue = 0;
    private double maxValue = 100;
    private double actualValue = 100;
    private String barTitle = "";
    boolean isLinkTitle = false;
    boolean isUnusual = false;
    boolean isInline = false;
    String helpTooltip = "";

    private String progressColor = "#206f9d";
    private String barToolTip;

    transient List<RoleAnalysisAttributeStatistics> attributeStats;

    public ProgressBarDto(double actualValue, @Nullable String progressColor, String titleModel) {
        loadActualValue(actualValue);

        if (progressColor != null) {
            this.progressColor = progressColor;
        }

        this.barTitle = titleModel;
    }

    public ProgressBarDto(double actualValue, @Nullable String progressColor, @NotNull List<RoleAnalysisAttributeStatistics> attributeStats) {
        loadActualValue(actualValue);

        if (progressColor != null) {
            this.progressColor = progressColor;
        }

        this.isLinkTitle = true;
        this.attributeStats = attributeStats;
        resolveHelpTooltip(attributeStats);
    }

    private void loadActualValue(double actualValue) {
        BigDecimal bd = new BigDecimal(actualValue);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        this.actualValue = bd.doubleValue();
    }

    public String getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(String progressColor) {
        this.progressColor = progressColor;
    }

    public double getActualValue() {
        return actualValue;
    }

    public void setActualValue(double actualValue) {
        this.actualValue = actualValue;
    }

    public void setBarTitle(String barTitle) {
        this.barTitle = barTitle;
    }

    public String getBarTitle() {
        return barTitle;
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

    public Component buildTitleComponent(String id, PageBase pageBase) {
        if (isLinkTitle()) {
            List<PrismObject<FocusType>> prismObjects = extractFocusObjectsFromAttributeAnalysis(attributeStats, pageBase);
            return buildAjaxLinkTitlePanel(id, pageBase, prismObjects);
        } else {
            IconWithLabel progressBarTitle = new IconWithLabel(id, Model.of(getBarTitle())) {
                @Override
                protected @NotNull String getIconCssClass() {
                    return "";
                }
            };
            progressBarTitle.setOutputMarkupId(true);
            if (isUnusual()) {
                progressBarTitle.add(new TooltipBehavior());
                progressBarTitle.add(AttributeModifier.replace("title",
                        pageBase.createStringResource("Unusual value")));
            }
            return progressBarTitle;
        }
    }

    private @NotNull AjaxLinkPanel buildAjaxLinkTitlePanel(String id, PageBase pageBase, List<PrismObject<FocusType>> prismObjects) {
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(id, Model.of(getBarTitle())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                MembersDetailsPopupPanel detailsPanel = new MembersDetailsPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        pageBase.createStringResource("RoleAnalysis.analyzed.members.details.panel"),
                        prismObjects, RoleAnalysisProcessModeType.USER) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };

                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);
        return ajaxLinkPanel;
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
    public List<PrismObject<FocusType>> extractFocusObjectsFromAttributeAnalysis(
            List<RoleAnalysisAttributeStatistics> roleAnalysisAttributeResult,
            @NotNull PageBase pageBase) {

        if (roleAnalysisAttributeResult == null || roleAnalysisAttributeResult.isEmpty()) {
            return null;
        }

        List<PrismObject<FocusType>> focusObjects = new ArrayList<>();
        Task task = pageBase.createSimpleTask("resolveTitleLabel");
        OperationResult result = task.getResult();

        for (RoleAnalysisAttributeStatistics attributeStats : roleAnalysisAttributeResult) {
            String attributeValue = attributeStats.getAttributeValue();
            if (isValidUUID(attributeValue)) {
                @Nullable PrismObject<FocusType> focusObject = WebModelServiceUtils.loadObject(
                        FocusType.class, attributeValue, pageBase, task, result);
                if (focusObject != null) {
                    focusObjects.add(focusObject);
                }
            }
        }

        updateBarTitle(focusObjects, roleAnalysisAttributeResult);
        return focusObjects.isEmpty() ? null : focusObjects;
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

    public boolean isInline() {
        return isInline;
    }

    public void setInline(boolean inline) {
        isInline = inline;
    }
}

