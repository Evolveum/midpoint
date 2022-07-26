/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class DefaultContainerablePanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>> extends BasePanel<CVW> {

    private static final String ID_PROPERTIES_LABEL = "propertiesLabel";
    protected static final String ID_CONTAINERS_LABEL = "containersLabel";
    private static final String ID_SHOW_EMPTY_BUTTON = "showEmptyButton";

    private ItemPanelSettings settings;

    public DefaultContainerablePanel(String id, IModel<CVW> model, ItemPanelSettings settings) {
        super(id, model);
        this.settings = settings;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        createNonContainersPanel();
        createContainersPanel();
        setOutputMarkupId(true);
    }

    private void createNonContainersPanel() {
        WebMarkupContainer propertiesLabel = new WebMarkupContainer(ID_PROPERTIES_LABEL);
        propertiesLabel.setOutputMarkupId(true);

        IModel<List<ItemWrapper<?, ?>>> nonContainerWrappers = new PropertyModel<>(getModel(), "nonContainers");

        ListView<ItemWrapper<?, ?>> properties = new ListView<>("properties", nonContainerWrappers) {

            @Override
            protected void populateItem(ListItem<ItemWrapper<?, ?>> item) {
                populateNonContainer(item);
            }
        };
        properties.setOutputMarkupId(true);
        add(propertiesLabel);
        propertiesLabel.add(properties);

        AjaxButton labelShowEmpty = createShowEmptyButton(ID_SHOW_EMPTY_BUTTON);
        labelShowEmpty.setOutputMarkupId(true);
        labelShowEmpty.add(AttributeAppender.append("style", "cursor: pointer;"));
        labelShowEmpty.add(new VisibleBehaviour(() -> isShowMoreButtonVisible(nonContainerWrappers)));
        propertiesLabel.add(labelShowEmpty);
    }

    protected AjaxButton createShowEmptyButton(String id) {
        return new AjaxButton(id) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                onShowEmptyClick(target);
            }

            @Override
            public IModel<?> getBody() {
                return getNameOfShowEmptyButton();
            }
        };
    }

    protected void createContainersPanel() {
        WebMarkupContainer containersLabel = new WebMarkupContainer(ID_CONTAINERS_LABEL);
        add(containersLabel);
        ListView<PrismContainerWrapper<?>> containers = new ListView<>("containers", createContainersModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<PrismContainerWrapper<?>> item) {
                populateContainer(item);
            }
        };

        containers.setReuseItems(true);
        containers.setOutputMarkupId(true);
        containersLabel.add(containers);
    }

    protected IModel<List<PrismContainerWrapper<? extends Containerable>>> createContainersModel() {
        return new ReadOnlyModel<>(() -> {
            PrismContainerValueWrapper<C> modelObject = getModelObject();
            return modelObject.getContainers(getPanelConfiguration());
        });
    }

    protected void populateNonContainer(ListItem<? extends ItemWrapper<?, ?>> item) {
        item.setOutputMarkupId(true);
        ItemWrapper<?, ?> itemWrapper = item.getModelObject();
        try {
            QName typeName = itemWrapper.getTypeName();
            if(item.getModelObject() instanceof ResourceAttributeWrapper) {
                typeName = new QName("ResourceAttributeDefinition");
            }

            ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
            Panel panel = getPageBase().initItemPanel("property", typeName, item.getModel(), settings);
            panel.setOutputMarkupId(true);
            item.add(new VisibleBehaviour(() -> itemWrapper.isVisible(getModelObject(), getVisibilityHandler())));
            item.add(panel);
        } catch (SchemaException e1) {
            throw new SystemException("Cannot instantiate " + itemWrapper.getTypeName());
        }
    }

    protected boolean isShowMoreButtonVisible(IModel<List<ItemWrapper<?, ?>>> nonContainerWrappers) {
        return nonContainerWrappers.getObject() != null && !nonContainerWrappers.getObject().isEmpty()
                && getModelObject().isExpanded();
    }

    private void populateContainer(ListItem<PrismContainerWrapper<?>> container) {
        PrismContainerWrapper<?> itemWrapper = container.getModelObject();
        try {
            ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
            Panel panel = getPageBase().initItemPanel("container", itemWrapper.getTypeName(), container.getModel(), settings);
//            panel.add(AttributeAppender.replace("class", "w-100"));
            panel.setOutputMarkupId(true);
            container.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return itemWrapper.isVisible(getModelObject(), getVisibilityHandler());
                }

                @Override
                public boolean isEnabled() {
                    return !itemWrapper.isReadOnly() || itemWrapper.isMetadata(); //TODO hack isMetadata - beacuse all links are then disabled.
                }
            });
            container.add(panel);
        } catch (SchemaException e) {
            throw new SystemException("Cannot instantiate panel for: " + itemWrapper.getDisplayName());
        }

    }

    private StringResourceModel getNameOfShowEmptyButton() {
        return getPageBase().createStringResource("ShowEmptyButton.showMore.${showEmpty}", getModel());
    }

    private void onShowEmptyClick(AjaxRequestTarget target) {
        CVW wrapper = getModelObject();
        wrapper.setShowEmpty(!wrapper.isShowEmpty());
        target.add(DefaultContainerablePanel.this);
    }

    protected ItemPanelSettings getSettings() {
        return settings;
    }

    private ContainerPanelConfigurationType getPanelConfiguration() {
        if (settings == null) {
            return null;
        }
        return settings.getConfig();
    }

    private ItemVisibilityHandler getVisibilityHandler() {
        if (settings == null) {
            return null;
        }
        return settings.getVisibilityHandler();
    }
}
