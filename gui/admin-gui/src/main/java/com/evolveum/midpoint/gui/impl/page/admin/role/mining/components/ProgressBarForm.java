/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributeAnalysisDto;

import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Represents a form containing multiple progress bars, each visualizing the frequency of certain values.
 * The form displays a title based on the attribute name and the count of values.
 * It iterates through JSON information provided during initialization to create individual progress bars.
 * <p>
 * Example usage:
 * <pre>{@code
 * String jsonInformation = "{ \"attribute1\": [{\"value\": \"value1\", \"frequency\": 50}, {\"value\": \"value2\", \"frequency\": 30}] }";
 * ProgressBarForm progressBarForm = new ProgressBarForm("progressBarForm", jsonInformation);
 * add(progressBarForm);
 * }</pre>
 */
public class ProgressBarForm extends BasePanel<RoleAnalysisAttributeAnalysisDto> {
    private static final String ID_CONTAINER = "container";
    private static final String ID_FORM_TITLE = "progressFormTitle";
    private static final String ID_REPEATING_VIEW = "repeatingProgressBar";
    private Set<String> pathToMark;

    public ProgressBarForm(String id, IModel<RoleAnalysisAttributeAnalysisDto> analysisResultDto, Set<String> pathToMark) {
        super(id, analysisResultDto);
        this.pathToMark = pathToMark;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        IModel<String> labelModel = createStringResource("${displayNameKey}", getModel());
        IconWithLabel titleForm = new IconWithLabel(ID_FORM_TITLE, labelModel) {
            @Override
            protected String getIconCssClass() {
                Class<?> parentType = ProgressBarForm.this.getModelObject().getType();
                if (parentType == null) {
                    return super.getIconCssClass();
                }
                if (UserType.class.equals(parentType)) {
                    return GuiStyleConstants.CLASS_OBJECT_USER_ICON + " fa-sm";
                } else {
                    return GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " fa-sm";
                }
            }

            @Override
            protected Component getSubComponent(String id) {
                List<RoleAnalysisAttributeStatistics> attributeStatistics = ProgressBarForm.this.getModelObject().getAttributeStatistics();
                if (attributeStatistics == null) {
                    return super.getSubComponent(id);
                }
                int attributeCount = attributeStatistics.size();
                Label label = new Label(id, attributeCount);
                label.setOutputMarkupId(true);
                label.add(AttributeAppender.append("class", "badge bg-transparent-red border border-danger text-danger"));
                return label;
            }
        };

        titleForm.setOutputMarkupId(true);
        container.add(titleForm);

        RepeatingView repeatingProgressBar = new RepeatingView(ID_REPEATING_VIEW);
        repeatingProgressBar.setOutputMarkupId(true);
        container.add(repeatingProgressBar);

        initProgressBars(repeatingProgressBar, container);
    }

    private void initProgressBars(@NotNull RepeatingView repeatingProgressBar, WebMarkupContainer container) {
        //TODO incorrect model unwrapping.
        List<RoleAnalysisAttributeStatistics> roleAnalysisAttributeStructures = new ArrayList<>(getModelObject().getAttributeStatistics());
        roleAnalysisAttributeStructures.sort(Comparator.comparingDouble(RoleAnalysisAttributeStatistics::getFrequency).reversed());

        int maxVisibleBars = 3;
        boolean isCompactView = roleAnalysisAttributeStructures.size() > 10;
        Map<Double, List<RoleAnalysisAttributeStatistics>> map = new TreeMap<>(Comparator.reverseOrder());

        if (isCompactView) {
            roleAnalysisAttributeStructures.forEach(item -> map.computeIfAbsent(item.getFrequency(), k -> new ArrayList<>()).add(item));
            for (Map.Entry<Double, List<RoleAnalysisAttributeStatistics>> entry : map.entrySet()) {
                Double frequency = entry.getKey();
                List<RoleAnalysisAttributeStatistics> stats = entry.getValue();
                boolean isMarkedAttribute = pathToMark != null && stats.stream().anyMatch(item -> pathToMark.contains(item.getAttributeValue()));
                ProgressBar progressBar = createProgressBar(repeatingProgressBar, frequency, stats, isMarkedAttribute);
                repeatingProgressBar.add(progressBar);
            }
        } else {
            for (RoleAnalysisAttributeStatistics item : roleAnalysisAttributeStructures) {
                boolean isMarkedAttribute = pathToMark != null && pathToMark.contains(item.getAttributeValue());
                ProgressBar progressBar = createProgressBar(repeatingProgressBar, item.getFrequency(), List.of(item), isMarkedAttribute);
                repeatingProgressBar.add(progressBar);
            }
        }

        var bars = repeatingProgressBar.stream().toList();
        for (int i = 0; i < repeatingProgressBar.size(); i++) {
            if (i >= maxVisibleBars) {
                bars.get(i).setVisible(false);
            }
        }

        if (repeatingProgressBar.size() > maxVisibleBars) {
            container.add(createShowAllButton(repeatingProgressBar, container, maxVisibleBars));
        } else {
            WebMarkupContainer showAllButton = new WebMarkupContainer("showAllButton");
            showAllButton.setVisible(false);
            container.add(showAllButton);
        }
    }

    private ProgressBar createProgressBar(RepeatingView repeatingProgressBar, double frequency, List<RoleAnalysisAttributeStatistics> value, boolean isMarkedAttribute) {
        return new ProgressBar(repeatingProgressBar.newChildId()) {
            @Override
            public double getActualValue() {
                return frequency;
            }

            @Override
            public String getProgressBarColor() {
                return isMarkedAttribute ? "#CA444B" : super.getProgressBarColor();
            }

            @Override
            public String getBarTitle() {
                return value.size() == 1 ? value.get(0).getAttributeValue() : "Objects (" + value.size() + ")";
            }

            @Override
            public String getInRepoCount() {
                return value.size() == 1 ? value.get(0).getInRepo().toString() : null;
            }

            @Override
            public boolean isUnusual() {
                return Objects.requireNonNullElse(value.get(0).getIsUnusual(), false);
            }

            @Override
            public String getInClusterCount() {
                return value.size() == 1 ? value.get(0).getInGroup().toString() : null;
            }

            @Override
            public List<RoleAnalysisAttributeStatistics> getRoleAnalysisAttributeResult() {
                return value;
            }
        };
    }

    private IconWithLabel createShowAllButton(RepeatingView repeatingProgressBar, WebMarkupContainer container, int maxVisibleBars) {
        return new IconWithLabel("showAllButton", createStringResource("ProgressBarForm.showAllButton")) {
            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                int counter = 0;
                for (Component component : repeatingProgressBar) {
                    counter++;
                    component.setVisible(counter <= maxVisibleBars || !component.isVisible());
                }
                target.add(container);
            }

            @Override
            protected @NotNull Component getSubComponent(String id) {
                Label image = new Label(id);
                image.add(AttributeModifier.replace("class", "fa fa-long-arrow-right"));
                image.add(AttributeModifier.replace("style", "color:rgb(32, 111, 157)"));
                image.setOutputMarkupId(true);
                return image;
            }
        };
    }
}
