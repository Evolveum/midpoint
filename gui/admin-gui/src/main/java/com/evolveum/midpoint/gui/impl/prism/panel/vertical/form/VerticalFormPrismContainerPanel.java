/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.w3c.dom.Attr;

/**
 * @author katka
 *
 */
public class VerticalFormPrismContainerPanel<C extends Containerable> extends PrismContainerPanel<C, PrismContainerWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_CONTAINER = "container";

    public VerticalFormPrismContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        //ugly hack TODO FIME - prism context is lost during serialization/deserialization.. find better way how to do it.
        if (getModelObject() != null) {
            getModelObject().revive(getPrismContext());
        }

        Component headerPanel = createHeaderPanel();
        headerPanel.add(new VisibleBehaviour(() -> getHeaderVisibility()));
        container.add(headerPanel);

        Component valuesPanel = createValuesPanel();
        container.add(valuesPanel);

    }

    @Override
    protected ItemHeaderPanel createHeaderPanel() {
        VerticalFormContainerHeaderPanel header = new VerticalFormContainerHeaderPanel(ID_HEADER, getModel()) {
            @Override
            protected String getIcon() {
                return VerticalFormPrismContainerPanel.this.getIcon();
            }

            @Override
            protected IModel<String> getTitleModel() {
                return VerticalFormPrismContainerPanel.this.getTitleModel();
            }

            @Override
            protected void onHeaderClick(AjaxRequestTarget target) {
                PrismContainerWrapper<C> wrapper = VerticalFormPrismContainerPanel.this.getModelObject();
                boolean expandedValue = !wrapper.isExpanded();
                wrapper.setExpanded(expandedValue);
                wrapper.getValues().forEach(v -> v.setExpanded(expandedValue));
                target.add(VerticalFormPrismContainerPanel.this.get(ID_CONTAINER));
            }

            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(VerticalFormPrismContainerPanel.this.get(ID_CONTAINER));
            }
        };
        header.setOutputMarkupId(true);
        header.add(AttributeAppender.append("class", () -> getModelObject().isExpanded() ? "card-header" : ""));
        return header;
    }

    protected IModel<String> getTitleModel() {
        return getPageBase().createStringResource(getModelObject().getDisplayName());
    }

    protected String getIcon() {
        return "";
    }

    @Override
    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item) {
        ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;

        if(getModelObject() != null && getModelObject().isExpanded()){
            item.getModel().getObject().setExpanded(true);
        }
        VerticalFormDefaultContainerablePanel<C> panel = new VerticalFormDefaultContainerablePanel<C>("value", item.getModel(), settings) {
            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
                return VerticalFormPrismContainerPanel.this.isVisibleSubContainer(c);
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return VerticalFormPrismContainerPanel.this.isShowEmptyButtonVisible();
            }
        };
        panel.setOutputMarkupId(true);
        panel.add(AttributeAppender.append("class", getClassForPrismContainerValuePanel()));
        item.add(panel);
        return panel;
    }

    protected String getClassForPrismContainerValuePanel() {
        return "";
    }

    protected boolean isShowEmptyButtonVisible() {
        return true;
    }

    protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
        return false;
    }

    protected boolean getHeaderVisibility() {
        return isHeaderVisible();
    }

    public Component getContainer(){
        return get(ID_CONTAINER);
    }
}
