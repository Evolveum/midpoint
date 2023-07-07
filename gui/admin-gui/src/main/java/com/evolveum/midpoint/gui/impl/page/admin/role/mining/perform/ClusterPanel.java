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

import com.github.openjson.JSONObject;
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
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class ClusterPanel extends BasePanel<String> implements Popupable {
    private static final String ID_EXECUTE_CLUSTERING_FORM = "thresholds_form_cluster";
    private static final String ID_WARNING_FEEDBACK = "warningFeedback";
    private static final String ID_JACCARD_THRESHOLD_FIELD = "eps_cluster";

    private static final String ID_NAME_FIELD = "name_field";

    private static final String ID_INTERSECTION_THRESHOLD_FIELD = "intersection_field_min_cluster";
    private static final String ID_MIN_ASSIGN = "assign_min_occupy";
    private static final String ID_GROUP_THRESHOLD_FIELD = "group_min_cluster";
    private static final String ID_SUBMIT_BUTTON = "ajax_submit_link_cluster";
    private static final String ID_IDENTIFIER_FIELD = "identifier_field";

    OperationResult result;
    ClusterOptions clusterOptions;

    public ClusterPanel(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        this.clusterOptions = new ClusterOptions((PageBase) getPage(), Mode.USER);
        add(clusterForm());

        WebMarkupContainer webMarkupContainer = new WebMarkupContainer("container");
        webMarkupContainer.setOutputMarkupId(true);
        add(webMarkupContainer);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    public Form<?> clusterForm() {

        Form<?> form = new Form<Void>(ID_EXECUTE_CLUSTERING_FORM);

        LabelWithHelpPanel labelMode = new LabelWithHelpPanel("modeSelector_label", Model.of("Process mode")) {
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of("d");
            }
        };
        labelMode.setOutputMarkupId(true);
        form.add(labelMode);

        ChoiceRenderer<Mode> renderer = new ChoiceRenderer<>("displayString");

        DropDownChoice<Mode> modeSelector = new DropDownChoice<>(
                "modeSelector", Model.of(clusterOptions.getMode()),
                new ArrayList<>(EnumSet.allOf(Mode.class)), renderer);
        modeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                clusterOptions.setMode(modeSelector.getModelObject());
            }
        });

        modeSelector.setOutputMarkupId(true);
        form.add(modeSelector);

        clusterOptions.setName("p_cluster_" + (countParentClusterTypeObjects((PageBase) getPage()) + 1));

        TextField<String> nameField = new TextField<>(ID_NAME_FIELD,
                Model.of(clusterOptions.getName()));
        nameField.setOutputMarkupId(true);
        nameField.setOutputMarkupPlaceholderTag(true);
        nameField.setVisible(true);
        form.add(nameField);

        LabelWithHelpPanel labelName = new LabelWithHelpPanel(ID_NAME_FIELD + "_label", Model.of("Name")) {
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of("d");
            }
        };
        labelName.setOutputMarkupId(true);
        form.add(labelName);

        TextField<Double> thresholdField = new TextField<>(ID_JACCARD_THRESHOLD_FIELD,
                Model.of(clusterOptions.getSimilarity()));
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        form.add(thresholdField);

        LabelWithHelpPanel label = new LabelWithHelpPanel(ID_JACCARD_THRESHOLD_FIELD + "_label", Model.of("Similarity")) {
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of("d");
            }
        };
        label.setOutputMarkupId(true);
        form.add(label);

        TextField<Integer> minIntersectionField = new TextField<>(ID_INTERSECTION_THRESHOLD_FIELD,
                Model.of(clusterOptions.getMinIntersections()));
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        form.add(minIntersectionField);

        LabelWithHelpPanel label1 = new LabelWithHelpPanel(ID_MIN_ASSIGN + "_label", Model.of("Min assignments")) {
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of("d");
            }
        };
        label1.setOutputMarkupId(true);
        form.add(label1);

        TextField<Integer> minAssign = new TextField<>(ID_MIN_ASSIGN,
                Model.of(clusterOptions.getAssignThreshold()));
        minAssign.setOutputMarkupId(true);
        minAssign.setOutputMarkupPlaceholderTag(true);
        minAssign.setVisible(true);
        form.add(minAssign);

        LabelWithHelpPanel label2 = new LabelWithHelpPanel(ID_INTERSECTION_THRESHOLD_FIELD + "_label", Model.of("Min intersection")) {
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of("d");
            }
        };
        label2.setOutputMarkupId(true);
        form.add(label2);

        TextField<Integer> minGroupField = new TextField<>(ID_GROUP_THRESHOLD_FIELD,
                Model.of(clusterOptions.getMinGroupSize()));
        minGroupField.setOutputMarkupId(true);
        minGroupField.setOutputMarkupPlaceholderTag(true);
        minGroupField.setVisible(true);
        form.add(minGroupField);

        LabelWithHelpPanel label3 = new LabelWithHelpPanel(ID_GROUP_THRESHOLD_FIELD + "_label", Model.of("Min members")) {
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of("d");
            }
        };
        label3.setOutputMarkupId(true);
        form.add(label3);

        clusterOptions.setIdentifier("p_cid_" + (countParentClusterTypeObjects((PageBase) getPage()) + 1));
        TextField<String> identifierField = new TextField<>(ID_IDENTIFIER_FIELD,
                Model.of(clusterOptions.getIdentifier()));
        identifierField.setOutputMarkupId(true);
        identifierField.setOutputMarkupPlaceholderTag(true);
        identifierField.setVisible(true);
        form.add(identifierField);

        LabelWithHelpPanel label4 = new LabelWithHelpPanel(ID_IDENTIFIER_FIELD + "_label", Model.of("Identifier")) {
            @Override
            protected IModel<String> getHelpModel() {
                return Model.of("d");
            }
        };
        label4.setOutputMarkupId(true);
        form.add(label4);

        objectFiltersPanel(form);
        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink(ID_SUBMIT_BUTTON, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                clusterOptions.setName(nameField.getModelObject());
                clusterOptions.setSimilarity(thresholdField.getModelObject());
                clusterOptions.setMinIntersections(minIntersectionField.getModelObject());
                clusterOptions.setMinGroupSize(minGroupField.getModelObject());
                clusterOptions.setIdentifier(identifierField.getModelObject());
                clusterOptions.setAssignThreshold(minAssign.getModelObject());

                ClusteringExecutor clusteringExecutor = new ClusteringExecutor(clusterOptions.getMode());
                List<PrismObject<ClusterType>> clusters = clusteringExecutor.execute(clusterOptions);

                OperationResult resultD = new OperationResult("Delete Cluster object");

                try {
                    cleanBeforeClustering(resultD, ((PageBase) getPage()), clusterOptions.getIdentifier());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                OperationResult result = new OperationResult("Generate Cluster object");
                double density = 0;
                int counsist = 0;
                List<String> childRef = new ArrayList<>();
                try {
                    for (PrismObject<ClusterType> clusterTypePrismObject : clusters) {
                        importClusterTypeObject(result, ((PageBase) getPage()), clusterTypePrismObject);
                        density += Double.parseDouble(clusterTypePrismObject.asObjectable().getDensity());
                        counsist += clusterTypePrismObject.asObjectable().getElementCount();
                        childRef.add(clusterTypePrismObject.getOid());
                    }
                    density = density / clusters.size();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                JSONObject options = new JSONObject();
                options.put("name", clusterOptions.getName());
                options.put("identifier", clusterOptions.getIdentifier());
                options.put("filter", clusterOptions.getQuery());
                options.put("assignThreshold", clusterOptions.getAssignThreshold());
                options.put("mode", clusterOptions.getMode());
                options.put("similarity", clusterOptions.getSimilarity());
                options.put("minIntersection", clusterOptions.getMinIntersections());
                options.put("minGroup", clusterOptions.getMinGroupSize());

                importParentClusterTypeObject(result, ((PageBase) getPage()), density, counsist, childRef, options);

                target.add(thresholdField);
                target.add(minIntersectionField);
                target.add(minGroupField);
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
                return Model.of("d");
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

        AjaxSubmitButton filterSubmitButton = new AjaxSubmitButton("filterForm_submit") {
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
        panel.add(filterSubmitButton);
    }

    private void showResultFeedback(OperationResult result, @NotNull Component resultPanel, @NotNull AjaxRequestTarget target) {
        this.result = result;
        resultPanel.setVisible(true);
        target.add(resultPanel);
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
