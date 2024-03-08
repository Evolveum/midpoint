/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.NotNull;

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
    private static final String ID_BAR_TITLE = "progressBarTitle";

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

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_BAR);
        container.add(progressBar);

        setProgressBarParameters(progressBar);

        Label progressBarText = new Label(ID_BAR_PERCENTAGE, () -> String.format("%.2f%%", getActualValue()));
        progressBarText.setOutputMarkupId(true);
        add(progressBarText);
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
    }

    private void resolveTitleLabel() {
        String title = getBarTitle();
        String[] split = title.split(";");
        Task task = getPageBase().createSimpleTask("loadObject");
        OperationResult result = task.getResult();

        if (split.length > 1) {
            barTitle = split.length + " Objects";
            List<PrismObject<FocusType>> objects = Arrays.stream(split)
                    .map(objectOid -> getPageBase().getRoleAnalysisService().getFocusTypeObject(objectOid, task, result))
                    .collect(Collectors.toList());
            addAjaxLinkPanel(barTitle, objects);
        } else {
            barTitle = split[0];
            UUID uuid = null;
            try {
                uuid = UUID.fromString(barTitle);
            } catch (IllegalArgumentException ignored) {
            }

            PrismObject<FocusType> focusTypeObject;
            if (uuid != null) {
                focusTypeObject = getPageBase().getRoleAnalysisService().getFocusTypeObject(barTitle, task, result);
            } else {
                focusTypeObject = null;
            }

            if (focusTypeObject != null) {
                if (focusTypeObject.getName() != null) {
                    barTitle = focusTypeObject.getName().getOrig();
                }
                addAjaxLinkPanel(barTitle, Collections.singletonList(focusTypeObject));
            } else {
                Label progressBarTitle = new Label(ID_BAR_TITLE, barTitle);
                progressBarTitle.setOutputMarkupId(true);
                add(progressBarTitle);
            }
        }
    }

    private void addAjaxLinkPanel(@NotNull String barTitle, @NotNull List<PrismObject<FocusType>> objects) {
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
}
