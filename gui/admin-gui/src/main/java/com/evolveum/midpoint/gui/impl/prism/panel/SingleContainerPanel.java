/*
 * Copyright (c) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import java.util.List;

public class SingleContainerPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(SingleContainerPanel.class);

    private static final String ID_CONTAINER = "container";
    private QName typeName = null;
    private ContainerPanelConfigurationType config;

    public SingleContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, QName typeName) {
        super(id, model);
        this.typeName = typeName;
    }

    public SingleContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ContainerPanelConfigurationType config) {
        super(id, model);
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        try {
            ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                    .visibilityHandler(wrapper -> getVisibility(wrapper))
                    .editabilityHandler(getEditabilityHandler())
                    .mandatoryHandler(getMandatoryHandler());
            if (config == null) {
                Panel panel = createPanel(ID_CONTAINER, getTypeName(), getModel(), builder);
                add(panel);
            } else {
                RepeatingView view = new RepeatingView(ID_CONTAINER);

                List<VirtualContainersSpecificationType> virtualContainers = config.getContainer();
                for (VirtualContainersSpecificationType virtualContainer : virtualContainers) {
                    if (!WebComponentUtil.getElementVisibility(virtualContainer.getVisibility())) {
                        continue;
                    }
                    IModel<PrismContainerWrapper<C>> virtualContainerModel = createVirtualContainerModel(virtualContainer);
                    if (virtualContainerModel == null) {
                        continue;
                    }
                    Panel virtualPanel = createVirtualPanel(view.newChildId(), virtualContainerModel, builder);
                    view.add(virtualPanel);
                }

                QName typeName = getTypeName();
                if (typeName != null) {
                    Panel panel = createPanel(view.newChildId(), typeName, getModel(), builder);
                    view.add(panel);
                }

                add(view);
            }

        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for {}, {}", getTypeName(), e.getMessage(), e);
            getSession().error("Cannot create panel for " + getTypeName()); // TODO opertion result? localization?
        }
    }

    protected Panel createVirtualPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettingsBuilder builder) {
        return new PrismContainerPanel<>(id, model, builder.build());
    }

    protected Panel createPanel(String id, QName typeName, IModel<PrismContainerWrapper<C>> model, ItemPanelSettingsBuilder builder) throws SchemaException {
        return getPageBase().initItemPanel(id, typeName, model, builder.build());
    }

    //just temporary protected.
    protected IModel<PrismContainerWrapper<C>> createVirtualContainerModel(VirtualContainersSpecificationType virtualContainer) {
        if (virtualContainer.getPath() != null) {
            return createContainerModel(virtualContainer.getPath().getItemPath());
        }
        if (StringUtils.isBlank(virtualContainer.getIdentifier())) {
            getSession().error(getString("SingleContainerPanel.empty.identifier", virtualContainer));
            return null;
        }
        return PrismContainerWrapperModel.fromContainerWrapper(getModel(), virtualContainer.getIdentifier());
    }

    private QName getTypeName() {
        return typeName;
    }

    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
        return ItemVisibility.AUTO;
    }

    protected ItemEditabilityHandler getEditabilityHandler() {
        return null;
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        return null;
    }

    //TODO copied from abstractObjectMainPanel
    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel(ItemPath path) {
        return PrismContainerWrapperModel.fromContainerWrapper(getModel(), path);
    }

}
