/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
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
    private static final String ID_TITLE_CONTAINER = "title-container";
    private static final String ID_BAR = "progressBar";
    private static final String ID_BAR_PERCENTAGE = "progressBarPercentage";
    private static final String ID_BAR_PERCENTAGE_INLINE = "progressBarPercentageInline";
    private static final String ID_BAR_TITLE = "progressBarTitle";
    private static final String ID_BAR_TITTLE_DATA = "progressBarDetails";
    private static final String ID_CONTAINER_TITLE_DATA = "details-container";

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
        container.add(AttributeModifier.replace("style", getProgressBarContainerStyle()));
        container.setOutputMarkupId(true);
        add(container);

        resolveTitleLabel();

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_BAR);
        container.add(progressBar);

        setProgressBarParameters(progressBar);
    }

    private void resolveTitleDataLabel(WebMarkupContainer titleContainer) {
        String value;
        if (getInClusterCount() == null || getInRepoCount() == null) {
            value = "";
        } else {
            value = " (in-group=" + getInClusterCount()
                    + ", in-repo=" + getInRepoCount() + ", "
                    + "unusual=" + isUnusual() + ")";
        }

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER_TITLE_DATA);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(value)));
        titleContainer.add(container);

        Label help = new Label(ID_BAR_TITTLE_DATA);
        IModel<String> helpModel = Model.of(value);
        help.add(AttributeModifier.replace("data-original-title",
                createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        help.setOutputMarkupId(true);
        if (isUnusual()) {
            help.add(AttributeModifier.append("class", "fa-exclamation-triangle text-warning"));
        } else {
            help.add(AttributeModifier.append("class", " fa-info-circle text-info"));
        }
        container.add(help);
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

        WebMarkupContainer titleContainer = new WebMarkupContainer(ID_TITLE_CONTAINER);
        titleContainer.setOutputMarkupId(true);
        titleContainer.add(new VisibleBehaviour(() -> !isInline()));
        add(titleContainer);

        List<RoleAnalysisAttributeStatistics> roleAnalysisAttributeResult = getRoleAnalysisAttributeResult();
        barTitle = getBarTitle();
        Task task = getPageBase().createSimpleTask("resolveTitleLabel");
        OperationResult result = task.getResult();

        if (roleAnalysisAttributeResult != null) {
            RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
            List<PrismObject<FocusType>> objects = new ArrayList<>();
            Map<String, RoleAnalysisAttributeStatistics> objectsMap = new HashMap<>();
            for (RoleAnalysisAttributeStatistics attributeStats : roleAnalysisAttributeResult) {
                String attributeValue = attributeStats.getAttributeValue();

                if (attributeValue == null || !isValidUUID(attributeValue)) {
                    continue;
                }

                PrismObject<FocusType> focusObject = roleAnalysisService.getFocusTypeObject(attributeValue, task, result);
                if (focusObject != null) {
                    objectsMap.put(focusObject.getOid(), attributeStats);
                    objects.add(focusObject);
                }
            }

            if (objects.size() == 1 && objects.get(0) != null) {
                PolyString name = objects.get(0).getName();
                barTitle = name != null && name.getOrig() != null ? name.getOrig() : barTitle;
            }
            addAjaxLinkPanel(titleContainer, barTitle, objects, objectsMap);
        } else {
            PrismObject<FocusType> focusTypeObject = resolveFocusTypeObject(barTitle, task, result);
            if (focusTypeObject != null) {
                barTitle = focusTypeObject.getName() != null ? focusTypeObject.getName().getOrig() : barTitle;
                addAjaxLinkPanel(titleContainer, barTitle, Collections.singletonList(focusTypeObject), null);
            } else {
                addProgressBarTitleLabel(titleContainer, barTitle);
            }
        }

        resolveTitleDataLabel(titleContainer);

        if (isInline()) {
            Label progressBarText = new Label(ID_BAR_PERCENTAGE_INLINE, () -> String.format("%.2f%%", getActualValue()));
            progressBarText.setOutputMarkupId(true);
            add(progressBarText);

            WebMarkupContainer progressBarInline = new WebMarkupContainer(ID_BAR_PERCENTAGE);
            progressBarInline.setOutputMarkupId(true);
            titleContainer.add(progressBarInline);
        } else {
            Label progressBarText = new Label(ID_BAR_PERCENTAGE, () -> String.format("%.2f%%", getActualValue()));
            progressBarText.setOutputMarkupId(true);
            titleContainer.add(progressBarText);

            WebMarkupContainer progressBarInline = new WebMarkupContainer(ID_BAR_PERCENTAGE_INLINE);
            progressBarInline.setOutputMarkupId(true);
            progressBarInline.add(new VisibleBehaviour(() -> false));
            add(progressBarInline);
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

    private void addProgressBarTitleLabel(@NotNull WebMarkupContainer titleContainer, String barTitle) {
        IconWithLabel progressBarTitle = new IconWithLabel(ID_BAR_TITLE, Model.of(barTitle)) {
            @Override
            protected @NotNull String getIconCssClass() {
//                if (isUnusual()) {
//                    return "fa fa-exclamation-triangle text-warning";
//                }
                return "";
            }
        };
        progressBarTitle.setOutputMarkupId(true);
        if (isUnusual()) {
            progressBarTitle.add(new TooltipBehavior());
            progressBarTitle.add(AttributeModifier.replace("title",
                    createStringResource("Unusual value")));
        }
        titleContainer.add(progressBarTitle);
    }

    private void addAjaxLinkPanel(
            @NotNull WebMarkupContainer titleContainer,
            @NotNull String barTitle,
            @NotNull List<PrismObject<FocusType>> objects,
            @Nullable Map<String, RoleAnalysisAttributeStatistics> objectsMap) {
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(ID_BAR_TITLE, Model.of(barTitle)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                MembersDetailsPopupPanel detailsPanel = new MembersDetailsPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        createStringResource("RoleAnalysis.analyzed.members.details.panel"),
                        objects, RoleAnalysisProcessModeType.USER) {
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
        titleContainer.add(ajaxLinkPanel);
    }

    private boolean isValidUUID(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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

    public boolean isUnusual() {
        return false;
    }

    public String getInClusterCount() {
        return null;
    }

    public boolean isInline() {
        return false;
    }

    protected String getProgressBarContainerStyle() {
        return null;
    }
}
