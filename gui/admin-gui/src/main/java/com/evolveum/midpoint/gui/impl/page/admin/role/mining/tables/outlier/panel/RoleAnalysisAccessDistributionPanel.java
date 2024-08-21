/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

public class RoleAnalysisAccessDistributionPanel<T extends Serializable> extends BasePanel<String> {

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
    public RoleAnalysisAccessDistributionPanel(String id) {
        super(id);
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
                Label label = new Label(id, getCount());
                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                label.add(AttributeAppender.append("style", "font-size:20px"));
                return label;
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
                label.add(AttributeAppender.append("class", "d-flex justify-content-end m-0"));
                label.add(AttributeAppender.append("style", "font-size:20px"));
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
                Label label = new Label(id, getDirectCount());
                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                label.add(AttributeAppender.append("style", "font-size:20px"));
                return label;
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
                Label label = new Label(id, getIndirectCount());
                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                label.add(AttributeAppender.append("style", "font-size:20px"));
                return label;
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
                Label label = new Label(id, getDuplicatedCount());
                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0"));
                label.add(AttributeAppender.append("style", "font-size:20px"));
                return label;
            }
        };
        duplicated.setOutputMarkupId(true);
        containerFooter.add(duplicated);

        Component actionComponent = getActionComponent(ID_ACTION_BUTTON);
        actionComponent.setOutputMarkupId(true);
        add(actionComponent);

    }

    protected Component getActionComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected Component getPanelComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected String getCount() {
        return "0";
    }

    protected String getAverageCount() {
        return "0 (TBD)";
    }

    protected String getDirectCount() {
        return "0";
    }

    protected String getIndirectCount() {
        return "0";
    }

    protected String getDuplicatedCount() {
        return "0";
    }
}
