/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author lskublik
 *
 */
public class VerticalFormDefaultContainerablePanel<C extends Containerable> extends DefaultContainerablePanel<C, PrismContainerValueWrapper<C>> {

    private static final String ID_PROPERTY = "property";

    private static final String ID_SHOW_EMPTY_BUTTON_CONTAINER = "showEmptyButtonContainer";

    public VerticalFormDefaultContainerablePanel(String id, IModel<PrismContainerValueWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
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
                    new VisibleBehaviour(() -> item.getModelObject().isVisible(
                            VerticalFormDefaultContainerablePanel.this.getModelObject(),
                            settings.getVisibilityHandler())));
        }

        item.add(propertyPanel);
    }

    protected IModel<List<PrismContainerWrapper<? extends Containerable>>> createContainersModel() {
        return Model.ofList(List.of());
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
