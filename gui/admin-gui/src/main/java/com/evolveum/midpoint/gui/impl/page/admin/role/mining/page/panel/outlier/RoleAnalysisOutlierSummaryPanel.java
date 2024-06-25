/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

import java.text.DecimalFormat;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.web.component.ObjectVerticalSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

public class RoleAnalysisOutlierSummaryPanel extends ObjectVerticalSummaryPanel<RoleAnalysisOutlierType> {

    public RoleAnalysisOutlierSummaryPanel(String id, IModel<RoleAnalysisOutlierType> model) {
        super(id, model);
    }

    @Override
    protected IModel<String> getTitleForNewObject(RoleAnalysisOutlierType modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisOutlierSummaryPanel.new");
    }

    @Override
    protected @NotNull IModel<List<DetailsTableItem>> createDetailsItems() {
        List<RoleAnalysisOutlierDescriptionType> analysisResult = getModelObject().getResult();
        if (analysisResult == null) {
            return Model.ofList(List.of());
        }

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        //TODO session not cluster
        ObjectReferenceType targetClusterRef = getModelObject().getTargetSessionRef();
        Task task = pageBase.createSimpleTask("loadDetailsModel");
        OperationResult result = task.getResult();
        @Nullable PrismObject<RoleAnalysisSessionType> sessionPrism = roleAnalysisService
                .getSessionTypeObject(targetClusterRef.getOid(), task, result);
        if (sessionPrism == null) {
            return Model.ofList(List.of());
        }
        RoleAnalysisSessionType session = sessionPrism.asObjectable();

        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisCategoryType analysisCategory = analysisOption.getAnalysisCategory();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();

        String mode = Character.
                toUpperCase(processMode.value().charAt(0))
                + processMode.value().substring(1)
                + "/"
                + Character
                .toUpperCase(analysisCategory.value().charAt(0))
                + analysisCategory.value().substring(1);

        double averageConfidence = getModelObject().getClusterConfidence();
        String formattedConfidence = String.format("%.2f", averageConfidence);

        List<DetailsTableItem> detailsModel = List.of(
                new DetailsTableItem(createStringResource("Mode"),
                        Model.of(mode)) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new Label(id, getValue());
                    }
                },
                new DetailsTableItem(createStringResource("Outlier properties"),
                        Model.of(String.valueOf(analysisResult.size()))) {
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
                new DetailsTableItem(createStringResource("Association"),
                        Model.of(sessionPrism.getName().getOrig())) {
                    @Override
                    public Component createValueComponent(String id) {
                        return new IconWithLabel(id, getValue()) {
                            @Override
                            public String getIconCssClass() {
                                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
                            }

                            @Override
                            protected String getComponentCssClass() {
                                return super.getComponentCssClass() + " d-flex justify-content-end";
                            }
                        };
                    }
                },
                new DetailsTableItem(createStringResource("Confidence"),
                        Model.of(formattedConfidence)) {

                    @Override
                    public Component createValueComponent(String id) {
                        String colorClass = densityBasedColor(
                                Double.parseDouble(getValue().getObject().replace(',', '.')));
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
    protected String defineDescription(RoleAnalysisOutlierType object) {
        String lastRebuild = "Last rebuild: ";
        MetadataType metadata = object.getMetadata();
        if (metadata != null) {
            XMLGregorianCalendar createTimestamp = metadata.getCreateTimestamp();
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
