/*
 * Copyright (c) 2020 Evolveum and contributors
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
                Panel panel = getPageBase().initItemPanel(ID_CONTAINER, getTypeName(), getModel(), builder.build());
                add(panel);
            } else {
                RepeatingView view = new RepeatingView(ID_CONTAINER);

                List<VirtualContainersSpecificationType> virtualContainers = config.getContainer();
                for (VirtualContainersSpecificationType virtualContainer : virtualContainers) {
                    IModel<PrismContainerWrapper<C>> virtualContainerModel = createVirtualContainerModel(virtualContainer);
                    Panel virtualPanel = new PrismContainerPanel<>(view.newChildId(), virtualContainerModel, builder.build());
                    view.add(virtualPanel);
                }

                Panel panel = getPageBase().initItemPanel(view.newChildId(), getTypeName(), getModel(), builder.build());
                view.add(panel);


                add(view);
            }

        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for {}, {}", getTypeName(), e.getMessage(), e);
            getSession().error("Cannot create panel for " + getTypeName()); // TODO opertion result? localization?

        }

    }

    private IModel<PrismContainerWrapper<C>> createVirtualContainerModel(VirtualContainersSpecificationType virtualContainer) {
        if (virtualContainer.getPath() != null) {
            return createContainerModel(virtualContainer.getPath().getItemPath());
        }
        return PrismContainerWrapperModel.fromContainerWrapper(getModel(), virtualContainer.getIdentifier());
    }

    protected QName getTypeName() {
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

    //TODO copied from abstractObejctMainPanel
    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel(ItemPath path) {
        return PrismContainerWrapperModel.fromContainerWrapper(getModel(), path);
    }

}
