/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.AttributeModifier;
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
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class DefaultContainerablePanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>> extends BasePanel<CVW> {

    private static final String ID_PROPERTIES_LABEL = "propertiesLabel";
    private static final String ID_CONTAINERS_LABEL = "containersLabel";
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
//        WebMarkupContainer defaultPanel = new WebMarkupContainer(id);
//        defaultPanel.add(createNonContainersPanel());
        createNonContainersPanel();
        createContainersPanel();
        setOutputMarkupId(true);
//        defaultPanel.add(createContainersPanel());
//        return defaultPanel;
    }

    private <IW extends ItemWrapper<?,?>> WebMarkupContainer createNonContainersPanel() {
        WebMarkupContainer propertiesLabel = new WebMarkupContainer(ID_PROPERTIES_LABEL);
        propertiesLabel.setOutputMarkupId(true);

        IModel<List<IW>> nonContainerWrappers = createNonContainerWrappersModel();

        ListView<IW> properties = new ListView<IW>("properties", nonContainerWrappers) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<IW> item) {
                populateNonContainer(item);
            }
        };
        properties.setOutputMarkupId(true);
        add(propertiesLabel);
        propertiesLabel.add(properties);

        AjaxButton labelShowEmpty = new AjaxButton(ID_SHOW_EMPTY_BUTTON) {
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
        labelShowEmpty.setOutputMarkupId(true);
        labelShowEmpty.add(AttributeAppender.append("style", "cursor: pointer;"));
        labelShowEmpty.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return nonContainerWrappers.getObject() != null && !nonContainerWrappers.getObject().isEmpty()
                        && getModelObject().isExpanded();// && !model.getObject().isShowEmpty();
            }
        });
        propertiesLabel.add(labelShowEmpty);
        return propertiesLabel;
    }

    protected WebMarkupContainer createContainersPanel() {
        WebMarkupContainer containersLable = new WebMarkupContainer(ID_CONTAINERS_LABEL);
        add(containersLable);
        ListView<PrismContainerWrapper<?>> containers = new ListView<PrismContainerWrapper<?>>("containers", new PropertyModel<>(getModel(), "containers")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<PrismContainerWrapper<?>> item) {
                populateContainer(item);
            }
        };

        containers.setReuseItems(true);
        containers.setOutputMarkupId(true);
        containersLable.add(containers);
        return containersLable;

    }

    private <IW extends ItemWrapper<?,?>> IModel<List<IW>> createNonContainerWrappersModel() {
        return new IModel<List<IW>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<IW> getObject() {
                return getNonContainerWrappers();
            }
        };
    }

    private <IW extends ItemWrapper<?,?>> List<IW> getNonContainerWrappers() {
        CVW containerValueWrapper = getModelObject();
        List<? extends ItemWrapper<?, ?>> nonContainers = containerValueWrapper.getNonContainers();

        Locale locale = WebModelServiceUtils.getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);       // e.g. "a" should be different from "á"
        collator.setDecomposition(Collator.FULL_DECOMPOSITION);
        ItemWrapperComparator<?> comparator = new ItemWrapperComparator<>(collator, getModelObject().isSorted());
        if (CollectionUtils.isNotEmpty(nonContainers)) {
            nonContainers.sort((Comparator) comparator);

            int visibleProperties = 0;

            for (ItemWrapper<?,?> item : nonContainers) {
                if (item.isVisible(getModelObject(), getVisibilityHandler())) {
                    visibleProperties++;
                }

                if (visibleProperties % 2 == 0) {
                    item.setStripe(false);
                } else {
                    item.setStripe(true);
                }

            }
        }

        return (List<IW>) nonContainers;
    }

    private <IW extends ItemWrapper<?,?>> void populateNonContainer(ListItem<IW> item) {
        item.setOutputMarkupId(true);
        IW itemWrapper = item.getModelObject();
        try {
            QName typeName = itemWrapper.getTypeName();
            if(item.getModelObject() instanceof ResourceAttributeWrapper) {
                typeName = new QName("ResourceAttributeDefinition");
            }

            ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
            Panel panel = getPageBase().initItemPanel("property", typeName, item.getModel(), settings);
            panel.setOutputMarkupId(true);
            panel.add(AttributeModifier.append("class", appendStyleClassModel(item.getModel())));
            panel.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return itemWrapper.isVisible(getModelObject(), getVisibilityHandler());
                }

                @Override
                public boolean isEnabled() {
                    return !itemWrapper.isReadOnly();
                }
            });
            item.add(panel);
        } catch (SchemaException e1) {
            throw new SystemException("Cannot instantiate " + itemWrapper.getTypeName());
        }
    }

    private void populateContainer(ListItem<PrismContainerWrapper<?>> container) {
        PrismContainerWrapper<?> itemWrapper = container.getModelObject();
        try {
            ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
            Panel panel = getPageBase().initItemPanel("container", itemWrapper.getTypeName(), container.getModel(), settings);
            panel.setOutputMarkupId(true);
            panel.add(new VisibleEnableBehaviour() {
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
//        target.add(getPageBase().getFeedbackPanel());
//        target.add(findParent(PrismContainerValuePanel.class));
    }

    private <IW extends ItemWrapper<?,?>> IModel<String> appendStyleClassModel(final IModel<IW> wrapper) {
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                ItemWrapper<?, ?> property = wrapper.getObject();
                return property.isStripe() ? "stripe" : null;
            }
        };
    }

    private ItemPanelSettings getSettings() {
        return settings;
    }

    private ItemVisibilityHandler getVisibilityHandler() {
        if (settings == null) {
            return null;
        }
        return settings.getVisibilityHandler();
    }
}
