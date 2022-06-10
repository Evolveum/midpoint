/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.verticalForm;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismContainerPanelContext;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.gui.impl.prism.panel.component.ListContainersPopup;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.Collections;
import java.util.List;

/**
 * @author katka
 *
 */
public class VerticalFormPrismContainerValuePanel<C extends Containerable> extends PrismContainerValuePanel<C, PrismContainerValueWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private LoadableDetachableModel<List<ItemWrapper<?, ?>>> propertiesModel;

    public VerticalFormPrismContainerValuePanel(String id, IModel<PrismContainerValueWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        initPropertiesModel();
        super.onInitialize();
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

    @Override
    protected void createValuePanel(MidpointForm form) {

        WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        valueContainer.setOutputMarkupId(true);
        form.add(valueContainer);


        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        propertiesContainer.add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackLabels(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        propertiesContainer.add(feedbackList);

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

    @Override
    protected WebMarkupContainer createHeaderPanel() {
        return new WebMarkupContainer(ID_HEADER_CONTAINER);
    }

    @Override
    protected boolean isRemoveButtonVisible() {
        return false;
    }
}
