/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.experimental;

import java.time.Duration;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.Handler;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class ProgressStatus extends BasePanel<String> implements Popupable {

    private static final String PROGRESS_PANEL = "progress_description";
    private static final String PROGRESS_BAR = "progressBar";
    public ProgressStatus(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public void initLayout() {

        WebMarkupContainer slider = new WebMarkupContainer(PROGRESS_BAR);
        slider.setOutputMarkupId(true);
        slider.add(new AttributeModifier("aria-valuenow", getHandler().getPercentage()));
        add(slider);

        Label progressLabel = new Label(PROGRESS_PANEL, Model.of("Progress: 0%"));
        progressLabel.setOutputMarkupId(true);
        progressLabel.setVisible(true);

        progressLabel.add(new AbstractAjaxTimerBehavior(Duration.ofMillis(100)) {

            @Override
            protected void onTimer(AjaxRequestTarget target) {
                String status = getHandler().toString();
                if (status == null) {
                    status = "UNKNOWN";
                }

                int percentage = getHandler().getPercentage();
                slider.add(new AttributeModifier("style", "width:" + percentage + "%"));
                target.add(slider);
                progressLabel.setDefaultModelObject("Progress: " + status);
                target.add(progressLabel);

                if (!isActive()) {
                    stop(target);
                    performAfterFinish();
                }

            }
        });

        add(progressLabel);

    }

    public void performAfterFinish() {
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    public boolean isActive() {
        return false;
    }

    public Handler getHandler() {
        return null;
    }

    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 70;
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
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("Progress bar");
    }
}
