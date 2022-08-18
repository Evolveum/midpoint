/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class VerticalFormDefaultContainerablePanel<C extends Containerable> extends DefaultContainerablePanel<C, PrismContainerValueWrapper<C>> {

    private static final String ID_PROPERTY = "property";

    private static final String ID_SHOW_EMPTY_BUTTON_CONTAINER = "showEmptyButtonContainer";

    public VerticalFormDefaultContainerablePanel(String id, IModel<PrismContainerValueWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        get(ID_PROPERTIES_LABEL).add(new VisibleBehaviour(this::isVisibleVirtualValueWrapper));
    }

    protected void populateNonContainer(ListItem<? extends ItemWrapper<?, ?>> item) {
        item.setOutputMarkupId(true);

        ItemPanel propertyPanel;
        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
        if (item.getModelObject() instanceof PrismPropertyWrapper) {
            propertyPanel = new VerticalFormPrismPropertyPanel(ID_PROPERTY, item.getModel(), settings);
        } else {
            propertyPanel = new VerticalFormPrismReferencePanel(ID_PROPERTY, item.getModel(), settings);
        }
        propertyPanel.setOutputMarkupId(true);

        if (settings != null) {
            propertyPanel.add(
                    new VisibleBehaviour(() -> isNonContainerVisible(item, settings)));
        }

        item.add(propertyPanel);
    }

    private Boolean isNonContainerVisible(ListItem<? extends ItemWrapper<?,?>> item, ItemPanelSettings settings) {
        if (!isVisibleVirtualValueWrapper()) {
            return false;
        }
        return item.getModelObject().isVisible(
                VerticalFormDefaultContainerablePanel.this.getModelObject(),
                settings.getVisibilityHandler());
    }

    public Boolean isVisibleVirtualValueWrapper() {
        VirtualContainersSpecificationType container = getConfigurationForVirtualContainer();
        if (container != null && !WebComponentUtil.getElementVisibility(container.getVisibility())) {
            return false;
        }
        return true;
    }

    private VirtualContainersSpecificationType getConfigurationForVirtualContainer() {
        ContainerPanelConfigurationType config = getSettings() != null ? getSettings().getConfig() : null;
        if (config != null) {
            @NotNull ItemPath containerValuePath = VerticalFormDefaultContainerablePanel.this.getModelObject().getPath().namedSegmentsOnly();
            PrismContainerWrapper parent = VerticalFormDefaultContainerablePanel.this.getModelObject().getParent();
            if (parent.getIdentifier() == null) {
                for (VirtualContainersSpecificationType container : config.getContainer()) {
                    if (container.getPath() != null
                            && container.getPath().getItemPath().equivalent(containerValuePath)) {
                        return container;
                    }
                }
            }
        }
        return null;
    }

    protected IModel<List<PrismContainerWrapper<? extends Containerable>>> createContainersModel() {
        ContainerPanelConfigurationType config = getPanelConfiguration();
        if (config == null) {
            return Model.ofList(List.of());
        }

        return new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerWrapper<? extends Containerable>> load() {
                PrismContainerValueWrapper<C> modelObject = getModelObject();
                List<PrismContainerWrapper<? extends Containerable>> containers = modelObject.getContainers(getPanelConfiguration(), getPageBase());
                containers.removeIf(c -> !c.isVirtual() || c.getIdentifier() == null);
                containers.forEach(c -> c.setShowEmpty(true, true));
                return containers;
            }
        };
    }

    @Override
    protected void populateContainer(ListItem<PrismContainerWrapper<?>> container) {
        PrismContainerWrapper<?> itemWrapper = container.getModelObject();
        IModel<ItemWrapper> wrapperModel = () -> container.getModelObject();

        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
        Panel panel = new VerticalFormPrismContainerPanel<>("container", (IModel) wrapperModel, settings) {

            @Override
            protected IModel<String> getTitleModel() {
                VirtualContainersSpecificationType containerConfig = getConfigurationForVirtualContainer();
                if (containerConfig != null
                        && containerConfig.getDisplay() != null
                        && containerConfig.getDisplay().getLabel() != null) {
                    return () -> WebComponentUtil.getTranslatedPolyString(containerConfig.getDisplay().getLabel());
                }
                return super.getTitleModel();
            }

            @Override
            protected String getIcon() {
                VirtualContainersSpecificationType containerConfig = getConfigurationForVirtualContainer();
                if (containerConfig != null
                        && containerConfig.getDisplay() != null) {
                    String iconCssClass = WebComponentUtil.getIconCssClass(containerConfig.getDisplay());
                    if (StringUtils.isNoneEmpty(iconCssClass)) {
                        return iconCssClass;
                    }
                }
                return "fa fa-circle";
            }
        };
        panel.setOutputMarkupId(true);
        container.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                boolean oldExpanded = getModelObject().isExpanded();
                getModelObject().setExpanded(true);
                boolean isVisible = itemWrapper.isVisible(getModelObject(), getVisibilityHandler());
                getModelObject().setExpanded(oldExpanded);
                return isVisible;
            }

            @Override
            public boolean isEnabled() {
                return !itemWrapper.isReadOnly() || itemWrapper.isMetadata(); //TODO hack isMetadata - beacuse all links are then disabled.
            }
        });
        container.add(panel);
    }

    @Override
    protected AjaxButton createShowEmptyButton(String id) {
        AjaxButton button = super.createShowEmptyButton(id);
        AjaxButton buttonContainer = new AjaxButton(ID_SHOW_EMPTY_BUTTON_CONTAINER) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                button.onClick(target);
            }

        };
        buttonContainer.add(button);
        return buttonContainer;
    }
}
