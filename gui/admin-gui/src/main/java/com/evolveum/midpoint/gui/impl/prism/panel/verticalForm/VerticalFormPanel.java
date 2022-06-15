/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.verticalForm;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public abstract class VerticalFormPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> {

    private static final String ID_SINGLE_CONTAINER = "singleContainer";
    private static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";

    private final ItemPanelSettings settings;
    private final ContainerPanelConfigurationType config;

    public VerticalFormPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettings settings, ContainerPanelConfigurationType config) {
        super(id, model);
        this.settings = settings;
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);

        SingleContainerPanel<C> singleContainer = new SingleContainerPanel<C>(ID_SINGLE_CONTAINER, getModel(), config){
            @Override
            protected Panel createPanel(String id, QName typeName, IModel<PrismContainerWrapper<C>> model, ItemPanelSettingsBuilder builder) throws SchemaException {
                return createVirtualPanel(id, model, builder);
            }

            @Override
            protected Panel createVirtualPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettingsBuilder builder) {
                builder.visibilityHandler(settings.getVisibilityHandler())
                        .mandatoryHandler(settings.getMandatoryHandler());
                return new VerticalFormPrismContainerPanel<C>(id, model, builder.build()) {
                    @Override
                    protected IModel<String> getTitleModel() {
                        return VerticalFormPanel.this.getTitleModel();
                    }

                    @Override
                    protected String getIcon() {
                        return VerticalFormPanel.this.getIcon();
                    }
                };
            }
        };
        singleContainer.setOutputMarkupId(true);
        add(singleContainer);
    }

    protected IModel<String> getTitleModel() {
        return getPageBase().createStringResource(getModelObject().getDisplayName());
    }

    protected String getIcon() {
        return "";
    }

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }
}
