/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component;

import java.io.Serial;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.table.ChartedHeaderDto;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.wicket.chartjs.ChartConfiguration;
import com.evolveum.wicket.chartjs.ChartJsPanel;

public class WidgetRmChartComponent<T extends ChartConfiguration> extends BasePanel<DisplayType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CHART_PANEL = "chartPanel";
    private static final String ID_CHART_INNER_LABEL = "chartInnerLabel";
    private static final String ID_CHART_TITLE = "chartTitle";
    private static final String ID_CHART_VALUE = "chartValue";

    private final IModel<ChartedHeaderDto<T>> chartedHeaderDtoModel;

    public WidgetRmChartComponent(String id, IModel<DisplayType> headerTitleDisplayModel,
            IModel<ChartedHeaderDto<T>> chartedHeaderDtoModel) {
        super(id, headerTitleDisplayModel);
        this.chartedHeaderDtoModel = chartedHeaderDtoModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initInnerChartLabel() {
        Label label = new Label(ID_CHART_INNER_LABEL, new PropertyModel<>(
                chartedHeaderDtoModel, ChartedHeaderDto.F_CHART_INNER_LABEL));
        label.setOutputMarkupId(true);
        label.add(new VisibleBehaviour(this::isChartInnerLabelVisible));
        add(label);
    }

    protected boolean isChartInnerLabelVisible() {
        return chartedHeaderDtoModel.getObject().getChartInnerLabel() != null;
    }

    private void initLayout() {
        ChartJsPanel<T> chartPanel = new ChartJsPanel<>(ID_CHART_PANEL, new PropertyModel<>(
                chartedHeaderDtoModel,
                ChartedHeaderDto.F_CHART_CONFIGURATION));
        chartPanel.setOutputMarkupId(true);
        chartPanel.add(new VisibleBehaviour(this::chartDataExists));
        add(chartPanel);

        initInnerChartLabel();

        add(new MultiLineLabel(ID_CHART_TITLE, new PropertyModel<>(chartedHeaderDtoModel, ChartedHeaderDto.F_CHART_TITLE)));

        add(new Label(ID_CHART_VALUE, new PropertyModel<>(chartedHeaderDtoModel, ChartedHeaderDto.F_CHART_VALUE)));
    }

    protected boolean chartDataExists() {
        if (chartedHeaderDtoModel == null || chartedHeaderDtoModel.getObject() == null) {
            return false;
        }
        T chatConfig = chartedHeaderDtoModel.getObject().getChartConfiguration();
        if (chatConfig == null || chatConfig.getData() == null
                || CollectionUtils.isEmpty(chatConfig.getData().getDatasets())) {
            return false;
        }
        return chatConfig.getData().getDatasets().stream()
                .anyMatch(dataset -> {
                    if (dataset.getData() != null) {
                        return dataset.getData().stream().anyMatch(data -> {
                            if (data == null) {
                                return false;
                            }
                            if (data instanceof Number) {
                                return ((Number) data).longValue() != 0;
                            }
                            if (data instanceof String) {
                                return !((String) data).isEmpty();
                            }
                            return false;
                        });
                    }
                    return false;
                });
    }
}
