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
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.Collections;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class VerticalFormPanel<C extends Containerable> extends BasePanel<PrismContainerValueWrapper<C>> {

    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_PROPERTIES_CONTAINER = "propertiesContainer";
    private static final String ID_PROPERTIES = "properties";
    private static final String ID_PROPERTY = "property";
    private static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";


    private LoadableDetachableModel<List<ItemWrapper<?, ?>>> propertiesModel;

    public VerticalFormPanel(String id, IModel<PrismContainerValueWrapper<C>> model) {
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
                    PrismContainerValueWrapper<C> wrapper = getModelObject();
                    if (wrapper != null) {
                        return getModelObject().getNonContainers();
                    }
                    return Collections.emptyList();
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


        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        propertiesContainer.add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);

        ListView<ItemWrapper<?, ?>> properties = new ListView<>(ID_PROPERTIES, propertiesModel) {
            @Override
            protected void populateItem(ListItem<ItemWrapper<?, ?>> item) {
                ItemPanel propertyPanel;
                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .visibilityHandler(w -> checkVisibility(item.getModelObject()))
                        .mandatoryHandler(W -> checkMandatory(item.getModelObject()))
                        .build();
                if (item.getModelObject() instanceof PrismPropertyWrapper) {
                    propertyPanel = new VerticalFormPrismPropertyPanel(ID_PROPERTY, item.getModel(), settings);
                } else {
                    propertyPanel = new PrismReferencePanel(ID_PROPERTY, item.getModel(), settings);
                }

                propertyPanel.setOutputMarkupId(true);
                propertyPanel.add(
                        new VisibleBehaviour(() -> item.getModelObject().isVisible(
                                VerticalFormPanel.this.getModelObject(),
                                w -> checkVisibility(item.getModelObject()))));
                item.add(propertyPanel);
            }
        };
        propertiesContainer.add(properties);
    }

    private boolean checkMandatory(ItemWrapper itemWrapper) {
        if(itemWrapper.getItemName().equals(ResourceType.F_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
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

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }
}
