/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class QuickFormPanel <C extends Containerable> extends BasePanel<PrismContainerValueWrapper<C>> {

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_PROPERTIES_CONTAINER = "propertiesContainer";
    private static final String ID_PROPERTIES = "properties";
    private static final String ID_PROPERTY_CONTAINER = "propertyContainer";
    private static final String ID_PROPERTY_TITLE = "propertyTitle";
    private static final String ID_PROPERTY_INPUT = "propertyInput";


    private LoadableDetachableModel<List<ItemWrapper<?, ?>>> propertiesModel;

    public QuickFormPanel(String id, IModel<PrismContainerValueWrapper<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initPropertiesModel();
        initLayout();
    }

    private void initPropertiesModel() {
        if (propertiesModel == null){
            propertiesModel = new LoadableDetachableModel<>() {
                @Override
                protected List<ItemWrapper<?, ?>> load() {
                    return getModelObject().getNonContainers();
                }
            };
        }
    }

    private void initLayout() {

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIcon()));
        add(icon);

        add(new Label(ID_TITLE, getTitleModel()));


        WebMarkupContainer propertiesContainer = new WebMarkupContainer(ID_PROPERTIES_CONTAINER);
        propertiesContainer.setOutputMarkupId(true);
        add(propertiesContainer);

        ListView<ItemWrapper<?, ?>> properties = new ListView<>(ID_PROPERTIES, propertiesModel) {
            @Override
            protected void populateItem(ListItem<ItemWrapper<?, ?>> item) {
                WebMarkupContainer propertyContainer = new WebMarkupContainer(ID_PROPERTY_CONTAINER);
                propertyContainer.setOutputMarkupId(true);
                propertyContainer.add(
                        new VisibleBehaviour(() -> item.getModelObject().isVisible(
                                QuickFormPanel.this.getModelObject(),
                                w -> checkVisibility(item.getModelObject()))));
                item.add(propertyContainer);

                propertyContainer.add(new Label(ID_PROPERTY_TITLE, new PropertyModel<>(item.getModel(), "displayName")));
                ItemPanel propertyPanel;
                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .visibilityHandler(w -> checkVisibility(item.getModelObject()))
                        .headerVisibility(false)
                        .build();
                if (item.getModelObject() instanceof PrismPropertyWrapper) {
                    propertyPanel = new PrismPropertyPanel(ID_PROPERTY_INPUT, item.getModel(), settings);
                } else {
                    propertyPanel = new PrismReferencePanel(ID_PROPERTY_INPUT, item.getModel(), settings);
                }
                propertyContainer.add(propertyPanel);
            }
        };
        propertiesContainer.add(properties);
    }

    protected IModel<?> getTitleModel() {
        return getPageBase().createStringResource(getModelObject().getDisplayName());
    }

    protected String getIcon() {
        return "";
    }

    protected ItemVisibility checkVisibility(ItemWrapper itemWrapper) {
        return ItemVisibility.AUTO;
    }
}
