/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class RoleAnalysisAccessDistributionPanel extends BasePanel<AccessDistributionDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER_HEADER = "container-header";
    private static final String ID_LEFT_METRIC = "left-metric";
    private static final String ID_RIGHT_METRIC = "right-metric";

    private static final String ID_CONTAINER_PANEL = "container-panel";
    private static final String ID_PANEL = "panel";

    private static final String ID_CONTAINER_FOOTER = "container-footer";
    private static final String ID_DIRECT = "direct";
    private static final String ID_INDIRECT = "indirect";
    private static final String ID_DUPLICATED = "duplicated";
    private static final String ID_ACTION_BUTTON = "actionButton";

    public RoleAnalysisAccessDistributionPanel(String id, IModel<AccessDistributionDto> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        WebMarkupContainer containerHeader = new WebMarkupContainer(ID_CONTAINER_HEADER);
        containerHeader.setOutputMarkupId(true);
        add(containerHeader);

        MetricValuePanel leftMetric = new MetricValuePanel(ID_LEFT_METRIC) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                return new Label(id, "Total accesses");
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull Component getValueComponent(String id) {
                return createLabel(id, getTotalAccessesCount());
            }
        };
        leftMetric.setOutputMarkupId(true);
        containerHeader.add(leftMetric);

        MetricValuePanel rightMetric = new MetricValuePanel(ID_RIGHT_METRIC) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                return new Label(id, "Average per user");
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull Component getValueComponent(String id) {
                Label label = new Label(id, getAverageCount());
                label.add(AttributeModifier.append("class", "d-flex justify-content-end m-0"));
                label.add(AttributeModifier.append("style", "font-size:20px"));
                return label;
            }
        };
        rightMetric.setOutputMarkupId(true);
        containerHeader.add(rightMetric);

        WebMarkupContainer containerPanel = new WebMarkupContainer(ID_CONTAINER_PANEL);
        containerPanel.setOutputMarkupId(true);
        add(containerPanel);

        Component panel = getPanelComponent(ID_PANEL);
        panel.setOutputMarkupId(true);
        containerPanel.add(panel);

        WebMarkupContainer containerFooter = new WebMarkupContainer(ID_CONTAINER_FOOTER);
        containerFooter.setOutputMarkupId(true);
        add(containerFooter);

        MetricValuePanel direct = new MetricValuePanel(ID_DIRECT) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                return new IconWithLabel(id, Model.of("Direct")) {
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
                return createLabel(id, getDirectCount());
            }
        };
        direct.setOutputMarkupId(true);
        containerFooter.add(direct);

        MetricValuePanel indirect = new MetricValuePanel(ID_INDIRECT) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                return new IconWithLabel(id, Model.of("Indirect")) {
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
                return createLabel(id, getIndirectCount());
            }
        };
        indirect.setOutputMarkupId(true);
        containerFooter.add(indirect);

        MetricValuePanel duplicated = new MetricValuePanel(ID_DUPLICATED) {
            @Contract("_ -> new")
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                return new IconWithLabel(id, Model.of("Duplicated")) {
                    @Override
                    protected String getIconCssClass() {
                        return "fa fa-circle text-danger fa-2xs";
                    }

                    @Override
                    protected String getLabelComponentCssClass() {
                        return "text-danger";
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
                return createLabel(id, getDuplicatedCount());
            }
        };
        duplicated.setOutputMarkupId(true);
        containerFooter.add(duplicated);

        Component actionComponent = getActionComponent(ID_ACTION_BUTTON);
        actionComponent.setOutputMarkupId(true);
        add(actionComponent);

    }

    private @NotNull Label createLabel(String id, int directCount) {
        Label label = new Label(id, directCount);
        label.add(AttributeModifier.append("class", "d-flex pl-3 m-0"));
        label.add(AttributeModifier.append("style", "font-size:20px"));
        return label;
    }

    protected Component getActionComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getPanelComponent(String id) {
        List<ProgressBar> progressBars = new ArrayList<>();
        addProgressBar(progressBars, ProgressBar.State.SUCCESS, getDirectCount(), getTotalAccessesCount());
        addProgressBar(progressBars, ProgressBar.State.WARNING, getIndirectCount(), getTotalAccessesCount());
        addProgressBar(progressBars, ProgressBar.State.DANGER, getDuplicatedCount(), getTotalAccessesCount());

        ProgressBarPanel progressBarPanel = new ProgressBarPanel(id, new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                return progressBars;
            }
        });
        progressBarPanel.add(AttributeModifier.append("class", "p-0 m-3 justify-content-center"));
        return progressBarPanel;
    }

    protected int getTotalAccessesCount() {
        return getModelObject().getAllAssignmentCount();
    }

    protected double getAverageCount() {
        return getModelObject().getAverageAccessPerUser();
    }

    protected int getDirectCount() {
        return getModelObject().getDirectAssignment();
    }

    protected int getIndirectCount() {
        return getModelObject().getIndirectAssignment();
    }

    protected int getDuplicatedCount() {
        return getModelObject().getDuplicatedRoleAssignmentCount();
    }

    private void addProgressBar(@NotNull List<ProgressBar> list, @NotNull ProgressBar.State state, int value, int totalValue) {
        //disabled legend
        list.add(new ProgressBar(value * 100 / (double) totalValue, state));
    }
}
