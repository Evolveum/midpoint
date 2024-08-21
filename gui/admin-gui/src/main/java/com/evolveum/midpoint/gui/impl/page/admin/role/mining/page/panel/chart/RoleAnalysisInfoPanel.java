/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.RoleAnalysisIdentifyWidgetPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDistributionProgressPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class RoleAnalysisInfoPanel extends BasePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisInfoPanel.class);

    private static final String ID_PATTERN_PANEL = "patternPanel";
    private static final String ID_OUTLIER_PANEL = "outlierPanel";
    private static final String ID_DISTRIBUTION_PANEL = "distributionPanel";

    public RoleAnalysisInfoPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initPanels();
    }

    private void initPanels() {
        initInfoPatternPanel();
        initInfoOutlierPanel();
        initDistributionPanel();
    }

    private void initDistributionPanel() {
        RoleAnalysisChartPanel roleAnalysisChartPanel = new RoleAnalysisChartPanel(ID_DISTRIBUTION_PANEL);
        roleAnalysisChartPanel.setOutputMarkupId(true);
        add(roleAnalysisChartPanel);
    }

    private void initInfoPatternPanel() {

        if (getModelPatterns() == null) {
            WebMarkupContainer roleAnalysisInfoOutlierPanel = new WebMarkupContainer(ID_PATTERN_PANEL);
            roleAnalysisInfoOutlierPanel.setOutputMarkupId(true);
            add(roleAnalysisInfoOutlierPanel);
        }

        RoleAnalysisIdentifyWidgetPanel test = new RoleAnalysisIdentifyWidgetPanel(ID_PATTERN_PANEL,
                createStringResource("Pattern.suggestions.title"), getModelPatterns()) {

            @Override
            protected @NotNull Component getBodyHeaderPanel(String id) {
                List<ProgressBar> progressBars = new ArrayList<>();

                progressBars.add(new ProgressBar(4 * 100 / (double) 5, ProgressBar.State.SUCCESS));
                progressBars.add(new ProgressBar(1 * 100 / (double) 5, ProgressBar.State.WARNINIG));

                RoleAnalysisDistributionProgressPanel<?> panel = new RoleAnalysisDistributionProgressPanel<>(id) {
                    @Contract("_ -> new")
                    @Override
                    protected @NotNull Component getPanelComponent(String id) {
                        return new ProgressBarPanel(id, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                return progressBars;
                            }
                        });
                    }

                    @Override
                    protected Component getLegendComponent(String id) {
                        RepeatingView view = new RepeatingView(id);
                        MetricValuePanel resolved = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id, Model.of("Resolved")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-success fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-success";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, 4);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                                label.add(AttributeAppender.append("style", "font-size:20px"));
                                return label;
                            }
                        };
                        resolved.setOutputMarkupId(true);
                        view.add(resolved);

                        MetricValuePanel inProgress = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id, Model.of("In progress")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-warning fa-2xs";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "text-warning";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + " gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, 1);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                                label.add(AttributeAppender.append("style", "font-size:20px"));
                                return label;
                            }
                        };
                        inProgress.setOutputMarkupId(true);
                        view.add(inProgress);

                        return view;

                    }
                };

                panel.setOutputMarkupId(true);
                panel.add(AttributeAppender.append("class", "col-12"));
                return panel;
            }

            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
            }
        };
        add(test);
    }

    private void initInfoOutlierPanel() {

        if (getModelOutliers() == null) {
            WebMarkupContainer roleAnalysisInfoOutlierPanel = new WebMarkupContainer(ID_OUTLIER_PANEL);
            roleAnalysisInfoOutlierPanel.setOutputMarkupId(true);
            add(roleAnalysisInfoOutlierPanel);
        }

        RoleAnalysisIdentifyWidgetPanel outlierPanel = new RoleAnalysisIdentifyWidgetPanel(ID_OUTLIER_PANEL,
                createStringResource("Outlier.suggestions.title"), getModelOutliers());
        outlierPanel.setOutputMarkupId(true);
        add(outlierPanel);
    }

    protected @Nullable IModel<List<IdentifyWidgetItem>> getModelOutliers() {
        return null;
    }

    protected @Nullable IModel<List<IdentifyWidgetItem>> getModelPatterns() {
        return null;
    }

}
