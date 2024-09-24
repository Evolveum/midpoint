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
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisAttributeSettingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusteringAttributeSettingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lskublik
 */
public class VerticalFormDefaultContainerablePanel<C extends Containerable> extends DefaultContainerablePanel<C, PrismContainerValueWrapper<C>> {

    private static final String ID_PROPERTY = "property";
    public static final String ID_FORM_CONTAINER = "formContainer";
    private static final String ID_SHOW_EMPTY_BUTTON_CONTAINER = "showEmptyButtonContainer";

    public VerticalFormDefaultContainerablePanel(String id, IModel<PrismContainerValueWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        get(ID_PROPERTIES_LABEL).add(new VisibleBehaviour(this::isVisibleVirtualValueWrapper));
    }

    protected void createNonContainersPanel() {
        WebMarkupContainer propertiesLabel = new WebMarkupContainer(ID_PROPERTIES_LABEL);
        propertiesLabel.setOutputMarkupId(true);
        add(propertiesLabel);

        IModel<List<ItemWrapper<?, ?>>> nonContainerWrappers = new PropertyModel<>(getModel(), "nonContainers");

        WebMarkupContainer formContainer = new WebMarkupContainer(ID_FORM_CONTAINER);
        formContainer.add(new VisibleBehaviour(() -> isShowMoreButtonVisible(nonContainerWrappers)));
        propertiesLabel.setOutputMarkupId(true);
        propertiesLabel.add(formContainer);
        ListView<ItemWrapper<?, ?>> properties = new ListView<>("properties", nonContainerWrappers) {

            @Override
            protected void populateItem(ListItem<ItemWrapper<?, ?>> item) {
                populateNonContainer(item);
            }
        };
        properties.setOutputMarkupId(true);
        formContainer.add(properties);

        AjaxButton labelShowEmpty = createShowEmptyButton(ID_SHOW_EMPTY_BUTTON);
        labelShowEmpty.setOutputMarkupId(true);
        labelShowEmpty.add(AttributeAppender.append("style", "cursor: pointer;"));
        labelShowEmpty.add(new VisibleBehaviour(() -> isShowMoreButtonVisible(nonContainerWrappers)));
        formContainer.add(labelShowEmpty);
    }

    protected void populateNonContainer(ListItem<? extends ItemWrapper<?, ?>> item) {
        item.setOutputMarkupId(true);

        ItemPanel propertyPanel = WebPrismUtil.createVerticalPropertyPanel(ID_PROPERTY, item.getModel(), getSettings());

        ItemPanelSettings settings = propertyPanel.getSettings();
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
        return new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerWrapper<? extends Containerable>> load() {
                ContainerPanelConfigurationType config = getPanelConfiguration();
                PrismContainerValueWrapper<C> modelObject = getModelObject();
                List<PrismContainerWrapper<? extends Containerable>> containers = modelObject.getContainers(getPanelConfiguration(), getPageBase());

                if (config == null) {
                    containers.removeIf(c -> c.isVirtual()  || !isVisibleSubContainer(c));
                } else {
                    containers.removeIf(c -> (c.isVirtual() && c.getIdentifier() == null)
                            || (!c.isVirtual() && !isVisibleSubContainer(c)));
                }

                return containers;
            }
        };
    }

    protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
        return false;
    }

    @Override
    protected void populateContainer(ListItem<PrismContainerWrapper<?>> container) {
        PrismContainerWrapper<?> itemWrapper = container.getModelObject();
        IModel<PrismContainerWrapper<?>> wrapperModel = container.getModel();

        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
        Panel panel = createContainerPanel(wrapperModel, settings);
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

            @Override
            public boolean isVisible() {
                return isShowEmptyButtonVisible();
            }
        };
        buttonContainer.add(button);
        return buttonContainer;
    }

    protected boolean isShowEmptyButtonVisible() {
        return true;
    }

    public Component getFormContainer() {
        return get(createComponentPath(ID_PROPERTIES_LABEL, ID_FORM_CONTAINER));
    }

    private VerticalFormPrismContainerPanel<?> createContainerPanel(IModel<PrismContainerWrapper<?>> wrapperModel, ItemPanelSettings settings) {
        if (QNameUtil.match(wrapperModel.getObject().getTypeName(), ClusteringAttributeSettingType.COMPLEX_TYPE)) {
            return new VerticalFormClusteringAttributesPanel("container", (IModel) wrapperModel, settings);
        }else if (QNameUtil.match(wrapperModel.getObject().getTypeName(), AnalysisAttributeSettingType.COMPLEX_TYPE)) {
            return new VerticalFormAnalysisAttributesPanel("container", (IModel) wrapperModel, settings);
        }
        return new VerticalFormPrismContainerPanel<>("container", (IModel) wrapperModel, settings) {

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
                    String iconCssClass = GuiDisplayTypeUtil.getIconCssClass(containerConfig.getDisplay());
                    if (StringUtils.isNoneEmpty(iconCssClass)) {
                        return iconCssClass;
                    }
                }
                return "fa fa-circle";
            }

            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper c) {
                return VerticalFormDefaultContainerablePanel.this.isVisibleSubContainer(c);
            }
        };
    }
}
