/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 *
 */
public class VerticalFormPrismContainerValuePanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>> extends PrismContainerValuePanel<C, CVW> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";

    public VerticalFormPrismContainerValuePanel(String id, IModel<CVW> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void appendClassForAddedOrRemovedItem() {
    }

    @Override
    protected void createValuePanel(Form form) {
        WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        valueContainer.setOutputMarkupId(true);
        form.add(valueContainer);

        VerticalFormDefaultContainerablePanel<C> panel
                = new VerticalFormDefaultContainerablePanel<C>(ID_INPUT, (IModel<PrismContainerValueWrapper<C>>) getModel(), getSettings()){
            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
                return VerticalFormPrismContainerValuePanel.this.isVisibleSubContainer(c);
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return VerticalFormPrismContainerValuePanel.this.isShowEmptyButtonVisible();
            }

            @Override
            protected boolean isRemoveValueButtonVisible() {
                return false;
            }
        };
        panel.setOutputMarkupId(true);
        valueContainer.add(panel);
    }

    protected boolean isShowEmptyButtonVisible() {
        return true;
    }

    protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
        return false;
    }

    @Override
    protected void addToHeader(WebMarkupContainer header) {
        super.addToHeader(header);
        header.add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onExpandClick(target);
            }
        });
        header.add(AttributeAppender.append("class", () -> getModelObject().isExpanded() ? "card-header" : ""));

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIcon()));
        header.add(icon);

        header.add(new VisibleBehaviour(() -> {
            String panelPath = getPageBase().createComponentPath(ID_VALUE_FORM, ID_VALUE_CONTAINER, ID_INPUT);
            VerticalFormDefaultContainerablePanel valueContainer =
                    (VerticalFormDefaultContainerablePanel) VerticalFormPrismContainerValuePanel.this.get(panelPath);
            return valueContainer.isVisibleVirtualValueWrapper();
        }));



        header.add(createExpandCollapseButton());
        return;
    }

    @Override
    protected WebMarkupContainer createHeaderPanel() {
        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER_CONTAINER);
        header.add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onHeaderClick(target);
            }
        });
        header.add(new VisibleBehaviour(() -> {
            if (getSettings() != null && !getSettings().isHeaderVisible()) {
                return false;
            }
            String panelPath = getPageBase().createComponentPath(ID_VALUE_FORM, ID_VALUE_CONTAINER, ID_INPUT);
            VerticalFormDefaultContainerablePanel valueContainer =
                    (VerticalFormDefaultContainerablePanel) VerticalFormPrismContainerValuePanel.this.get(panelPath);
            return valueContainer.isVisibleVirtualValueWrapper();
        }));

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIcon()));
        header.add(icon);

        LoadableDetachableModel<String> headerLabelModel = getLabelModel();
        Label labelComponent = new Label(ID_LABEL, headerLabelModel);
        labelComponent.setOutputMarkupId(true);
        labelComponent.setOutputMarkupPlaceholderTag(true);
        header.add(labelComponent);

        header.add(createExpandCollapseButton());

        return header;
    }

    @Override
    protected void onExpandClick(AjaxRequestTarget target) {
    }

    private void onHeaderClick(AjaxRequestTarget target) {
        super.onExpandClick(target);
    }

    protected String getIcon() {
        return "fa fa-circle";
    }
}
