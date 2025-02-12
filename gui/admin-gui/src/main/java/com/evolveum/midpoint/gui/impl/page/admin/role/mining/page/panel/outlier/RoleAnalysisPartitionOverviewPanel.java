/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.buildDensityProgressPanel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.web.component.dialog.Popupable;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * This class represents a panel that provides an overview of a role analysis partition.
 * It displays various widgets with information about the partition, such as anomalies, patterns, similarity, attribute, category, and duplicated assignments.
 * It is part of the admin GUI of the application.
 * Popups are supported (for example, when we want to display top/specific outlier partition in dialog).
 */
public class RoleAnalysisPartitionOverviewPanel extends BasePanel<RoleAnalysisOutlierPartitionType> implements Popupable {

    private static final String ID_WIDGETS = "widgets";

    IModel<RoleAnalysisOutlierType> outlierModel;

    /**
     * Constructor for the RoleAnalysisPartitionOverviewPanel.
     *
     * @param id The Wicket component ID.
     * @param model The model representing the role analysis partition.
     * @param outlierModel The model representing the outlier object.
     */
    public RoleAnalysisPartitionOverviewPanel(
            @NotNull String id,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> model, IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, model);
        this.outlierModel = outlierModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initWidgets();
    }

    private @NotNull Component getHeaderComponent(String id) {
        if (getOutlierModel() == null || getOutlierModel().getObject() == null) {
            return new WebMarkupContainer(id);
        }
        RoleAnalysisOutlierType object = getOutlierModel().getObject();

        PageBase pageBase = getPageBase();
        Task simpleTask = pageBase.createSimpleTask("Load object");
        OperationResult result = simpleTask.getResult();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        PrismObject<UserType> userPrismObject = roleAnalysisService
                .getUserTypeObject(object.getObjectRef().getOid(), simpleTask, result);
        if (userPrismObject == null) {
            return new WebMarkupContainer(id);
        }

        UserType user = userPrismObject.asObjectable();
        String outlierName = user.getName().getOrig();

        //TODO check why targetName is null
//        PolyStringType targetName = object.getTargetObjectRef().getTargetName();
//        String outlierName = "unknown";
//        if (targetName != null) {
//            outlierName = targetName.getOrig();
//        }

        RoleAnalysisOutlierPartitionType partition = getModelObject();
        XMLGregorianCalendar createTimestamp = partition.getCreateTimestamp();
        GregorianCalendar gregorianCalendar = createTimestamp.toGregorianCalendar();
        Date date = gregorianCalendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sdf.format(date);

        Double overallConfidence = partition.getPartitionAnalysis().getOverallConfidence();
        BigDecimal bd = new BigDecimal(overallConfidence);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(id,
                outlierModel.getObject().getOid(),
                outlierName,
                translate("Analysis.outlier.result.panel.title"),
                String.valueOf(pointsDensity), formattedDate);
        components.setOutputMarkupId(true);
        return components;
    }

    private void initWidgets() {

        RoleAnalysisHeaderWithWidgetsPanel components = new RoleAnalysisHeaderWithWidgetsPanel(ID_WIDGETS, loadDetailsModel()) {
            @Override
            protected @NotNull Component getHeaderComponent(String id) {
                return RoleAnalysisPartitionOverviewPanel.this.getHeaderComponent(id);
            }

            @Override
            protected @NotNull String replaceWidgetCssClass() {
                String css = super.replaceWidgetCssClass();
                String thisCss = RoleAnalysisPartitionOverviewPanel.this.replaceWidgetCssClass();
                if (thisCss != null) {
                    return thisCss;
                }

                return css;
            }
        };
        components.setOutputMarkupId(true);
        add(components);
    }

    protected @Nullable String replaceWidgetCssClass() {
        return null;
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel() {

        if (getOutlierModel() == null || getOutlierModel().getObject() == null || getModelObject() == null) {
            return Model.ofList(new ArrayList<>());
        }

        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        RoleAnalysisOutlierPartitionType partition = getModelObject();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();
        RoleAnalysisPatternAnalysis patternAnalysis = partitionAnalysis.getPatternAnalysis();

        Set<String> anomalyRoles = new HashSet<>();
        for (DetectedAnomalyResult anomalyResult : detectedAnomalyResult) {
            anomalyRoles.add(anomalyResult.getTargetObjectRef().getOid());
        }

        List<ObjectReferenceType> duplicatedRoleAssignment = outlier.getDuplicatedRoleAssignment();
        int duplicatedRoleAssignmentCount = 0;
        for (ObjectReferenceType objectReferenceType : duplicatedRoleAssignment) {
            if (anomalyRoles.contains(objectReferenceType.getOid())) {
                duplicatedRoleAssignmentCount++;
            }
        }

        int finalDuplicatedRoleAssignmentCount = duplicatedRoleAssignmentCount;
        List<WidgetItemModel> detailsModel = List.of(
                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.anomalies"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.anomalies.description.single";
                        if (detectedAnomalyResult.size() > 1) {
                            localizedKey = "RoleAnalysisOutlierType.widget.anomalies.description.multiple";
                        }
                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        double confidence = partitionAnalysis.getAnomalyObjectsConfidence();
                        String title = createStringResource("RoleAnalysisOutlierType.widget.anomalies.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, confidence, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1 mt-auto";
                    }

                    @Override
                    public Component createFooterComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.anomalies.footer",
                                detectedAnomalyResult.size()));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.similarity"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.similarity.description.single";
                        double confidence = 0.0;
                        double threshold = 0.0;
                        if (partitionAnalysis.getSimilarObjectAnalysis() != null) {
                            confidence = partitionAnalysis.getOutlierAssignmentFrequencyConfidence();
                            threshold = partitionAnalysis.getSimilarObjectAnalysis().getSimilarObjectsThreshold();
                        }
                        return new Label(id, createStringResource(localizedKey,
                                threshold, confidence));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        double confidence = 0.0;
                        if (partitionAnalysis.getSimilarObjectAnalysis() != null) {
                            confidence = partitionAnalysis.getSimilarObjectAnalysis().getSimilarObjectsDensity();
                        }
                        String title = createStringResource("RoleAnalysisOutlierType.widget.similarity.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, confidence, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1 ";
                    }

                    @Override
                    public Component createFooterComponent(String id) {
                        int similarObjectsCount = 0;
                        if (partitionAnalysis.getSimilarObjectAnalysis() != null) {
                            similarObjectsCount = partitionAnalysis.getSimilarObjectAnalysis().getSimilarObjectsCount();
                        }
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.similarity.footer",
                                similarObjectsCount));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.attribute"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.attribute.description.single";
                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        double confidence = 0.0;
                        AttributeAnalysis attributeAnalysis = partitionAnalysis.getAttributeAnalysis();
                        if (attributeAnalysis != null
                                && attributeAnalysis.getUserClusterCompare() != null
                                && attributeAnalysis.getUserClusterCompare().getScore() != null) {
                            confidence = attributeAnalysis.getUserClusterCompare().getScore();
                        }

                        String title = createStringResource("RoleAnalysisOutlierType.widget.attribute.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, confidence, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1";
                    }

                    @Override
                    public Component createFooterComponent(String id) {

                        int attributeAboveThreshold = 0;
                        int threshold = 80;

                        AttributeAnalysis attributeAnalysis1 = partitionAnalysis.getAttributeAnalysis();
                        if (attributeAnalysis1 != null) {
                            RoleAnalysisAttributeAnalysisResult userClusterCompare = attributeAnalysis1.getUserClusterCompare();
                            if (userClusterCompare != null && userClusterCompare.getAttributeAnalysis() != null) {
                                List<RoleAnalysisAttributeAnalysis> attributeAnalysis = userClusterCompare.getAttributeAnalysis();

                                for (RoleAnalysisAttributeAnalysis attribute : attributeAnalysis) {
                                    Double density = attribute.getDensity();
                                    if (density != null) {
                                        if (density >= threshold) {
                                            attributeAboveThreshold++;
                                        }
                                    }
                                }
                            }
                        }

                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.attribute.footer",
                                attributeAboveThreshold));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.patterns"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        if (patternAnalysis == null) {
                            return new Label(id, 0);
                        }

                        String localizedKey = "RoleAnalysisOutlierType.widget.patterns.description.single";
                        if (patternAnalysis.getDetectedPatternCount() > 1) {
                            localizedKey = "RoleAnalysisOutlierType.widget.patterns.description.multiple";
                        }
                        return new Label(id, createStringResource(localizedKey, patternAnalysis.getTotalRelations()));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        Double confidence = 0.0;
                        if (patternAnalysis != null) {
                            confidence = patternAnalysis.getConfidence();
                        }
                        String title = createStringResource("RoleAnalysisOutlierType.widget.patterns.confidence")
                                .getString();
                        return buildDensityProgressPanel(id, confidence, title);
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "pt-3 pb-1";
                    }

                    @Override
                    public Component createFooterComponent(String id) {
                        int detectedPatternCount = 0;
                        if (patternAnalysis != null) {
                            detectedPatternCount = patternAnalysis.getDetectedPatternCount();
                        }
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.patterns.footer",
                                detectedPatternCount));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.category"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "RoleAnalysisOutlierType.widget.category.description.single";
                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        String category = "unknown";
                        OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
                        if (outlierCategory != null && outlierCategory.getOutlierNoiseCategory() != null) {
                            category = outlierCategory.getOutlierNoiseCategory().value();
                        }

                        Label label = new Label(id, category);
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "d-flex align-items-center badge p-3 m-3 justify-content-center";
                    }

                    @Override
                    protected String replaceValueCssStyle() {
                        return "color: #18a2b8; font-size: 20px; background-color: #dcf1f4;";
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {

                    @Override
                    public Component createTitleComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.duplicated"));
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        String localizedKey = "No duplicated anomaly assignments has been detected";
                        if (finalDuplicatedRoleAssignmentCount == 1) {
                            localizedKey = "RoleAnalysisOutlierType.widget.duplicated.description.single";
                        } else if (finalDuplicatedRoleAssignmentCount > 1) {
                            localizedKey = "RoleAnalysisOutlierType.widget.duplicated.description.multiple";
                        }

                        return new Label(id, createStringResource(localizedKey));
                    }

                    @Override
                    public Component createValueComponent(String id) {
                        String category = createStringResource(
                                "RoleAnalysisOutlierType.widget.duplicated.value.notFound").getString();
                        if (finalDuplicatedRoleAssignmentCount > 0) {
                            category = createStringResource(
                                    "RoleAnalysisOutlierType.widget.duplicated.value.found").getString();
                        }

                        Label label = new Label(id, category);
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected String replaceValueCssClass() {
                        return "d-flex align-items-center badge p-3 m-3 justify-content-center";
                    }

                    @Override
                    protected String replaceValueCssStyle() {
                        return "color: #18a2b8; font-size: 20px; background-color: #dcf1f4;";
                    }

                    @Override
                    public Component createFooterComponent(String id) {
                        return new Label(id, createStringResource("RoleAnalysisOutlierType.widget.duplicated.footer",
                                finalDuplicatedRoleAssignmentCount));
                    }
                }
        );

        return Model.ofList(detailsModel);
    }

    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return this;
    }
}
