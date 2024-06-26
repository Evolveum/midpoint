/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.web.component.ObjectVerticalSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DecimalFormat;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

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

        RoleAnalysisOptionType analysisOption = getModelObject().getAnalysisOption();
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        String mode = Character.
                toUpperCase(processMode.value().charAt(0))
                + processMode.value().substring(1)
                + "/"
                + Character
                .toUpperCase(analysisCategory.value().charAt(0))
                + analysisCategory.value().substring(1);

        Double density = sessionStatistic.getMeanDensity();
        if (density == null) {
            density = 0.0;
        }

        String formattedDensity = new DecimalFormat("#.###")
                .format(Math.round(density * 1000.0) / 1000.0);

        List<DetailsTableItem> detailsModel = List.of(
                new DetailsTableItem(createStringResource("Mode"),
                        Model.of(mode)) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new Label(id, getValue());
                    }
                },
                new DetailsTableItem(createStringResource("Cluster count"),
                        Model.of(sessionStatistic.getClusterCount().toString())) {
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
                new DetailsTableItem(createStringResource("Processed objects"),
                        Model.of(sessionStatistic.getProcessedObjectCount().toString())) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new IconWithLabel(id, getValue()) {
                            @Override
                            public String getIconCssClass() {
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
                new DetailsTableItem(createStringResource("Mean density"),
                        Model.of(formattedDensity)) {

                    @Override
                    public Component createValueComponent(String id) {
                        String colorClass = densityBasedColor(Double.parseDouble(getValue().getObject().replace(',', '.')));
                        ProgressBar progressBar = new ProgressBar(id) {

                            @Override
                            public boolean isInline() {
                                return true;
                            }

                            @Override
                            public double getActualValue() {
                                return Double.parseDouble(getValue().getObject().replace(',', '.'));
                            }

                            @Override
                            public String getProgressBarColor() {
                                return colorClass;
                            }

                            @Override
                            public String getBarTitle() {
                                return "";
                            }
                        };
                        progressBar.setOutputMarkupId(true);
                        return progressBar;
                    }

                });

        return Model.ofList(detailsModel);
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-role";
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
