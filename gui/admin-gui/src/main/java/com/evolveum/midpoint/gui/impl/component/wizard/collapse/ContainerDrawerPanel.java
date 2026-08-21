/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.prism.Containerable;

public class ContainerDrawerPanel<C extends Containerable>
        extends BasePanel<PrismContainerValueWrapper<C>> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_FORM = "form";

    private final ItemPanelSettings settings;
    private final IModel<String> descriptionModel;

    public ContainerDrawerPanel(
            String id,
            IModel<PrismContainerValueWrapper<C>> wrapperModel,
            ItemPanelSettings settings,
            IModel<String> descriptionModel) {
        super(id, wrapperModel);

        this.settings = settings;
        this.descriptionModel = descriptionModel != null ? descriptionModel : Model.of("");
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createDescription());
        add(createFeedbackPanel());
        add(createFormPanel());
    }

    private Component createDescription() {
        return new Label(ID_DESCRIPTION, descriptionModel) {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return descriptionModel.getObject() != null && !descriptionModel.getObject().isEmpty();
            }
        };
    }

    private FeedbackAlerts createFeedbackPanel() {
        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        feedback.setOutputMarkupPlaceholderTag(true);
        feedback.setEscapeModelStrings(false);
        return feedback;
    }

    @Override
    public FeedbackAlerts getFeedbackPanel() {
        return (FeedbackAlerts) get(ID_FEEDBACK);
    }

    public void info(IModel<String> messageModel, AjaxRequestTarget target) {
        info(messageModel.getObject());
        target.add(getFeedbackPanel());
    }

    public void error(IModel<String> messageModel, AjaxRequestTarget target) {
        error(messageModel.getObject());
        target.add(getFeedbackPanel());
    }

    public void warn(IModel<String> messageModel, AjaxRequestTarget target) {
        warn(messageModel.getObject());
        target.add(getFeedbackPanel());
    }

    private VerticalFormDefaultContainerablePanel<C> createFormPanel() {
        IModel<PrismContainerValueWrapper<C>> wrapperModel = getModel();

        PrismContainerValueWrapper<C> wrapper = wrapperModel != null ? wrapperModel.getObject() : null;

        if (wrapper != null) {
            wrapper.setExpanded(true);
            wrapper.setShowEmpty(false);
        }

        ItemPanelSettings settings = this.settings != null ? this.settings : new ItemPanelSettingsBuilder().build();

        VerticalFormDefaultContainerablePanel<C> panel =
                new VerticalFormDefaultContainerablePanel<>(
                        ID_FORM,
                        wrapperModel,
                        settings) {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String getCssClassForFormContainer() {
                        return "p-1";
                    }

                    @Override
                    protected boolean isRemoveValueButtonVisible() {
                        return false;
                    }
                };

        panel.setOutputMarkupId(true);
        return panel;
    }
}
