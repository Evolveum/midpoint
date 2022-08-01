/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public abstract class VerticalFormPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(VerticalFormPanel.class);

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
        checkMultivalueValues();
        initLayout();
    }

    private void checkMultivalueValues() {
        PrismContainerWrapper<C> container = getModelObject();
        try {
            if (container.getItem().getDefinition().isMultiValue() && container.getValues().isEmpty()) {
                PrismContainerValue<C> newItem = container.getItem().createNewValue();
                PrismContainerValueWrapper<C> newItemWrapper = WebPrismUtil.createNewValueWrapper(container, newItem, getPageBase());
                container.getValues().add(newItemWrapper);
            }
        } catch (SchemaException e) {
            LOGGER.error("Cannot find wrapper: {}", e.getMessage());
        }
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

        SingleContainerPanel<C> singleContainer = new SingleContainerPanel<C>(ID_SINGLE_CONTAINER, getModel(), config) {
            @Override
            protected Panel createPanel(String id, QName typeName, IModel<PrismContainerWrapper<C>> model, ItemPanelSettingsBuilder builder) throws SchemaException {
                return createVirtualPanel(id, model, builder);
            }

            @Override
            protected Panel createVirtualPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettingsBuilder builder) {
                if (settings.getVisibilityHandler() != null) {
                    builder.visibilityHandler(getVisibilityHandler(settings.getVisibilityHandler()));
                }
                builder.mandatoryHandler(settings.getMandatoryHandler());
                return new VerticalFormPrismContainerPanel<C>(id, model, builder.build()) {
                    @Override
                    protected IModel<String> getTitleModel() {
                        PrismContainerWrapper<C> container = getModelObject();
                        if (container == null || !container.isVirtual() || StringUtils.isEmpty(container.getDisplayName())) {
                            return VerticalFormPanel.this.getTitleModel();
                        }
                        return super.getTitleModel();
                    }

                    @Override
                    protected String getIcon() {
                        return VerticalFormPanel.this.getIcon();
                    }
                };
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> createVirtualContainerModel(VirtualContainersSpecificationType virtualContainer) {
                PrismContainerWrapper<C> container = getModelObject();
                if (container.getPath() != null
                        && virtualContainer.getPath() != null
                        && container.getPath().equivalent(virtualContainer.getPath().getItemPath())) {
                    return getModel();
                }
                return super.createVirtualContainerModel(virtualContainer);
            }

            private ItemVisibilityHandler getVisibilityHandler(ItemVisibilityHandler handler) {
                return wrapper -> {
                    ItemVisibility visibility = handler.isVisible(wrapper);
                    if (ItemVisibility.AUTO.equals(visibility)) {
                        return getVisibility(wrapper);
                    }
                    return visibility;
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
