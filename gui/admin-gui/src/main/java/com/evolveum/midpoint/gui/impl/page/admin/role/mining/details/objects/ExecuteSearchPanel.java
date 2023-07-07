/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.TextFieldLabelPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class ExecuteSearchPanel extends BasePanel<String> implements Popupable {

    private static final String ID_FREQUENCY_FORM = "thresholds_form";
    private static final String ID_FREQUENCY_THRESHOLD_MIN_FRQ = "threshold_frequency";
    private static final String ID_FREQUENCY_THRESHOLD_MAX_FRQ = "threshold_frequency_max";
    private static final String ID_FREQUENCY_SUBMIT = "ajax_submit_link";
    private static final String ID_FREQUENCY_THRESHOLD_ITR = "threshold_intersection";
    private static final String ID_OCCUPANCY_THRESHOLD = "threshold_occupancy";
    double minFrequency = 0.3;
    double maxFrequency = 1.0;
    Integer minIntersection = 10;
    Integer minOccupancy = 5;

    public ExecuteSearchPanel(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(frequencyForm());
    }

    public Form<?> frequencyForm() {

        Form<?> form = new Form<Void>(ID_FREQUENCY_FORM);

        TextFieldLabelPanel minFreqField = generateFieldPanel(ID_FREQUENCY_THRESHOLD_MIN_FRQ,
                Model.of(minFrequency), getString("RoleMining.frequency.min.title"));
        form.add(minFreqField);

        TextFieldLabelPanel maxFreqField = generateFieldPanel(ID_FREQUENCY_THRESHOLD_MAX_FRQ,
                Model.of(maxFrequency), getString("RoleMining.frequency.max.title"));
        form.add(maxFreqField);

        TextFieldLabelPanel minIntersectionField = generateFieldPanel(ID_FREQUENCY_THRESHOLD_ITR,
                Model.of(minIntersection), getString("RoleMining.frequency.intersections.title"));
        form.add(minIntersectionField);

        TextFieldLabelPanel minOccupancyField = generateFieldPanel(ID_OCCUPANCY_THRESHOLD,
                Model.of(minOccupancy), getString("RoleMining.frequency.occupancy.title"));
        form.add(minOccupancyField);

        AjaxSubmitLink ajaxSubmitLink = new AjaxSubmitLink(ID_FREQUENCY_SUBMIT, form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                minFrequency = (double) minFreqField.getBaseFormComponent().getModelObject();
                maxFrequency = (double) maxFreqField.getBaseFormComponent().getModelObject();
                minIntersection = (Integer) minIntersectionField.getBaseFormComponent().getModelObject();
                minOccupancy = (Integer) minOccupancyField.getBaseFormComponent().getModelObject();

                performAction(target);

                target.add(minOccupancyField);
                target.add(minIntersectionField);
                target.add(minFreqField);
                target.add(maxFreqField);

                getPageBase().hideMainPopup(target);
            }
        };

        ajaxSubmitLink.setOutputMarkupId(true);
        form.add(ajaxSubmitLink);

        return form;
    }

    public void performAction(AjaxRequestTarget target) {

    }

    private TextFieldLabelPanel generateFieldPanel(String id, IModel<?> model, String stringResource) {
        TextFieldLabelPanel components = new TextFieldLabelPanel(id,
                model, stringResource);
        components.setOutputMarkupId(true);
        components.setOutputMarkupPlaceholderTag(true);
        components.setVisible(true);
        return components;
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 300;
    }

    @Override
    public int getHeight() {
        return 400;
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
        return new StringResourceModel("RoleMining.members.execute.search.panel.title");
    }

    public double getMinFrequency() {
        return minFrequency;
    }

    public double getMaxFrequency() {
        return maxFrequency;
    }

    public Integer getMinIntersection() {
        return minIntersection;
    }

    public Integer getMinOccupancy() {
        return minOccupancy;
    }
}
