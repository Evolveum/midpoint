/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysis;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeStatistics;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
public class ProgressBarForm extends BasePanel<String> {
    private static final String ID_CONTAINER = "container";
    private static final String ID_FORM_TITLE = "progressFormTitle";
    private static final String ID_REPEATING_VIEW = "repeatingProgressBar";

    transient RoleAnalysisAttributeAnalysis analysisResult;

    public ProgressBarForm(String id, RoleAnalysisAttributeAnalysis analysisResult) {
        super(id);
        this.analysisResult = analysisResult;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        String title = analysisResult.getItemPath();
        int valuesCount = analysisResult.getAttributeStatistics().size();
        String attribute = getPageBase().createStringResource("Attribute.item.name").getString();
        Label titleForm = new Label(ID_FORM_TITLE, attribute + ": " + title + " (" + valuesCount + ")");
        titleForm.setOutputMarkupId(true);
        container.add(titleForm);

        RepeatingView repeatingProgressBar = new RepeatingView(ID_REPEATING_VIEW);
        repeatingProgressBar.setOutputMarkupId(true);
        container.add(repeatingProgressBar);

        initProgressBars(analysisResult, repeatingProgressBar, container);

    }

    private static void initProgressBars(@NotNull RoleAnalysisAttributeAnalysis analysisResult,
            @NotNull RepeatingView repeatingProgressBar, WebMarkupContainer container) {
        List<RoleAnalysisAttributeStatistics> roleAnalysisAttributeStructures = new ArrayList<>(analysisResult.getAttributeStatistics());
        roleAnalysisAttributeStructures.sort(Comparator.comparingDouble(RoleAnalysisAttributeStatistics::getFrequency).reversed());

        int maxVisibleBars = 3;
        int totalBars = 0;
        int size = roleAnalysisAttributeStructures.size();
        Map<Double, List<RoleAnalysisAttributeStatistics>> map = new TreeMap<>(Comparator.reverseOrder());
        if (size > 5 && analysisResult.isIsMultiValue()) {
            roleAnalysisAttributeStructures.forEach((item) -> {
                double frequency = item.getFrequency();
                List<RoleAnalysisAttributeStatistics> list = map.getOrDefault(frequency, new ArrayList<>());
                list.add(item);
                map.put(frequency, list);
            });

            totalBars = map.size();
            int counter = 0;
            for (Map.Entry<Double, List<RoleAnalysisAttributeStatistics>> entry : map.entrySet()) {
                Double key = entry.getKey();
                List<RoleAnalysisAttributeStatistics> value = entry.getValue();
                counter++;
                ProgressBar progressBar = new ProgressBar(repeatingProgressBar.newChildId()) {
                    @Override
                    public double getActualValue() {
                        return key;
                    }

                    @Override
                    public String getBarTitle() {
                        int size = value.size();
                        if (size == 1) {
                            return value.get(0).getAttributeValue();
                        }
                        return "Objects (" + size + ")";
                    }

                    @Override
                    public List<RoleAnalysisAttributeStatistics> getRoleAnalysisAttributeResult() {
                        return value;
                    }
                };
                progressBar.setOutputMarkupId(true);

                if (counter > maxVisibleBars) {
                    progressBar.setVisible(false);
                }

                repeatingProgressBar.add(progressBar);
            }

        } else {
            totalBars = roleAnalysisAttributeStructures.size();
            for (int i = 0; i < roleAnalysisAttributeStructures.size(); i++) {
                RoleAnalysisAttributeStatistics item = roleAnalysisAttributeStructures.get(i);
                String jsonValue = item.getAttributeValue();
                double frequency = item.getFrequency();

                ProgressBar progressBar = new ProgressBar(repeatingProgressBar.newChildId()) {
                    @Override
                    public double getActualValue() {
                        return frequency;
                    }

                    @Override
                    public String getBarTitle() {
                        return jsonValue;
                    }
                };
                progressBar.setOutputMarkupId(true);

                if (i >= maxVisibleBars) {
                    progressBar.setVisible(false);
                }

                repeatingProgressBar.add(progressBar);
            }
        }

        if (totalBars > maxVisibleBars) {
            AjaxLinkPanel showAllButton = new AjaxLinkPanel("showAllButton", Model.of("...")) {
                @Override
                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                    int counter = 0;
                    for (Component component : repeatingProgressBar) {
                        counter++;
                        if (!component.isVisible()) {
                            component.setVisible(true);
                        } else if (counter > maxVisibleBars) {
                            if (component.isVisible()) {
                                component.setVisible(false);
                            }
                        }
                    }
                    ajaxRequestTarget.add(container);
                }
            };
            container.add(showAllButton);
        } else {
            WebMarkupContainer showAllButton = new WebMarkupContainer("showAllButton");
            showAllButton.setVisible(false);
            container.add(showAllButton);
        }

    }
}
