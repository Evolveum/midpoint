/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a progress bar component used for visualizing progress in the user interface.
 * The progress bar displays a title, actual progress value, and a text representation of the progress percentage.
 * It also provides the ability to click on the title to view details about the analyzed members.
 * <p>
 * The progress bar can be customized by setting the minimum and maximum values, as well as the actual progress value.
 */
public class ProgressBar extends BasePanel<String> {
    private static final String ID_CONTAINER = "progressBarContainer";
    private static final String ID_BAR = "progressBar";
    private static final String ID_BAR_PERCENTAGE = "progressBarPercentage";
    private static final String ID_BAR_PERCENTAGE_INLINE = "progressBarPercentageInline";
    private static final String ID_BAR_TITLE = "progressBarTitle";
    private static final String ID_BAR_TITTLE_DATA = "progressBarDetails";

    double minValue = 0;
    double maxValue = 100;
    double actualValue = 100;
    String barTitle = "";

    public ProgressBar(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        resolveTitleLabel();

        resolveTitleDataLabel();

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_BAR);
        container.add(progressBar);

        setProgressBarParameters(progressBar);

        initProgressValueLabel();
    }

    private void initProgressValueLabel() {
        if (isInline()) {
            Label progressBarText = new Label(ID_BAR_PERCENTAGE_INLINE, () -> String.format("%.2f%%", getActualValue()));
            progressBarText.setOutputMarkupId(true);
            add(progressBarText);

            WebMarkupContainer progressBarInline = new WebMarkupContainer(ID_BAR_PERCENTAGE);
            progressBarInline.setOutputMarkupId(true);
            add(progressBarInline);
        } else {
            Label progressBarText = new Label(ID_BAR_PERCENTAGE, () -> String.format("%.2f%%", getActualValue()));
            progressBarText.setOutputMarkupId(true);
            add(progressBarText);

            WebMarkupContainer progressBarInline = new WebMarkupContainer(ID_BAR_PERCENTAGE_INLINE);
            progressBarInline.setOutputMarkupId(true);
            add(progressBarInline);
        }
    }

    private void resolveTitleDataLabel() {
        String value;
        if (getInClusterCount() == null || getInRepoCount() == null) {
            value = "";
        } else {
            value = " (in-group=" + getInClusterCount()
                    + ", in-repo=" + getInRepoCount() + ")";
        }
        Label label = new Label(ID_BAR_TITTLE_DATA, value);
        label.setOutputMarkupId(true);
        add(label);
    }

    private void setProgressBarParameters(@NotNull WebMarkupContainer progressBar) {
        progressBar.add(AttributeModifier.replace("aria-valuemin",
                getMinValue()));
        progressBar.add(AttributeModifier.replace("aria-valuemax",
                getMaxValue()));
        progressBar.add(AttributeModifier.replace("aria-valuenow",
                getActualValue()));
        progressBar.add(AttributeModifier.replace("style", "width: " +
                getActualValue() + "%"));
        //set color
        progressBar.add(AttributeModifier.append("style", "; background-color: " +
                getProgressBarColor()));
    }

    private void resolveTitleLabel() {
        List<RoleAnalysisAttributeStatistics> roleAnalysisAttributeResult = getRoleAnalysisAttributeResult();
        barTitle = getBarTitle();
        Task task = getPageBase().createSimpleTask("resolveTitleLabel");
        OperationResult result = task.getResult();

        if (roleAnalysisAttributeResult != null) {
            RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
            List<PrismObject<FocusType>> objects = new ArrayList<>();
            Map<String, RoleAnalysisAttributeStatistics> objectsMap = new HashMap<>();
            for (RoleAnalysisAttributeStatistics analysisResult : roleAnalysisAttributeResult) {
                PrismObject<FocusType> focusTypeObject = roleAnalysisService.getFocusTypeObject(
                        analysisResult.getAttributeValue(), task, result);
                if (focusTypeObject == null) {
                    continue;
                }
                objectsMap.put(focusTypeObject.getOid(), analysisResult);
                objects.add(focusTypeObject);
            }
            if (objects.size() == 1 && objects.get(0) != null) {
                PolyString name = objects.get(0).getName();
                barTitle = name != null && name.getOrig() != null ? name.getOrig() : barTitle;
            }
            addAjaxLinkPanel(barTitle, objects, objectsMap);
        } else {
            PrismObject<FocusType> focusTypeObject = resolveFocusTypeObject(barTitle, task, result);
            if (focusTypeObject != null) {
                barTitle = focusTypeObject.getName() != null ? focusTypeObject.getName().getOrig() : barTitle;
                addAjaxLinkPanel(barTitle, Collections.singletonList(focusTypeObject), null);
            } else {
                addProgressBarTitleLabel(barTitle);
            }
        }
    }

    private PrismObject<FocusType> resolveFocusTypeObject(String barTitle, Task task, OperationResult result) {
        PrismObject<FocusType> focusTypeObject = null;
        try {
            UUID uuid = UUID.fromString(barTitle);
            focusTypeObject = getPageBase().getRoleAnalysisService().getFocusTypeObject(uuid.toString(), task, result);
        } catch (IllegalArgumentException ignored) {
        }
        return focusTypeObject;
    }

    private void addProgressBarTitleLabel(String barTitle) {
        Label progressBarTitle = new Label(ID_BAR_TITLE, barTitle);
        progressBarTitle.setOutputMarkupId(true);
        add(progressBarTitle);
    }

    private void addAjaxLinkPanel(@NotNull String barTitle, @NotNull List<PrismObject<FocusType>> objects,
            @Nullable Map<String, RoleAnalysisAttributeStatistics> objectsMap) {
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(ID_BAR_TITLE, Model.of(barTitle)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                MembersDetailsPopupPanel detailsPanel = new MembersDetailsPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"), objects, RoleAnalysisProcessModeType.USER) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };

                detailsPanel.setMap(objectsMap);
                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);
        add(ajaxLinkPanel);
    }

    private double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    private double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getActualValue() {
        return actualValue;
    }

    public String getBarTitle() {
        return barTitle;
    }

    public String getProgressBarColor() {
        return "#206f9d";
    }

    public List<RoleAnalysisAttributeStatistics> getRoleAnalysisAttributeResult() {
        return null;
    }

    public String getInRepoCount() {
        return null;
    }

    public String getInClusterCount() {
        return null;
    }

    public boolean isInline() {
        return false;
    }
}
