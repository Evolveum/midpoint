/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.buildSimpleDensityBasedProgressBar;

import java.text.DecimalFormat;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisSettingsUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.web.component.ObjectVerticalSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisSessionSummaryPanel extends ObjectVerticalSummaryPanel<RoleAnalysisSessionType> {

    public RoleAnalysisSessionSummaryPanel(String id, IModel<RoleAnalysisSessionType> model) {
        super(id, model);
    }

    @Override
    protected IModel<String> getTitleForNewObject(RoleAnalysisSessionType modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisSessionSummaryPanel.new");
    }

    @Override
    protected @NotNull IModel<List<DetailsTableItem>> createDetailsItems() {
        RoleAnalysisSessionStatisticType sessionStatistic = getModelObject().getSessionStatistic();

        if (sessionStatistic == null) {
            return Model.ofList(List.of());
        }

        Double density = sessionStatistic.getMeanDensity();
        if (density == null) {
            density = 0.0;
        }

        String formattedDensity = new DecimalFormat("#.###")
                .format(Math.round(density * 1000.0) / 1000.0);

        Integer clusterCount = sessionStatistic.getClusterCount();

        if (clusterCount == null) {
            clusterCount = 0;
        }

        Integer processedObjectCount = sessionStatistic.getProcessedObjectCount();

        if (processedObjectCount == null) {
            processedObjectCount = 0;
        }

        List<DetailsTableItem> detailsModel = List.of(
                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.analysis.type.title"),
                        () -> RoleAnalysisSettingsUtil.getRoleAnalysisTypeMode(getModelObject().getAnalysisOption())) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new Label(id, getValue());
                    }
                },

                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.mode.title"),
                        () -> RoleAnalysisSettingsUtil.getRoleAnalysisMode(getModelObject().getAnalysisOption())) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new Label(id, getValue());
                    }
                },

                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.cluster.count.title"),
                        Model.of(clusterCount.toString())) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new IconWithLabel(id, getValue()) {
                            @Override
                            public String getIconCssClass() {
                                return IconAndStylesUtil.createDefaultColoredIcon(RoleAnalysisClusterType.COMPLEX_TYPE);
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " d-flex justify-content-end";
                            }
                        };
                    }
                },
                new DetailsTableItem(createStringResource(
                        "RoleAnalysisSessionSummaryPanel.details.table.processed.objects.title"),
                        Model.of(processedObjectCount.toString())) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new IconWithLabel(id, getValue()) {
                            @Override
                            public String getIconCssClass() {
                                RoleAnalysisOptionType analysisOption = RoleAnalysisSessionSummaryPanel.this.getModelObject().getAnalysisOption();
                                RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

                                if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                                    return IconAndStylesUtil.createDefaultColoredIcon(RoleType.COMPLEX_TYPE);
                                }
                                return IconAndStylesUtil.createDefaultColoredIcon(UserType.COMPLEX_TYPE);
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " d-flex justify-content-end";
                            }
                        };
                    }
                },
                new DetailsTableItem(
                        createStringResource("RoleAnalysisSessionSummaryPanel.details.table.mean.density.title"),
                        Model.of(formattedDensity)) {

                    @Override
                    public Component createValueComponent(String id) {
                        return buildSimpleDensityBasedProgressBar(id, getValue());
                    }

                });

        return Model.ofList(detailsModel);
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-mining";
    }

    @Override
    protected String defineDescription(RoleAnalysisSessionType object) {
        String lastRebuild = "Last rebuild: ";
        RoleAnalysisOperationStatus operationStatus = object.getOperationStatus();
        if (operationStatus != null) {
            XMLGregorianCalendar createTimestamp = operationStatus.getCreateTimestamp();
            if (createTimestamp != null) {
                int eonAndYear = createTimestamp.getYear();
                int month = createTimestamp.getMonth();
                int day = createTimestamp.getDay();
                String time = day + "/" + month + "/" + eonAndYear;
                lastRebuild = lastRebuild + time;
            }
        }
        return lastRebuild;
    }
}
