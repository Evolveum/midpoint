/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.model.api.expr.MidpointFunctions.LOGGER;

import java.io.Serial;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.RangeSliderPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.ClusteringAction;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
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
    private static final String ID_MAX_ASSIGN = "assign_max_occupy";

    private static final String ID_GROUP_THRESHOLD_FIELD = "group_min_cluster";
    private static final String ID_MIN_MEMBERS_THRESHOLD_FIELD = "min_members";
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

        this.clusterOptions = new ClusterOptions((PageBase) getPage(),
                RoleAnalysisProcessModeType.USER,
                RoleAnalysisDetectionModeType.INTERSECTION);

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

                ClusteringAction clusteringAction = new ClusteringAction(clusterOptions.getMode());
                List<PrismObject<RoleAnalysisClusterType>> clusters = clusteringAction.execute(clusterOptions);

                RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption = getRoleAnalysisSessionFilterOption();

                RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption = getRoleAnalysisSessionDetectionOption();

                importRoleAnalysisClusteringResult(importResult, clusters, roleAnalysisSessionClusterOption,
                        roleAnalysisSessionDetectionOption, clusterOptions.getName(), clusterOptions.getMode());

                getPageBase().hideMainPopup(ajaxRequestTarget);
            }

            @NotNull
            private RoleAnalysisDetectionOptionType getRoleAnalysisSessionDetectionOption() {
                RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption = new RoleAnalysisDetectionOptionType();
                roleAnalysisSessionDetectionOption.setDetectionMode(clusterOptions.getSearchMode());
                roleAnalysisSessionDetectionOption.setMinFrequencyThreshold(clusterOptions.getDefaultMinFrequency());
                roleAnalysisSessionDetectionOption.setMaxFrequencyThreshold(clusterOptions.getDefaultMaxFrequency());
                roleAnalysisSessionDetectionOption.setMinMembersOccupancy(clusterOptions.getDefaultOccupancySearch());
                roleAnalysisSessionDetectionOption.setMinPropertiesOccupancy(clusterOptions.getDefaultIntersectionSearch());
                roleAnalysisSessionDetectionOption.setJaccardSimilarityThreshold(clusterOptions.getDefaultJaccardThreshold());

                return roleAnalysisSessionDetectionOption;
            }

            @NotNull
            private RoleAnalysisSessionOptionType getRoleAnalysisSessionFilterOption() {
                RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption = new RoleAnalysisSessionOptionType();
                if (clusterOptions.getQuery() != null) {
                    roleAnalysisSessionClusterOption.setFilter(clusterOptions.getQuery().toString());
                }

                roleAnalysisSessionClusterOption.setProcessMode(clusterOptions.getMode());
                roleAnalysisSessionClusterOption.setSimilarityThreshold(clusterOptions.getSimilarity());
                roleAnalysisSessionClusterOption.setMinUniqueMembersCount(clusterOptions.getMinGroupSize());
                roleAnalysisSessionClusterOption.setMinMembersCount(clusterOptions.getMinMembers());
                roleAnalysisSessionClusterOption.setMinPropertiesCount(clusterOptions.getMinProperties());
                roleAnalysisSessionClusterOption.setMaxPropertiesCount(clusterOptions.getMaxProperties());
                roleAnalysisSessionClusterOption.setMinPropertiesOverlap(clusterOptions.getMinIntersections());
                return roleAnalysisSessionClusterOption;
            }

            private void importRoleAnalysisClusteringResult(OperationResult result,
                    List<PrismObject<RoleAnalysisClusterType>> clusters,
                    RoleAnalysisSessionOptionType roleAnalysisSessionClusterOption,
                    RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption,
                    String name,
                    RoleAnalysisProcessModeType processMode) {

                List<ObjectReferenceType> roleAnalysisClusterRef = new ArrayList<>();

                @NotNull PageBase pageBase = (PageBase) getPage();
                int processedObjectCount = 0;

                QName complexType;
                if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                    complexType = RoleType.COMPLEX_TYPE;
                } else {complexType = UserType.COMPLEX_TYPE;}

                double meanDensity = 0;
                for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
                    RoleAnalysisClusterStatisticType clusterStatistic = clusterTypePrismObject.asObjectable().getClusterStatistic();
                    meanDensity += clusterStatistic.getPropertiesDensity();
                    processedObjectCount += clusterStatistic.getMemberCount();

                    ObjectReferenceType objectReferenceType = new ObjectReferenceType();
                    objectReferenceType.setOid(clusterTypePrismObject.getOid());
                    objectReferenceType.setType(complexType);
                    roleAnalysisClusterRef.add(objectReferenceType);
                }

                meanDensity = meanDensity / clusters.size();

                RoleAnalysisSessionStatisticType roleAnalysisSessionStatisticType = new RoleAnalysisSessionStatisticType();
                roleAnalysisSessionStatisticType.setProcessedObjectCount(processedObjectCount);
                roleAnalysisSessionStatisticType.setMeanDensity(meanDensity);

                ObjectReferenceType parentRef = importRoleAnalysisSessionObject(result, pageBase, roleAnalysisSessionClusterOption,
                        roleAnalysisSessionStatisticType, roleAnalysisClusterRef, name);

                Task task = pageBase.createSimpleTask("ImportClusterTypeObject");

                try {
                    for (PrismObject<RoleAnalysisClusterType> clusterTypePrismObject : clusters) {
                        importRoleAnalysisClusterObject(result, task, ((PageBase) getPage()), clusterTypePrismObject, parentRef, roleAnalysisSessionDetectionOption);
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

    LabelWithHelpPanel intersectionLabel;
    LabelWithHelpPanel occupancyLabel;
    LabelWithHelpPanel minFrequencyLabel;

    public Form<?> getDefaultDetectionForm() {
        return (Form<?>) get("default_intersection_option_form");
    }
    public void defaultIntersectionForm() {

        Form<?> defaultDetectionForm = new Form<>("default_intersection_option_form");
        defaultDetectionForm.setOutputMarkupId(true);
        defaultDetectionForm.setOutputMarkupPlaceholderTag(true);
        defaultDetectionForm.setVisible(false);
        add(defaultDetectionForm);

        LabelWithHelpPanel labelMode = new LabelWithHelpPanel("searchMode_label",
                Model.of("Search mode")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.cluster.option.detection.mode");
            }
        };
        labelMode.setOutputMarkupId(true);
        defaultDetectionForm.add(labelMode);

        TextField<Double> defaultDetectionSimilarityField = new TextField<>(ID_JACCARD_THRESHOLD_FIELD,
                new LoadableModel<>() {
                    @Override
                    protected Double load() {
                        return clusterOptions.getDefaultJaccardThreshold();
                    }
                });
        defaultDetectionSimilarityField.setOutputMarkupId(true);
        defaultDetectionSimilarityField.setOutputMarkupPlaceholderTag(true);
        defaultDetectionSimilarityField.add(new EnableBehaviour(this::isEditMiningOptionAndJaccardMode));

        defaultDetectionForm.add(defaultDetectionSimilarityField);

        LabelWithHelpPanel thresholdLabel = new LabelWithHelpPanel(ID_JACCARD_THRESHOLD_FIELD + "_label",
                Model.of("Similarity")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.cluster.option.similarity");
            }
        };
        thresholdLabel.setOutputMarkupId(true);
        defaultDetectionForm.add(thresholdLabel);

        TextField<Integer> intersectionField = new TextField<>("intersectionField",
                new LoadableModel<>() {
                    @Override
                    protected Integer load() {
                        return clusterOptions.getDefaultIntersectionSearch();
                    }
                });
        intersectionField.setOutputMarkupId(true);
        intersectionField.setOutputMarkupPlaceholderTag(true);
        intersectionField.setVisible(true);
        intersectionField.add(new EnableBehaviour(this::isEditMiningOption));
        defaultDetectionForm.add(intersectionField);
        intersectionLabel = new LabelWithHelpPanel("intersection_label",
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getIntersectionHeaderTitle();
                    }
                }) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.cluster.option.min.members.occupancy");
            }
        };
        intersectionLabel.setOutputMarkupId(true);
        intersectionLabel.setOutputMarkupPlaceholderTag(true);
        defaultDetectionForm.add(intersectionLabel);

        TextField<Integer> occupancyField = new TextField<>("minOccupancyField",
                new LoadableModel<Integer>() {
                    @Override
                    protected Integer load() {
                        return clusterOptions.getDefaultOccupancySearch();
                    }
                });
        occupancyField.setOutputMarkupId(true);
        occupancyField.setOutputMarkupPlaceholderTag(true);
        occupancyField.setVisible(true);
        occupancyField.add(new EnableBehaviour(this::isEditMiningOption));

        defaultDetectionForm.add(occupancyField);
        occupancyLabel = new LabelWithHelpPanel("occupancy_label",
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getOccupancyHeaderTitle();
                    }
                }) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.cluster.option.min.properties.occupancy");
            }
        };
        occupancyLabel.setOutputMarkupId(true);
        occupancyLabel.setOutputMarkupPlaceholderTag(true);
        defaultDetectionForm.add(occupancyLabel);

        TextField<Double> minFrequencyField = new TextField<>("minFrequency",
                new LoadableModel<>() {
                    @Override
                    protected Double load() {
                        return clusterOptions.getDefaultMinFrequency();
                    }
                });
        minFrequencyField.setOutputMarkupId(true);
        minFrequencyField.setOutputMarkupPlaceholderTag(true);
        minFrequencyField.setVisible(true);
        minFrequencyField.add(new EnableBehaviour(this::isEditMiningOption));
        defaultDetectionForm.add(minFrequencyField);
        minFrequencyLabel = new LabelWithHelpPanel("minFrequency_label",
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getMinFrequencyHeaderTitle();
                    }
                }) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.cluster.option.frequency.range");
            }
        };
        minFrequencyLabel.setOutputMarkupId(true);
        minFrequencyField.setOutputMarkupPlaceholderTag(true);
        defaultDetectionForm.add(minFrequencyLabel);

        TextField<Double> maxFrequencyField = new TextField<>("maxFrequency",
                new LoadableModel<>() {
                    @Override
                    protected Double load() {
                        return clusterOptions.getDefaultMaxFrequency();
                    }
                });
        maxFrequencyField.setOutputMarkupId(true);
        maxFrequencyField.setOutputMarkupPlaceholderTag(true);
        maxFrequencyField.setVisible(true);
        maxFrequencyField.add(new EnableBehaviour(this::isEditMiningOption));
        defaultDetectionForm.add(maxFrequencyField);

        LabelWithHelpPanel labelDetectMode = new LabelWithHelpPanel("detect_label",
                Model.of("Process detection")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.cluster.option.detection.mode");
            }
        };
        labelDetectMode.setOutputMarkupId(true);
        defaultDetectionForm.add(labelDetectMode);

        ChoiceRenderer<DETECT> rendererDetect = new ChoiceRenderer<>("displayString");

        if (clusterOptions.getDetect() == null) {
            clusterOptions.setDetect(DETECT.PARTIAL);
        }
        DropDownChoice<DETECT> modeSelectorDetect = new DropDownChoice<>(
                "detect", Model.of(clusterOptions.getDetect()),
                new ArrayList<>(EnumSet.allOf(DETECT.class)), rendererDetect);
        modeSelectorDetect.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                DETECT modelObject = modeSelectorDetect.getModelObject();
                clusterOptions.setDetect(modelObject);
            }
        });

        modeSelectorDetect.setOutputMarkupId(true);
        modeSelectorDetect.setOutputMarkupPlaceholderTag(true);
        modeSelectorDetect.setVisible(true);
        modeSelectorDetect.add(new EnableBehaviour(this::isEditMiningOption));
        defaultDetectionForm.add(modeSelectorDetect);

        ChoiceRenderer<RoleAnalysisDetectionModeType> renderer = new ChoiceRenderer<>("value");

        DropDownChoice<RoleAnalysisDetectionModeType> searchModeSelector = new DropDownChoice<>(
                "searchModeSelector", Model.of(clusterOptions.getSearchMode()),
                new ArrayList<>(EnumSet.allOf(RoleAnalysisDetectionModeType.class)), renderer);
        searchModeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                RoleAnalysisDetectionModeType modelObject = searchModeSelector.getModelObject();
                clusterOptions.setSearchMode(modelObject);
                if (modelObject.equals(RoleAnalysisDetectionModeType.JACCARD)) {
                    defaultDetectionSimilarityField.setVisible(true);
                    target.add(defaultDetectionSimilarityField);
                } else {
                    defaultDetectionSimilarityField.setVisible(false);
                    target.add(defaultDetectionSimilarityField);
                }
            }
        });
        searchModeSelector.setOutputMarkupId(true);
        searchModeSelector.setOutputMarkupPlaceholderTag(true);
        searchModeSelector.setVisible(true);
        searchModeSelector.add(new EnableBehaviour(this::isEditMiningOption));
        defaultDetectionForm.add(searchModeSelector);

        AjaxSubmitButton ajaxSubmitButton = new AjaxSubmitButton("submit_default_option_search") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget) {
                if (isEditClusterOption()) {
                    return;
                }

                if (isEditMiningOption()) {
                    setEditMiningOption(false);
                    int intersection = Integer.parseInt(String.valueOf(intersectionField.getModelObject()));
                    int occupancy = Integer.parseInt(String.valueOf(occupancyField.getModelObject()));
                    double minFrequency = Double.parseDouble(String.valueOf(minFrequencyField.getModelObject()));
                    double maxFrequency = Double.parseDouble(String.valueOf(maxFrequencyField.getModelObject()));
                    double similarity = Double.parseDouble(String.valueOf(defaultDetectionSimilarityField.getModelObject()));

                    clusterOptions.setDefaultIntersectionSearch(intersection);
                    clusterOptions.setDefaultOccupancySearch(occupancy);
                    clusterOptions.setDefaultMinFrequency(minFrequency);
                    clusterOptions.setDefaultMaxFrequency(maxFrequency);

                    if (clusterOptions.getSearchMode().equals(RoleAnalysisDetectionModeType.JACCARD)) {
                        clusterOptions.setDefaultJaccardThreshold(similarity);
                    }

                    this.add(AttributeAppender.replace("value",
                            createStringResource("RoleMining.edit.options.mining")));
                    this.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
                } else {
                    setEditMiningOption(true);
                    this.add(AttributeAppender.replace("value",
                            createStringResource("RoleMining.save.options.mining")));
                    this.add(AttributeAppender.replace("class", "btn btn-primary btn-sm"));
                }

                ajaxRequestTarget.add(defaultDetectionSimilarityField);
                ajaxRequestTarget.add(executeClustering);
                ajaxRequestTarget.add(intersectionField);
                ajaxRequestTarget.add(occupancyField);
                ajaxRequestTarget.add(minFrequencyField);
                ajaxRequestTarget.add(maxFrequencyField);
                ajaxRequestTarget.add(searchModeSelector);
                ajaxRequestTarget.add(modeSelectorDetect);
                ajaxRequestTarget.add(this);

            }
        };
        ajaxSubmitButton.setOutputMarkupId(true);
        ajaxSubmitButton.setOutputMarkupPlaceholderTag(true);
        defaultDetectionForm.add(ajaxSubmitButton);

        add(defaultDetectionForm);
        AjaxButton showAdditionalOptions = showDefaultMiningPanel(defaultDetectionForm);
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
                return createStringResource("RoleMining.session.option.process.mode");
            }
        };
        labelMode.setOutputMarkupId(true);
        form.add(labelMode);

        clusterOptions.setName("session_" + (countParentClusterTypeObjects((PageBase) getPage()) + 1));

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

        RangeSliderPanel rangeSliderPanel = new RangeSliderPanel(ID_JACCARD_THRESHOLD_FIELD) {
            @Override
            public int getMinValue() {
                return 10;
            }

            @Override
            public int getSliderWidth() {
                return 90;
            }

            @Override
            public String getSliderWidthUnit() {
                return "%";
            }

            @Override
            public int getMaxValue() {
                return 100;
            }

            @Override
            public int getDefaultValue() {
                return 80;
            }
        };
        rangeSliderPanel.setOutputMarkupId(true);
        rangeSliderPanel.setOutputMarkupPlaceholderTag(true);
        rangeSliderPanel.setVisible(true);
        rangeSliderPanel.add(new EnableBehaviour(this::isEditClusterOption));

//        TextField<Double> thresholdField = new TextField<>(ID_JACCARD_THRESHOLD_FIELD,
//                Model.of(clusterOptions.getSimilarity()));
//        thresholdField.setOutputMarkupId(true);
//        thresholdField.setOutputMarkupPlaceholderTag(true);
//        thresholdField.setVisible(true);
//        thresholdField.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(rangeSliderPanel);

        LabelWithHelpPanel thresholdLabel = new LabelWithHelpPanel(ID_JACCARD_THRESHOLD_FIELD + "_label",
                Model.of("Similarity")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.session.option.similarity");
            }
        };
        thresholdLabel.setOutputMarkupId(true);
        thresholdLabel.setOutputMarkupPlaceholderTag(true);
        form.add(thresholdLabel);

        TextField<Integer> minIntersectionField = new TextField<>(ID_INTERSECTION_THRESHOLD_FIELD,
                Model.of(clusterOptions.getMinIntersections()));
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        minIntersectionField.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(minIntersectionField);

        LabelWithHelpPanel overlapLabel = new LabelWithHelpPanel(ID_INTERSECTION_THRESHOLD_FIELD + "_label",
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getOverlapHeaderTitle();
                    }
                }) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.session.option.min.overlap");
            }
        };
        overlapLabel.setOutputMarkupId(true);
        overlapLabel.setOutputMarkupPlaceholderTag(true);
        form.add(overlapLabel);

        TextField<Integer> minAssign = new TextField<>(ID_MIN_ASSIGN,
                Model.of(clusterOptions.getMinProperties()));
        minAssign.setOutputMarkupId(true);
        minAssign.setOutputMarkupPlaceholderTag(true);
        minAssign.setVisible(true);
        minAssign.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(minAssign);

        int defaultThreshold = 1000;
        clusterOptions.setMaxProperties(defaultThreshold);
        TextField<Integer> maxAssign = new TextField<>(ID_MAX_ASSIGN,
                Model.of(clusterOptions.getMaxProperties()));
        maxAssign.setOutputMarkupId(true);
        maxAssign.setOutputMarkupPlaceholderTag(true);
        maxAssign.setVisible(true);
        maxAssign.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(maxAssign);

        LabelWithHelpPanel assignmentsLabel = new LabelWithHelpPanel(ID_MIN_ASSIGN + "_label",
                new LoadableModel<>() {
                    @Override
                    protected String load() {
                        return getPropertyRangeHeaderTitle();
                    }
                }) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.session.option.properties.range");
            }
        };
        assignmentsLabel.setOutputMarkupId(true);
        assignmentsLabel.setOutputMarkupPlaceholderTag(true);
        form.add(assignmentsLabel);

        TextField<Integer> minGroupField = new TextField<>(ID_GROUP_THRESHOLD_FIELD,
                Model.of(clusterOptions.getMinGroupSize()));
        minGroupField.setOutputMarkupId(true);
        minGroupField.setOutputMarkupPlaceholderTag(true);
        minGroupField.setVisible(true);
        minGroupField.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(minGroupField);

        LabelWithHelpPanel minMembersLabel = new LabelWithHelpPanel(ID_MIN_MEMBERS_THRESHOLD_FIELD + "_label",
                Model.of("Min members count")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.session.option.min.members");
            }
        };
        minMembersLabel.setOutputMarkupId(true);
        minMembersLabel.setOutputMarkupPlaceholderTag(true);
        form.add(minMembersLabel);

        TextField<Integer> minMembers = new TextField<>(ID_MIN_MEMBERS_THRESHOLD_FIELD,
                Model.of(clusterOptions.getMinMembers()));
        minMembers.setOutputMarkupId(true);
        minMembers.setOutputMarkupPlaceholderTag(true);
        minMembers.setVisible(true);
        minMembers.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(minMembers);

        LabelWithHelpPanel groupLabel = new LabelWithHelpPanel(ID_GROUP_THRESHOLD_FIELD + "_label",
                Model.of("Min unique members")) {
            @Override
            protected IModel<String> getHelpModel() {
                return createStringResource("RoleMining.session.option.min.unique.members");
            }
        };
        groupLabel.setOutputMarkupId(true);
        groupLabel.setOutputMarkupPlaceholderTag(true);
        form.add(groupLabel);

        objectFiltersPanel(form);

        ChoiceRenderer<RoleAnalysisProcessModeType> renderer = new ChoiceRenderer<>("value");

        DropDownChoice<RoleAnalysisProcessModeType> modeSelector = new DropDownChoice<>(
                "modeSelector", Model.of(clusterOptions.getMode()),
                new ArrayList<>(EnumSet.allOf(RoleAnalysisProcessModeType.class)), renderer);
        modeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                clusterOptions.setMode(modeSelector.getModelObject());

                target.add(overlapLabel);
                target.add(assignmentsLabel);
                target.add(minMembers);

                target.add(intersectionLabel);
                target.add(occupancyLabel);
                target.add(minFrequencyLabel);
            }
        });
        modeSelector.setOutputMarkupId(true);
        modeSelector.setOutputMarkupPlaceholderTag(true);
        modeSelector.setVisible(true);
        modeSelector.add(new EnableBehaviour(this::isEditClusterOption));
        form.add(modeSelector);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink("ajax_submit_cluster_parameter", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                if(isEditMiningOption()){
                    return;
                }

                if (isEditClusterOption()) {
                    setEditClusterOption(false);
                    clusterOptions.setName(nameField.getModelObject());
                    double similarity = Integer.parseInt(rangeSliderPanel.getSliderFormComponent().getValue()) / (double) 100;
                    clusterOptions.setSimilarity(similarity);
                    clusterOptions.setSimilarity(similarity);
                    clusterOptions.setMinIntersections(minIntersectionField.getModelObject());
                    clusterOptions.setMinGroupSize(minGroupField.getModelObject());
                    clusterOptions.setMinMembers(minMembers.getModelObject());
                    clusterOptions.setMinProperties(minAssign.getModelObject());
                    clusterOptions.setMaxProperties(maxAssign.getModelObject());

                    clusterOptions.setDefaultIntersectionSearch(minMembers.getModelObject());
                    clusterOptions.setDefaultOccupancySearch(minIntersectionField.getModelObject());
                    clusterOptions.setDefaultMinFrequency(0.3);
                    clusterOptions.setDefaultMaxFrequency(1.0);
                    clusterOptions.setDefaultJaccardThreshold(similarity);

                    target.add(getDefaultDetectionForm());

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
                target.add(minMembers);
                target.add(minAssign);
                target.add(maxAssign);
                target.add(rangeSliderPanel);
                target.add(minIntersectionField);
                target.add(minGroupField);
                target.add(modeSelector);
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
                return createStringResource("RoleMining.session.option.axiom.query");
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
        return editMiningOption && clusterOptions.getSearchMode().equals(RoleAnalysisDetectionModeType.JACCARD);
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

    protected String getIntersectionHeaderTitle() {
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return getString("RoleMining.cluster.table.column.header.role.occupation");
        } else {
            return getString("RoleMining.cluster.table.column.header.user.occupation");
        }
    }

    protected String getOccupancyHeaderTitle() {
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return getString("RoleMining.cluster.table.column.header.user.occupation");
        } else {
            return getString("RoleMining.cluster.table.column.header.role.occupation");
        }
    }

    protected String getMinFrequencyHeaderTitle() {
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return getString("RoleMining.cluster.table.column.header.user.frequency.min");
        } else {
            return getString("RoleMining.cluster.table.column.header.role.frequency.min");
        }
    }

    protected String getMaxFrequencyHeaderTitle() {
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return getString("RoleMining.cluster.table.column.header.user.frequency.max");
        } else {
            return getString("RoleMining.cluster.table.column.header.role.frequency.max");
        }
    }

    protected String getOverlapHeaderTitle() {
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return getString("RoleMining.cluster.table.column.header.user.overlap");
        } else {
            return getString("RoleMining.cluster.table.column.header.role.overlap");
        }
    }

    protected String getPropertyRangeHeaderTitle() {
        if (clusterOptions.getMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            return "Property range (USER)";
        } else {
            return "Property range (ROLE)";
        }
    }

}
