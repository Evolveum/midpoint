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

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
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
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyWrapperModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AxiomQueryWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ClusterAlgorithm;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ClusterAlgorithmByRole;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

public class ClusterPanel extends BasePanel<String> implements Popupable {
    private static final String ID_EXECUTE_CLUSTERING_FORM = "thresholds_form_cluster";
    private static final String ID_WARNING_FEEDBACK = "warningFeedback";
    private static final String ID_JACCARD_THRESHOLD_FIELD = "eps_cluster";
    private static final String ID_INTERSECTION_THRESHOLD_FIELD = "intersection_field_min_cluster";
    private static final String ID_GROUP_THRESHOLD_FIELD = "group_min_cluster";
    private static final String ID_SUBMIT_BUTTON = "ajax_submit_link_cluster";
    private static final String ID_IDENTIFIER_FIELD = "identifier_field";

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";

    double threshold = 0.2;
    int intersectionThreshold = 10;
    int groupThreshold = 5;
    String identifier;

    ObjectFilter objectFilter;
    OperationResult result;

    public ClusterPanel(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(clusterForm());

        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClose(target);
            }
        };
        add(cancelButton);

        WebMarkupContainer webMarkupContainer = new WebMarkupContainer("container");
        webMarkupContainer.setOutputMarkupId(true);
        add(webMarkupContainer);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    Mode executeMode;

    public Form<?> clusterForm() {

        Form<?> form = new Form<Void>(ID_EXECUTE_CLUSTERING_FORM);

        ChoiceRenderer<Mode> renderer = new ChoiceRenderer<>("displayString");

        executeMode = Mode.USER;
        DropDownChoice<Mode> modeSelector = new DropDownChoice<>(
                "modeSelector", Model.of(executeMode),
                new ArrayList<>(EnumSet.allOf(Mode.class)), renderer);
        modeSelector.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                executeMode = modeSelector.getModelObject();
            }
        });

        modeSelector.setOutputMarkupId(true);
        form.add(modeSelector);

        TextField<Double> thresholdField = new TextField<>(ID_JACCARD_THRESHOLD_FIELD,
                Model.of(threshold));
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        form.add(thresholdField);

        Label label = new Label(ID_JACCARD_THRESHOLD_FIELD + "_label", "Jaccard distance");
        label.setOutputMarkupId(true);
        form.add(label);

        TextField<Integer> minIntersectionField = new TextField<>(ID_INTERSECTION_THRESHOLD_FIELD,
                Model.of(intersectionThreshold));
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        form.add(minIntersectionField);

        Label label2 = new Label(ID_INTERSECTION_THRESHOLD_FIELD + "_label", "Min intersection");
        label2.setOutputMarkupId(true);
        form.add(label2);

        TextField<Integer> minGroupField = new TextField<>(ID_GROUP_THRESHOLD_FIELD,
                Model.of(groupThreshold));
        minGroupField.setOutputMarkupId(true);
        minGroupField.setOutputMarkupPlaceholderTag(true);
        minGroupField.setVisible(true);
        form.add(minGroupField);

        Label label3 = new Label(ID_GROUP_THRESHOLD_FIELD + "_label", "Min members");
        label3.setOutputMarkupId(true);
        form.add(label3);

        identifier = "cluster_" + countParentClusters((PageBase) getPage()) + 1;
        TextField<String> identifierField = new TextField<>(ID_IDENTIFIER_FIELD,
                Model.of(identifier));
        identifierField.setOutputMarkupId(true);
        identifierField.setOutputMarkupPlaceholderTag(true);
        identifierField.setVisible(true);
        form.add(identifierField);

        Label label4 = new Label(ID_IDENTIFIER_FIELD + "_label", "Identifier");
        label3.setOutputMarkupId(true);
        form.add(label4);

        objectFiltersPanel(form);
        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink(ID_SUBMIT_BUTTON, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                threshold = thresholdField.getModelObject();
                intersectionThreshold = minIntersectionField.getModelObject();
                groupThreshold = minGroupField.getModelObject();
                identifier = identifierField.getModelObject();

                List<PrismObject<ClusterType>> clusters = null;
                if (executeMode.equals(Mode.ROLE)) {
                    ClusterAlgorithmByRole clusterAlgorithmByRole = new ClusterAlgorithmByRole(((PageBase) getPage()));
                    clusters = clusterAlgorithmByRole.executeClustering(threshold,
                            groupThreshold, intersectionThreshold, identifier, objectFilter);
                } else if (executeMode.equals(Mode.USER)) {
                    ClusterAlgorithm clusterAlgorithm = new ClusterAlgorithm(((PageBase) getPage()));
                    clusters = clusterAlgorithm.executeClustering(threshold,
                            groupThreshold, intersectionThreshold, identifier, objectFilter);
                }

                OperationResult resultD = new OperationResult("Delete Cluster object");

                try {
                    cleanBeforeClustering(resultD, ((PageBase) getPage()), identifier);
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

                importParentClusterTypeObject(result, ((PageBase) getPage()), density, counsist, childRef, identifier, executeMode);

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
                            objectFilter = getPrismContext().createQueryParser().parseFilter(FocusType.class, midPointQuery);
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
