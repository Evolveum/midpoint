/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public abstract class VerticalFormPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(VerticalFormPanel.class);

    private static final String ID_SINGLE_CONTAINER = "singleContainer";

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
                PrismContainerValueWrapper<C> newItemWrapper = WebPrismUtil.createNewValueWrapper(container, newItem, getPageBase(), getWrapperContext());
                container.getValues().add(newItemWrapper);
            }
        } catch (SchemaException e) {
            LOGGER.error("Cannot find wrapper: {}", e.getMessage());
        }
    }

    private WrapperContext getWrapperContext() {
        WrapperContext context = createWrapperContext();
        context.setObjectStatus(getModelObject().findObjectStatus());
        context.setShowEmpty(true);
        context.setCreateIfEmpty(true);
        return context;
    }

    protected WrapperContext createWrapperContext() {
        return null;
    }

    private void initLayout() {
        SingleContainerPanel<C> singleContainer = new SingleContainerPanel<C>(ID_SINGLE_CONTAINER, getModel(), config) {
            @Override
            protected Panel createPanel(String id, QName typeName, IModel<PrismContainerWrapper<C>> model, ItemPanelSettingsBuilder builder) throws SchemaException {
                return createVirtualPanel(id, model, builder, null);
            }

            @Override
            protected Panel createVirtualPanel(
                    String id,
                    IModel<PrismContainerWrapper<C>> model,
                    ItemPanelSettingsBuilder builder,
                    VirtualContainersSpecificationType virtualContainer) {
                if (settings.getVisibilityHandler() != null) {
                    builder.visibilityHandler(getVisibilityHandler(settings.getVisibilityHandler()));
                }
                builder.mandatoryHandler(settings.getMandatoryHandler());
                return new VerticalFormPrismContainerPanel<C>(id, model, builder.build()) {
                    @Override
                    protected IModel<String> getTitleModel() {
                        if (virtualContainer != null
                                && virtualContainer.getDisplay() != null
                                && virtualContainer.getDisplay().getLabel() != null) {
                            return () -> WebComponentUtil.getTranslatedPolyString(virtualContainer.getDisplay().getLabel());
                        }

                        return VerticalFormPanel.this.getTitleModel();
                    }

                    @Override
                    protected String getIcon() {
                        return VerticalFormPanel.this.getIcon();
                    }

                    @Override
                    protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
                        return VerticalFormPanel.this.isVisibleSubContainer(c);
                    }

                    @Override
                    protected boolean isHeaderVisible() {
                        return VerticalFormPanel.this.isHeaderVisible(model);
                    }

                    @Override
                    protected String getCssForHeader() {
                        return VerticalFormPanel.this.getCssForHeader();
                    }

                    @Override
                    protected boolean isShowEmptyButtonVisible() {
                        return VerticalFormPanel.this.isShowEmptyButtonVisible();
                    }

                    @Override
                    protected String getClassForPrismContainerValuePanel() {
                        return VerticalFormPanel.this.getClassForPrismContainerValuePanel();
                    }

                    @Override
                    protected String getCssClassForFormContainerOfValuePanel() {
                        return VerticalFormPanel.this.getCssClassForFormContainerOfValuePanel();
                    }

                    @Override
                    protected boolean isExpandedButtonVisible() {
                        return VerticalFormPanel.this.isExpandedButtonVisible();

                    }
                };
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> createVirtualContainerModel(VirtualContainersSpecificationType virtualContainer) {
                PrismContainerWrapper<C> container = getModelObject();
                if (container.getPath() != null
                        && virtualContainer.getPath() != null
                        && container.getPath().namedSegmentsOnly().equivalent(virtualContainer.getPath().getItemPath())) {
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

    protected String getCssClassForFormContainerOfValuePanel() {
        return "card-body border-top mb-0 p-3";
    }

    protected String getCssForHeader() {
        return "bg-white border-bottom-0 p-2 pl-3 pr-3 mb-0 btn w-100";
    }

    protected String getClassForPrismContainerValuePanel() {
        return null;
    }

    protected boolean isShowEmptyButtonVisible() {
        return true;
    }

    protected boolean isExpandedButtonVisible() {
        return true;
    }

    protected boolean isHeaderVisible(IModel<PrismContainerWrapper<C>> model) {
        return true;
    }

    protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
        return false;
    }

    protected IModel<String> getTitleModel() {
        return getPageBase().createStringResource(getModelObject().getDisplayName());
    }

    protected String getIcon() {
        return "";
    }
}
