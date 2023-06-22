/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.perform;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningObjectUtils.deleteMiningObjects;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningObjectUtils.prepareExactUsersSetByRoles;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.TextFieldLabelPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class MiningPanel extends BasePanel<String> implements Popupable {
    private static final String ID_EXECUTE_CLUSTERING_FORM = "thresholds_form_cluster";
    private static final String ID_INTERSECTION_THRESHOLD_FIELD = "intersection_field_min_cluster";
    private static final String ID_SUBMIT_BUTTON = "ajax_submit_link_cluster";

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";

    int intersectionThreshold;

    public MiningPanel(String id, IModel<String> messageModel) {
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

        TextFieldLabelPanel minIntersectionField = new TextFieldLabelPanel(ID_INTERSECTION_THRESHOLD_FIELD,
                Model.of(intersectionThreshold), "Min intersection");
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(true);
        form.add(minIntersectionField);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink(ID_SUBMIT_BUTTON, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                intersectionThreshold = (int) minIntersectionField.getBaseFormComponent().getModelObject();

                long startTime = System.currentTimeMillis();

                OperationResult resultD = new OperationResult("Delete MiningType object");

                try {
                    deleteMiningObjects(resultD, getPageBase());

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                OperationResult result = new OperationResult("Import MiningType object");

                try {
                    prepareExactUsersSetByRoles(result, getPageBase(), intersectionThreshold);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                long endTime = System.currentTimeMillis();
                long elapsedTime = endTime - startTime;
                double elapsedSeconds = elapsedTime / 1000.0;
                System.out.println("Elapsed time: " + elapsedSeconds + " seconds. (update mining type object (db))");
                target.add(minIntersectionField);
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
