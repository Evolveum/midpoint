/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.form.SwitchBoxPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarSecondStyle;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public enum AnomalyTableCategory implements Serializable {

    PARTITION_ANOMALY,
    OUTLIER_ANOMALY,
    OUTLIER_ACESS,
    OUTLIER_OVERVIEW;

    AnomalyTableCategory() {
    }

    public @NotNull List<IColumn<SelectableBean<RoleType>, String>> generateConfiguration(
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel,
            @NotNull PageBase pageBase,
            @NotNull ListMultimap<String, DetectedAnomalyResult> anomalyResultMap,
            @NotNull ListMultimap<String, RoleAnalysisOutlierPartitionType> anomalyPartitionMa) {
        switch (this) {
            case PARTITION_ANOMALY -> {
                return createDefaultColumnsPartitionAnomaly(pageBase, outlierModel, anomalyResultMap, anomalyPartitionMa);
            }
            case OUTLIER_ANOMALY -> {
                return createDefaultColumnsOutlierAnomaly(pageBase, anomalyResultMap, anomalyPartitionMa);
            }
            case OUTLIER_ACESS -> {
                return createDefaultColumnsOutlierAccess(pageBase, outlierModel);
            }
            case OUTLIER_OVERVIEW -> {
                return createDefaultColumnsOutlierOverview(pageBase, anomalyResultMap, outlierModel);
            }
        }
        return null;
    }

    public @NotNull List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumnsPartitionAnomaly(
            @NotNull PageBase pageBase,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel,
            @NotNull ListMultimap<String, DetectedAnomalyResult> anomalyResultMap,
            @NotNull ListMultimap<String, RoleAnalysisOutlierPartitionType> anomalyPartitionMap) {

        List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<RoleType>, String> column;

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.confidence.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                SelectableBean<RoleType> object = model.getObject();
                RoleType role = object.getValue();
                String oid = role.getOid();
                List<DetectedAnomalyResult> detectedAnomalyResults = anomalyResultMap.get(oid);

                if(detectedAnomalyResults.isEmpty()) {
                    cellItem.add(new EmptyPanel(componentId));
                    return;
                }

                if (detectedAnomalyResults.size() != 1) {
                    throw new IllegalStateException("Unexpected number of detected anomaly results for role "
                                    + oid + ": " + detectedAnomalyResults.size());
                }
                DetectedAnomalyResult anomalyResult = anomalyResultMap.get(oid).get(0);
                double confidence = anomalyResult.getStatistics().getConfidence();
                initDensityProgressPanelNew(cellItem, componentId, confidence);

            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.identification.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                SelectableBean<RoleType> object = model.getObject();
                RoleType role = object.getValue();
                String oid = role.getOid();
                List<RoleAnalysisOutlierPartitionType> partitions = anomalyPartitionMap.get(oid);
                Label label = new Label(componentId, String.valueOf(partitions.size()));
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.pattern.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {

                DetectedAnomalyResult anomalyResult = anomalyResultMap.get(model.getObject().getValue().getOid()).get(0);
                DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
                Double confidence = statistics.getPatternAnalysis().getConfidence();

                BigDecimal bd = new BigDecimal(Double.toString(confidence));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                double pointsConfidence = bd.doubleValue();

                Label label = new Label(componentId, pointsConfidence + "%");
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.member.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {

                DetectedAnomalyResult anomalyResult = anomalyResultMap.get(model.getObject().getValue().getOid()).get(0);
                DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
                Double memberCoverageConfidence = statistics.getMemberCoverageConfidence();

                BigDecimal bd = new BigDecimal(Double.toString(memberCoverageConfidence));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                double pointsConfidence = bd.doubleValue();

                Label label = new Label(componentId, (pointsConfidence) + "%");
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.frequency.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {

                DetectedAnomalyResult anomalyResult = anomalyResultMap.get(model.getObject().getValue().getOid()).get(0);
                DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
                Double frequency = statistics.getFrequency();

                BigDecimal bd = new BigDecimal(Double.toString(frequency));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                double pointsConfidence = bd.doubleValue();

                Label label = new Label(componentId, (pointsConfidence * 100) + "%");
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.itemConfidence.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {

                DetectedAnomalyResult anomalyResult = anomalyResultMap.get(model.getObject().getValue().getOid()).get(0);
                DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
                Double itemConfidence = statistics.getItemFactorConfidence();

                if(itemConfidence == null) {
                    itemConfidence = 0.0;
                }

                BigDecimal bd = new BigDecimal(Double.toString(itemConfidence));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                double pointsConfidence = bd.doubleValue();

                Label label = new Label(componentId, (pointsConfidence) + "%");
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.isDuplicate.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                RoleAnalysisOutlierType outlier = outlierModel.getObject();

                Set<String> duplicatedRoleAssignmentOids = loadUserDuplicatedRoleSet(outlier);
                RoleType role = model.getObject().getValue();
                String oid = role.getOid();
                boolean isDuplicate = duplicatedRoleAssignmentOids.contains(oid);
                Label label = new Label(componentId, isDuplicate);
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);
        return columns;
    }

    public @NotNull List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumnsOutlierAnomaly(
            @NotNull PageBase pageBase,
            @NotNull ListMultimap<String, DetectedAnomalyResult> anomalyResultMap,
            @NotNull ListMultimap<String, RoleAnalysisOutlierPartitionType> anomalyPartitionMap) {

        List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<RoleType>, String> column;

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.average.confidence.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                SelectableBean<RoleType> object = model.getObject();
                RoleType role = object.getValue();
                String oid = role.getOid();
                List<DetectedAnomalyResult> anomalyResult = anomalyResultMap.get(oid);
                double averageConfidence = 0.0;
                for (DetectedAnomalyResult detectedAnomalyResult : anomalyResult) {
                    double confidence = detectedAnomalyResult.getStatistics().getConfidence();
                    averageConfidence += confidence;
                }
                averageConfidence = averageConfidence / anomalyResult.size();

                initDensityProgressPanelNew(cellItem, componentId, averageConfidence);

            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.identification.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                SelectableBean<RoleType> object = model.getObject();
                RoleType role = object.getValue();
                String oid = role.getOid();
                List<RoleAnalysisOutlierPartitionType> partitions = anomalyPartitionMap.get(oid);
                Label label = new Label(componentId, String.valueOf(partitions.size()));
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

//        column = new AbstractExportableColumn<>(
//                pageBase.createStringResource("")) {
//
//            @Override
//            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
//                return null;
//            }
//
//            @Override
//            public Component getHeader(String componentId) {
//                return new Label(componentId,
//                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.unreliability.title"));
//            }
//
//            @Override
//            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
//                    String componentId, IModel<SelectableBean<RoleType>> model) {
//                Label label = new Label(componentId, "TBA");
//                label.setOutputMarkupId(true);
//                cellItem.add(label);
//            }
//
//            @Override
//            public boolean isSortable() {
//                return false;
//            }
//
//        };
//        columns.add(column);


        return columns;
    }

    public @NotNull List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumnsOutlierAccess(
            @NotNull PageBase pageBase,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {

        Set<String> duplicatedRoleSet = loadUserDuplicatedRoleSet(outlierModel.getObject());

        Set<String> userDirectRoleAssignemntSet = loadUserDirectAssignmentRoleSet(pageBase, outlierModel);

        List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<RoleType>, String> column;

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.assignment.type.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                SelectableBean<RoleType> object = model.getObject();
                RoleType role = object.getValue();
                String oid = role.getOid();

                String labelValue = "DIRECTLY ASSIGNED";
                boolean isDirectlyAssigned = userDirectRoleAssignemntSet.contains(oid);
                if (!isDirectlyAssigned) {
                    labelValue = "INDIRECTLY ASSIGNED";
                }

                Label label = new Label(componentId, labelValue);
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.isDuplicate.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                RoleType role = model.getObject().getValue();
                String oid = role.getOid();
                String labelValue = "NOT DUPLICATED";
                boolean isDuplicate = duplicatedRoleSet.contains(oid);

                if (isDuplicate) {
                    labelValue = "DUPLICATED";
                }

                Label label = new Label(componentId, labelValue);
                label.setOutputMarkupId(true);
                cellItem.add(label);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);
        return columns;
    }

    public @NotNull List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumnsOutlierOverview(
            @NotNull PageBase pageBase,
            @NotNull ListMultimap<String, DetectedAnomalyResult> anomalyResultMap,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {

        Set<String> duplicatedRoleSet = loadUserDuplicatedRoleSet(outlierModel.getObject());

        Set<String> userDirectRoleAssignemntSet = loadUserDirectAssignmentRoleSet(pageBase, outlierModel);

        List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<RoleType>, String> column;

        column = new AbstractExportableColumn<>(
                pageBase.createStringResource("")) {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
                return null;
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.average.confidence.title"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleType>> model) {
                SelectableBean<RoleType> object = model.getObject();
                RoleType role = object.getValue();
                String oid = role.getOid();

                List<DetectedAnomalyResult> detectedAnomalyResults = anomalyResultMap.get(oid);
                double averageConfidence = 0.0;
                for (DetectedAnomalyResult detectedAnomalyResult : detectedAnomalyResults) {
                    double confidence = detectedAnomalyResult.getStatistics().getConfidence();
                    averageConfidence += confidence;
                }
                averageConfidence = averageConfidence / detectedAnomalyResults.size();
                BigDecimal bd = new BigDecimal(Double.toString(averageConfidence));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                double pointsConfidence = bd.doubleValue();
                initDensityProgressPanelNew(cellItem, componentId, pointsConfidence);
            }

            @Override
            public boolean isSortable() {
                return false;
            }

        };
        columns.add(column);

//        column = new AbstractExportableColumn<>(
//                pageBase.createStringResource("")) {
//
//            @Override
//            public IModel<?> getDataModel(IModel<SelectableBean<RoleType>> iModel) {
//                return null;
//            }
//
//            @Override
//            public Component getHeader(String componentId) {
//                return new Label(componentId,
//                        pageBase.createStringResource("RoleAnalysisDetectedAnomalyTable.header.state.title"));
//            }
//
//            @Override
//            public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
//                    String componentId, IModel<SelectableBean<RoleType>> model) {
//                RoleType role = model.getObject().getValue();
//                String oid = role.getOid();
//                String title = "TBD";
//
//                Label label = new Label(componentId, title);
//                label.setOutputMarkupId(true);
//                cellItem.add(label);
//            }
//
//            @Override
//            public boolean isSortable() {
//                return false;
//            }
//
//        };
//        columns.add(column);
        return columns;
    }

    private static @NotNull Set<String> loadUserDuplicatedRoleSet(@NotNull RoleAnalysisOutlierType outlierModel) {
        List<ObjectReferenceType> duplicatedRoleAssignment = outlierModel.getDuplicatedRoleAssignment();
        Set<String> duplicatedRoleAssignmentOids = new HashSet<>();
        for (ObjectReferenceType objectReferenceType : duplicatedRoleAssignment) {
            duplicatedRoleAssignmentOids.add(objectReferenceType.getOid());
        }
        return duplicatedRoleAssignmentOids;
    }

    private static @NotNull Set<String> loadUserDirectAssignmentRoleSet(@NotNull PageBase pageBase,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        RoleAnalysisOutlierType outlier = outlierModel.getObject();
        ObjectReferenceType outlierUserRef = outlier.getObjectRef();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("loadUserTypeObject");

        PrismObject<UserType> userPrismObject = roleAnalysisService.getUserTypeObject(outlierUserRef.getOid(), task, task.getResult());
        if (userPrismObject == null) {
            return new HashSet<>();
        }

        UserType user = userPrismObject.asObjectable();
        List<AssignmentType> assignment = user.getAssignment();
        Set<String> assignmentOids = new HashSet<>();
        for (AssignmentType assignmentType : assignment) {
            if (assignmentType.getTargetRef().getType().equals(RoleType.COMPLEX_TYPE)) {
                assignmentOids.add(assignmentType.getTargetRef().getOid());
            }
        }
        return assignmentOids;
    }

    private static void initDensityProgressPanelNew(
            Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
            @NotNull String componentId,
            @NotNull Double density) {

        BigDecimal bd = new BigDecimal(Double.toString(density));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        String colorClass = densityBasedColorOposite(pointsDensity);

        ProgressBarSecondStyle progressBar = new ProgressBarSecondStyle(componentId) {

            @Override
            public boolean isInline() {
                return true;
            }

            @Override
            public double getActualValue() {
                return pointsDensity;
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
        progressBar.add(AttributeAppender.append("style", "min-width: 150px; max-width:220px;"));
        cellItem.add(progressBar);
    }
}
