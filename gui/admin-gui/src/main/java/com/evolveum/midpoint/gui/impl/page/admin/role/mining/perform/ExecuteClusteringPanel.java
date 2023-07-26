/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.perform;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.model.api.expr.MidpointFunctions.LOGGER;

import java.io.Serial;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyWrapperModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AxiomQueryWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.ClusteringExecutor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ExecuteClusteringPanel extends BasePanel<String> implements Popupable {
    private static final String ID_EXECUTE_CLUSTERING_FORM = "thresholds_form_cluster";
    private static final String ID_WARNING_FEEDBACK = "warningFeedback";
    private static final String ID_JACCARD_THRESHOLD_FIELD = "eps_cluster";
    protected static final String ID_SHOW_MINING_PANEL = "show_mining_panel";
    protected static final String ID_SHOW_CLUSTER_PANEL = "show_cluster_panel";
    private static final String ID_NAME_FIELD = "name_field";
    private static final String ID_INTERSECTION_THRESHOLD_FIELD = "intersection_field_min_cluster";
    private static final String ID_MIN_ASSIGN = "assign_min_occupy";
    private static final String ID_GROUP_THRESHOLD_FIELD = "group_min_cluster";
    private static final String ID_SUBMIT_BUTTON = "ajax_submit_link_cluster";

    OperationResult result;
    ClusterOptions clusterOptions;
    private boolean editMiningOption = false;
    private boolean editClusterOption = true;
    AjaxSubmitButton executeClustering;
    AjaxSubmitButton filterSubmitButton;

    OperationResult importResult = new OperationResult("ImportClusterTypeObject");

    public ExecuteClusteringPanel(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        this.clusterOptions = new ClusterOptions((PageBase) getPage(), RoleAnalysisProcessMode.USER, RoleAnalysisSearchMode.INTERSECTION);

        Form<?> cluseterForm = clusterForm();
        add(cluseterForm);

        AjaxButton showClusterPanelButton = showClusterPanel(cluseterForm);
        add(showClusterPanelButton);
        defaultIntersectionForm();
        addExecuteClusteringButton();
    }

    private void addExecuteClusteringButton() {
        executeClustering = new AjaxSubmitButton(ID_SUBMIT_BUTTON) {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget) {

                ClusteringExecutor clusteringExecutor = new ClusteringExecutor(clusterOptions.getMode());
                List<PrismObject<RoleAnalysisCluster>> clusters = clusteringExecutor.execute(clusterOptions);

                RoleAnalysisSessionFilterOption roleAnalysisSessionFilterOption = getRoleAnalysisSessionFilterOption();

                RoleAnalysisSessionDetectionOption roleAnalysisSessionDetectionOption = getRoleAnalysisSessionDetectionOption();

                importRoleAnalysisClusteringResult(importResult, clusters, roleAnalysisSessionFilterOption,
                        roleAnalysisSessionDetectionOption, clusterOptions.getName(), clusterOptions.getMode());

            }

            @NotNull
            private RoleAnalysisSessionDetectionOption getRoleAnalysisSessionDetectionOption() {
                RoleAnalysisSessionDetectionOption roleAnalysisSessionDetectionOption = new RoleAnalysisSessionDetectionOption();
                roleAnalysisSessionDetectionOption.setSearchMode(clusterOptions.getSearchMode());
                roleAnalysisSessionDetectionOption.setMinFrequencyThreshold(clusterOptions.getDefaultMinFrequency());
                roleAnalysisSessionDetectionOption.setMaxFrequencyThreshold(clusterOptions.getDefaultMaxFrequency());
                roleAnalysisSessionDetectionOption.setMinOccupancy(clusterOptions.getDefaultOccupancySearch());
                roleAnalysisSessionDetectionOption.setMinPropertyOverlap(clusterOptions.getDefaultIntersectionSearch());
                roleAnalysisSessionDetectionOption.setJaccardSimilarityThreshold(clusterOptions.getDefaultJaccardThreshold());

                return roleAnalysisSessionDetectionOption;
            }

            @NotNull
            private RoleAnalysisSessionFilterOption getRoleAnalysisSessionFilterOption() {
                RoleAnalysisSessionFilterOption roleAnalysisSessionFilterOption = new RoleAnalysisSessionFilterOption();
                if (clusterOptions.getQuery() != null) {
                    roleAnalysisSessionFilterOption.setAxiomFilter(clusterOptions.getQuery().toString());
                }

                roleAnalysisSessionFilterOption.setProcessMode(clusterOptions.getMode());
                roleAnalysisSessionFilterOption.setSimilarityThreshold(clusterOptions.getSimilarity());
                roleAnalysisSessionFilterOption.setMinUniqueGroupCount(clusterOptions.getMinGroupSize());
                roleAnalysisSessionFilterOption.setMinPropertiesCount(clusterOptions.getAssignThreshold());
                roleAnalysisSessionFilterOption.setMinPropertyOverlap(clusterOptions.getMinIntersections());
                return roleAnalysisSessionFilterOption;
            }

            private void importRoleAnalysisClusteringResult(OperationResult result,
                    List<PrismObject<RoleAnalysisCluster>> clusters,
                    RoleAnalysisSessionFilterOption roleAnalysisSessionFilterOption,
                    RoleAnalysisSessionDetectionOption roleAnalysisSessionDetectionOption,
                    String name,
                    RoleAnalysisProcessMode processMode) {

                List<ObjectReferenceType> roleAnalysisClusterRef = new ArrayList<>();

                @NotNull PageBase pageBase = (PageBase) getPage();
                int processedObjectCount = 0;

                QName complexType;
                if (processMode.equals(RoleAnalysisProcessMode.ROLE)) {
                    complexType = RoleType.COMPLEX_TYPE;
                } else {complexType = UserType.COMPLEX_TYPE;}

                double meanDensity = 0;
                for (PrismObject<RoleAnalysisCluster> clusterTypePrismObject : clusters) {
                    meanDensity += Double.parseDouble(clusterTypePrismObject.asObjectable().getPointsDensity());
                    processedObjectCount += clusterTypePrismObject.asObjectable().getElementsCount();

                    ObjectReferenceType objectReferenceType = new ObjectReferenceType();
                    objectReferenceType.setOid(clusterTypePrismObject.getOid());
                    objectReferenceType.setType(complexType);
                    roleAnalysisClusterRef.add(objectReferenceType);
                }

                meanDensity = meanDensity / clusters.size();

                String parentRef = importRoleAnalysisSessionObject(result, pageBase, roleAnalysisSessionFilterOption,
                        roleAnalysisSessionDetectionOption, roleAnalysisClusterRef, processedObjectCount,
                        meanDensity, "", name);

                Task task = pageBase.createSimpleTask("ImportClusterTypeObject");

                try {
                    for (PrismObject<RoleAnalysisCluster> clusterTypePrismObject : clusters) {
                        importRoleAnalysisClusterObject(result, task, ((PageBase) getPage()), clusterTypePrismObject, parentRef);
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Import RoleAnalysisCluster object failed" + e);
                }
            }

        };

        executeClustering.setOutputMarkupId(true);
        executeClustering.setOutputMarkupPlaceholderTag(true);
        executeClustering.setVisible(true);
        executeClustering.add(new EnableBehaviour(() -> !isEditMiningOption() && !isEditClusterOption()));
        add(executeClustering);
    }

    public void defaultIntersectionForm() {

        Form<?> form = new Form<>("default_intersection_option_form");
        form.setOutputMarkupId(true);
        form.setOutputMarkupPlaceholderTag(true);
        form.setVisible(false);
        add(form);

        LabelWithHelpPanel labelMode = new LabelWithHelpPanel("searchMode_label",
                Model.of("Search mode")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.search.mode");
            }
        };
        labelMode.setOutputMarkupId(true);
        form.add(labelMode);


        TextField<Double> thresholdField = new TextField<>(ID_JACCARD_THRESHOLD_FIELD,
                Model.of(clusterOptions.getSimilarity()));
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.add(new EnableBehaviour(this::isEditMiningOptionAndJaccardMode));

        form.add(thresholdField);

        LabelWithHelpPanel thresholdLabel = new LabelWithHelpPanel(ID_JACCARD_THRESHOLD_FIELD + "_label",
                Model.of("Similarity")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.similarity");
            }
        };
        thresholdLabel.setOutputMarkupId(true);
        form.add(thresholdLabel);

        ChoiceRenderer<RoleAnalysisSearchMode> renderer = new ChoiceRenderer<>("value");

        DropDownChoice<RoleAnalysisSearchMode> modeSelector = new DropDownChoice<>(
                "searchModeSelector", Model.of(clusterOptions.getSearchMode()),
                new ArrayList<>(EnumSet.allOf(RoleAnalysisSearchMode.class)), renderer);
        modeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                RoleAnalysisSearchMode modelObject = modeSelector.getModelObject();
                clusterOptions.setSearchMode(modelObject);
                target.add(thresholdField);
            }
        });
        modeSelector.setOutputMarkupId(true);
        modeSelector.setOutputMarkupPlaceholderTag(true);
        modeSelector.setVisible(true);
        modeSelector.add(new EnableBehaviour(this::isEditMiningOption));
        form.add(modeSelector);


        TextField<Integer> intersectionField = new TextField<>("intersectionField",
                Model.of(clusterOptions.getDefaultIntersectionSearch()));
        intersectionField.setOutputMarkupId(true);
        intersectionField.setOutputMarkupPlaceholderTag(true);
        intersectionField.setVisible(true);
        intersectionField.add(new EnableBehaviour(this::isEditMiningOption));
        form.add(intersectionField);
        LabelWithHelpPanel intersectionLabel = new LabelWithHelpPanel("intersection_label",
                Model.of("Min intersection")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.default.intersection");
            }
        };
        intersectionLabel.setOutputMarkupId(true);
        form.add(intersectionLabel);

        TextField<Integer> occupancyField = new TextField<>("minOccupancyField",
                Model.of(clusterOptions.getDefaultOccupancySearch()));
        occupancyField.setOutputMarkupId(true);
        occupancyField.setOutputMarkupPlaceholderTag(true);
        occupancyField.setVisible(true);
        occupancyField.add(new EnableBehaviour(this::isEditMiningOption));

        form.add(occupancyField);
        LabelWithHelpPanel occupancyLabel = new LabelWithHelpPanel("occupancy_label",
                Model.of("Min occupancy")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.default.occupancy");
            }
        };
        occupancyLabel.setOutputMarkupId(true);
        form.add(occupancyLabel);

        TextField<Double> minFrequencyField = new TextField<>("minFrequency",
                Model.of(clusterOptions.getDefaultMinFrequency()));
        minFrequencyField.setOutputMarkupId(true);
        minFrequencyField.setOutputMarkupPlaceholderTag(true);
        minFrequencyField.setVisible(true);
        minFrequencyField.add(new EnableBehaviour(this::isEditMiningOption));
        form.add(minFrequencyField);
        LabelWithHelpPanel minFrequencyLabel = new LabelWithHelpPanel("minFrequency_label",
                Model.of("Min frequency")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.default.min.frequency");
            }
        };
        minFrequencyLabel.setOutputMarkupId(true);
        form.add(minFrequencyLabel);

        TextField<Double> maxFrequencyField = new TextField<>("maxFrequency",
                Model.of(clusterOptions.getDefaultMaxFrequency()));
        maxFrequencyField.setOutputMarkupId(true);
        maxFrequencyField.setOutputMarkupPlaceholderTag(true);
        maxFrequencyField.setVisible(true);
        maxFrequencyField.add(new EnableBehaviour(this::isEditMiningOption));
        form.add(maxFrequencyField);
        LabelWithHelpPanel maxFrequencyLabel = new LabelWithHelpPanel("maxFrequency_label",
                Model.of("Max frequency")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.default.max.frequency");
            }
        };
        maxFrequencyLabel.setOutputMarkupId(true);
        form.add(maxFrequencyLabel);

        AjaxSubmitButton ajaxSubmitButton = new AjaxSubmitButton("submit_default_option_search") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget) {
                if (isEditMiningOption()) {
                    setEditMiningOption(false);
                    clusterOptions.setDefaultIntersectionSearch(intersectionField.getModelObject());
                    clusterOptions.setDefaultOccupancySearch(occupancyField.getModelObject());
                    clusterOptions.setDefaultMinFrequency(minFrequencyField.getModelObject());
                    clusterOptions.setDefaultMaxFrequency(maxFrequencyField.getModelObject());

                    if(clusterOptions.getSearchMode().equals(RoleAnalysisSearchMode.JACCARD)){
                        clusterOptions.setDefaultJaccardThreshold(thresholdField.getModelObject());
                    }

                    intersectionField.setEnabled(false);
                    this.add(AttributeAppender.replace("value",
                            createStringResource("RoleMining.edit.options.mining")));
                    this.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
                } else {
                    setEditMiningOption(true);
                    this.add(AttributeAppender.replace("value",
                            createStringResource("RoleMining.save.options.mining")));
                    this.add(AttributeAppender.replace("class", "btn btn-primary btn-sm"));
                }

                ajaxRequestTarget.add(executeClustering);
                ajaxRequestTarget.add(intersectionField);
                ajaxRequestTarget.add(occupancyField);
                ajaxRequestTarget.add(minFrequencyField);
                ajaxRequestTarget.add(maxFrequencyField);
                ajaxRequestTarget.add(modeSelector);
                ajaxRequestTarget.add(this);

            }
        };
        ajaxSubmitButton.setOutputMarkupId(true);
        ajaxSubmitButton.setOutputMarkupPlaceholderTag(true);
        form.add(ajaxSubmitButton);

        add(form);
        AjaxButton showAdditionalOptions = showDefaultMiningPanel(form);
        add(showAdditionalOptions);

    }

    @NotNull
    private AjaxButton showDefaultMiningPanel(Form<?> form) {
        AjaxButton showAdditionalOptions = new AjaxButton(ID_SHOW_MINING_PANEL) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                form.setVisible(!form.isVisible());
                target.add(form);
                target.add(this);
            }

            @Override
            public IModel<?> getBody() {
                return getNameOfMiningOptionsButton(form.isVisible());
            }
        };

        showAdditionalOptions.setOutputMarkupId(true);
        showAdditionalOptions.add(AttributeAppender.append("style", "cursor: pointer;"));
        return showAdditionalOptions;
    }

    @NotNull
    private AjaxButton showClusterPanel(Form<?> form) {
        AjaxButton showAdditionalOptions = new AjaxButton(ID_SHOW_CLUSTER_PANEL) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                form.setVisible(!form.isVisible());
                target.add(form);
                target.add(this);
            }

            @Override
            public IModel<?> getBody() {
                return getNameOfClusterOptionsButton(form.isVisible());
            }
        };

        showAdditionalOptions.setOutputMarkupId(true);
        showAdditionalOptions.add(AttributeAppender.append("style", "cursor: pointer;"));
        return showAdditionalOptions;
    }

    public Form<?> clusterForm() {

        Form<?> form = new Form<Void>(ID_EXECUTE_CLUSTERING_FORM);
        form.setOutputMarkupId(true);
        form.setOutputMarkupPlaceholderTag(true);
        form.setVisible(true);

        LabelWithHelpPanel labelMode = new LabelWithHelpPanel("modeSelector_label",
                Model.of("Process mode")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.mode");
            }
        };
        labelMode.setOutputMarkupId(true);
        form.add(labelMode);

        ChoiceRenderer<RoleAnalysisProcessMode> renderer = new ChoiceRenderer<>("value");

        DropDownChoice<RoleAnalysisProcessMode> modeSelector = new DropDownChoice<>(
                "modeSelector", Model.of(clusterOptions.getMode()),
                new ArrayList<>(EnumSet.allOf(RoleAnalysisProcessMode.class)), renderer);
        modeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                clusterOptions.setMode(modeSelector.getModelObject());
            }
        });
        modeSelector.setOutputMarkupId(true);
        modeSelector.setOutputMarkupPlaceholderTag(true);
        modeSelector.setVisible(true);
        modeSelector.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(modeSelector);

        clusterOptions.setName("p_cluster_" + (countParentClusterTypeObjects((PageBase) getPage()) + 1));

        TextField<String> nameField = new TextField<>(ID_NAME_FIELD,
                Model.of(clusterOptions.getName()));
        nameField.setOutputMarkupId(true);
        nameField.setOutputMarkupPlaceholderTag(true);
        nameField.setVisible(true);
        nameField.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(nameField);

        LabelWithHelpPanel labelName = new LabelWithHelpPanel(ID_NAME_FIELD + "_label",
                Model.of("Name")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.name");
            }
        };
        labelName.setOutputMarkupId(true);
        form.add(labelName);

        TextField<Double> thresholdField = new TextField<>(ID_JACCARD_THRESHOLD_FIELD,
                Model.of(clusterOptions.getSimilarity()));
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        thresholdField.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(thresholdField);

        LabelWithHelpPanel thresholdLabel = new LabelWithHelpPanel(ID_JACCARD_THRESHOLD_FIELD + "_label",
                Model.of("Similarity")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.similarity");
            }
        };
        thresholdLabel.setOutputMarkupId(true);
        form.add(thresholdLabel);

        TextField<Integer> minIntersectionField = new TextField<>(ID_INTERSECTION_THRESHOLD_FIELD,
                Model.of(clusterOptions.getMinIntersections()));
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        minIntersectionField.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(minIntersectionField);

        LabelWithHelpPanel intersectionLabel = new LabelWithHelpPanel(ID_INTERSECTION_THRESHOLD_FIELD + "_label",
                Model.of("Min intersection")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.intersection");
            }
        };
        intersectionLabel.setOutputMarkupId(true);
        form.add(intersectionLabel);

        TextField<Integer> minAssign = new TextField<>(ID_MIN_ASSIGN,
                Model.of(clusterOptions.getAssignThreshold()));
        minAssign.setOutputMarkupId(true);
        minAssign.setOutputMarkupPlaceholderTag(true);
        minAssign.setVisible(true);
        minAssign.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(minAssign);

        LabelWithHelpPanel assignmentsLabel = new LabelWithHelpPanel(ID_MIN_ASSIGN + "_label",
                Model.of("Min properties")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.min.assign");
            }
        };
        assignmentsLabel.setOutputMarkupId(true);
        form.add(assignmentsLabel);

        TextField<Integer> minGroupField = new TextField<>(ID_GROUP_THRESHOLD_FIELD,
                Model.of(clusterOptions.getMinGroupSize()));
        minGroupField.setOutputMarkupId(true);
        minGroupField.setOutputMarkupPlaceholderTag(true);
        minGroupField.setVisible(true);
        minGroupField.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(minGroupField);

        LabelWithHelpPanel groupLabel = new LabelWithHelpPanel(ID_GROUP_THRESHOLD_FIELD + "_label",
                Model.of("Min members")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.min.members");
            }
        };
        groupLabel.setOutputMarkupId(true);
        form.add(groupLabel);

        objectFiltersPanel(form);
        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink("ajax_submit_cluster_parameter", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                if (isEditClusterOption()) {
                    setEditClusterOption(false);
                    clusterOptions.setName(nameField.getModelObject());
                    clusterOptions.setSimilarity(thresholdField.getModelObject());
                    clusterOptions.setMinIntersections(minIntersectionField.getModelObject());
                    clusterOptions.setMinGroupSize(minGroupField.getModelObject());
                    clusterOptions.setAssignThreshold(minAssign.getModelObject());
                    this.add(AttributeAppender.replace("value",
                            createStringResource("RoleMining.edit.options.cluster")));
                    this.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
                } else {
                    setEditClusterOption(true);
                    this.add(AttributeAppender.replace("value",
                            createStringResource("RoleMining.save.options.cluster")));
                    this.add(AttributeAppender.replace("class", "btn btn-primary btn-sm"));
                }

                target.add(filterSubmitButton);
                target.add(executeClustering);
                target.add(nameField);
                target.add(minAssign);
                target.add(thresholdField);
                target.add(minIntersectionField);
                target.add(minGroupField);
                target.add(this);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);
        form.add(ajaxSubmitLink);

        return form;
    }

    private void objectFiltersPanel(@NotNull WebMarkupContainer panel) {

        OperationResultPanel operationResultPanel = new OperationResultPanel(ID_WARNING_FEEDBACK,
                new LoadableModel<>() {
                    @Override
                    protected OpResult load() {
                        if (result == null) {
                            result = new OperationResult(getString(("roleMiningExportPanel.operation.query.parse")));
                        }
                        return OpResult.getOpResult(getPageBase(), result);
                    }
                }) {
            @Override
            public void close(@NotNull AjaxRequestTarget target, boolean parent) {
                target.add(this.setVisible(false));
            }
        };
        operationResultPanel.setOutputMarkupPlaceholderTag(true);
        operationResultPanel.setOutputMarkupId(true);
        operationResultPanel.setVisible(false);
        panel.add(operationResultPanel);

        initFilter(panel, operationResultPanel);
    }

    private void initFilter(@NotNull WebMarkupContainer panel,
            OperationResultPanel operationResultPanel) {

        Form<?> filterForm = new Form<>("userFilter");
        filterForm.setOutputMarkupId(true);
        filterForm.setOutputMarkupPlaceholderTag(true);
        filterForm.setVisible(false);

        LabelWithHelpPanel label = new LabelWithHelpPanel("filter_field_label",
                createStringResource("roleMiningClusterPanel.query.label.title")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.option.filter");
            }
        };
        label.setOutputMarkupId(true);
        panel.add(label);

        NonEmptyModel<AxiomQueryWrapper> filterModel = new NonEmptyWrapperModel<>(
                new Model<>(new AxiomQueryWrapper(null)));

        TextField<String> queryDslField = new TextField<>("filter_field",
                new PropertyModel<>(filterModel, AxiomQueryWrapper.F_DSL_QUERY));
        queryDslField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        queryDslField.add(AttributeAppender.append("placeholder",
                createStringResource("roleMiningExportPanel.insertAxiomQuery")));
        filterForm.add(queryDslField);
        panel.add(filterForm);

        filterSubmitButton = new AjaxSubmitButton("filterForm_submit") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                if (filterForm.isVisible()) {

                    if (filterModel.getObject().getDslQuery() != null
                            && !filterModel.getObject().getDslQuery().isEmpty()) {
                        String midPointQuery = filterModel.getObject().getDslQuery();

                        try {
                            clusterOptions.setQuery(getPrismContext().createQueryParser()
                                    .parseFilter(FocusType.class, midPointQuery));
                            filterForm.setVisible(false);
                            this.add(AttributeAppender.replace("class", " btn btn-success btn-sm"));
                        } catch (CommonException | RuntimeException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, getString(
                                    "roleMiningExportPanel.message.couldNotExecuteQuery"), e);
                            OperationResult result = new OperationResult(getString(("roleMiningExportPanel.operation.query.parse")));

                            result.setMessage(getString("roleMiningExportPanel.result.failed.filter"));
                            result.recordFatalError(getString("roleMiningExportPanel.message.couldNotExecuteQuery"), e);

                            showResultFeedback(result, operationResultPanel, target);
                            this.add(AttributeAppender.replace("class", " btn btn-danger btn-sm"));
                            target.add(this);
                        }
                    } else {
                        filterForm.setVisible(false);
                        this.add(AttributeAppender.replace("class", " btn btn-default btn-sm"));
                    }
                } else {
                    operationResultPanel.setVisible(false);
                    target.add(operationResultPanel);
                    filterForm.setVisible(true);
                    this.add(AttributeAppender.replace("class", " btn btn-primary btn-sm"));
                }

                target.add(filterForm);
                target.add(this);
            }

            @Override
            public IModel<?> getBody() {
                if (filterForm.isVisible()) {
                    return createStringResource("roleMiningExportPanel.save");
                } else {
                    return createStringResource("roleMiningExportPanel.filter.options");
                }
            }
        };

        filterSubmitButton.setOutputMarkupId(true);
        filterSubmitButton.add(AttributeAppender.append("style", "cursor: pointer;"));

        filterSubmitButton.setOutputMarkupId(true);
        filterSubmitButton.setOutputMarkupPlaceholderTag(true);
        filterSubmitButton.setVisible(true);
        filterSubmitButton.add(new EnableBehaviour(this::isEditClusterOption));
        panel.add(filterSubmitButton);
    }

    private void showResultFeedback(OperationResult result, @NotNull Component resultPanel, @NotNull AjaxRequestTarget target) {
        this.result = result;
        resultPanel.setVisible(true);
        target.add(resultPanel);
    }

    private StringResourceModel getNameOfMiningOptionsButton(boolean visible) {
        return createStringResource("RoleMining.mining.panel.showAdditionalOptions.button." + !visible);
    }

    private StringResourceModel getNameOfClusterOptionsButton(boolean visible) {
        return createStringResource("RoleMining.cluster.panel.showAdditionalOptions.button." + !visible);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    private boolean isEditMiningOption() {
        return editMiningOption;
    }


    private boolean isEditMiningOptionAndJaccardMode() {
        return editMiningOption && clusterOptions.getSearchMode().equals(RoleAnalysisSearchMode.JACCARD) ;
    }

    private void setEditMiningOption(boolean editMiningOption) {
        this.editMiningOption = editMiningOption;
    }

    public boolean isEditClusterOption() {
        return editClusterOption;
    }

    public void setEditClusterOption(boolean editClusterOption) {
        this.editClusterOption = editClusterOption;
    }

    @Override
    public int getWidth() {
        return 1400;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("ClusterPanel.panel.title");
    }
}
