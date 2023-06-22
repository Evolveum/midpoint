/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.perform;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ClusterAlgorithm;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.TextFieldLabelPanel;
import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;

import org.apache.wicket.model.StringResourceModel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;

public class ClusterPanel extends BasePanel<String> implements Popupable {
    private static final String ID_EXECUTE_CLUSTERING_FORM = "thresholds_form_cluster";
    private static final String ID_JACCARD_THRESHOLD_FIELD = "eps_cluster";
    private static final String ID_INTERSECTION_THRESHOLD_FIELD = "intersection_field_min_cluster";
    private static final String ID_GROUP_THRESHOLD_FIELD = "group_min_cluster";
    private static final String ID_SUBMIT_BUTTON = "ajax_submit_link_cluster";
    private static final String ID_IDENTIFIER_FIELD = "identifier_field";

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";

    double threshold;
    int intersectionThreshold;
    int groupThreshold;
    String identifier;

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
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    public Form<?> clusterForm() {

        Form<?> form = new Form<Void>(ID_EXECUTE_CLUSTERING_FORM);

        TextFieldLabelPanel thresholdField = new TextFieldLabelPanel(ID_JACCARD_THRESHOLD_FIELD,
                Model.of(threshold), "Jaccard distance");
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(true);
        form.add(thresholdField);

        TextFieldLabelPanel minIntersectionField = new TextFieldLabelPanel(ID_INTERSECTION_THRESHOLD_FIELD,
                Model.of(intersectionThreshold), "Min intersection");
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        form.add(minIntersectionField);

        TextFieldLabelPanel minGroupField = new TextFieldLabelPanel(ID_GROUP_THRESHOLD_FIELD,
                Model.of(groupThreshold), "Min members");
        minGroupField.setOutputMarkupId(true);
        minGroupField.setOutputMarkupPlaceholderTag(true);
        minGroupField.setVisible(true);
        form.add(minGroupField);

        TextFieldLabelPanel identifierField = new TextFieldLabelPanel(ID_IDENTIFIER_FIELD,
                Model.of(identifier), "Identifier");
        identifierField.setOutputMarkupId(true);
        identifierField.setOutputMarkupPlaceholderTag(true);
        identifierField.setVisible(true);
        form.add(identifierField);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink(ID_SUBMIT_BUTTON, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                threshold = (double) thresholdField.getBaseFormComponent().getModelObject();
                intersectionThreshold = (int) minIntersectionField.getBaseFormComponent().getModelObject();
                groupThreshold = (int) minGroupField.getBaseFormComponent().getModelObject();
                identifier = (String) identifierField.getBaseFormComponent().getModelObject();

                ClusterAlgorithm clusterAlgorithm = new ClusterAlgorithm(((PageBase) getPage()));
                List<PrismObject<ClusterType>> clusterTypeList = clusterAlgorithm.executeClustering(threshold,
                        groupThreshold, intersectionThreshold, identifier);

                long startTime = System.currentTimeMillis();

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
                    for (PrismObject<ClusterType> clusterTypePrismObject : clusterTypeList) {
                        importClusterTypeObject(result, ((PageBase) getPage()), clusterTypePrismObject);
                        density += Double.parseDouble(clusterTypePrismObject.asObjectable().getDensity());
                        counsist += clusterTypePrismObject.asObjectable().getMembersCount();
                        childRef.add(clusterTypePrismObject.getOid());
                    }
                    density = density / clusterTypeList.size();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                importParentClusterTypeObject(result, ((PageBase) getPage()), density, counsist, childRef, identifier);

                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                double elapsedSeconds = elapsedTime / 1000.0;
                System.out.println("Elapsed time: " + elapsedSeconds + " seconds. (update cluster db)");

                target.add(thresholdField);
                target.add(minIntersectionField);
                target.add(minGroupField);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);
        form.add(ajaxSubmitLink);

        return form;
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
