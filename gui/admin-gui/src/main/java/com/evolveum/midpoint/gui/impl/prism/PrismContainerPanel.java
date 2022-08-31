/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.PrismContainerPanelContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author katka
 *
 */
public class PrismContainerPanel<C extends Containerable> extends ItemPanel<PrismContainerValueWrapper<C>, PrismContainerWrapper<C>>{

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";

    /**
     * @param id
     * @param model
     */
    public PrismContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();


        add(AttributeModifier.append("class", () -> {
            String cssClasses = "";

            if (isShowOnTopLevel() || (getModelObject() != null && getModelObject().isVirtual()) || (!(getParent() instanceof PrismContainerValuePanel) && getParent()!=null
                    && getParent().getParent() instanceof PrismContainerValuePanel)) {
                cssClasses = "top-level-prism-container";
            }

            if (getModelObject() != null && getModelObject().isMultiValue()) {
                cssClasses = " multivalue-container";
            }
            return cssClasses;
        }));

    }

    @Override
    protected Panel createHeaderPanel() {
        return new PrismContainerHeaderPanel(ID_HEADER, getModel());
    }

    @Override
    protected boolean getHeaderVisibility() {
        if(!super.getHeaderVisibility()) {
            return false;
        }
        return getModelObject() != null && getModelObject().isMultiValue();
    }

    @Override
    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item, GuiComponentFactory componentFactory, ItemVisibilityHandler visibilityHandler) {
        if (componentFactory == null) {
            PrismContainerValuePanel<C, PrismContainerValueWrapper<C>> valuePanel = new PrismContainerValuePanel<C, PrismContainerValueWrapper<C>>("value", item.getModel(), getVisibilityHandler());
            valuePanel.setOutputMarkupId(true);
            valuePanel.add(new VisibleBehaviour(() -> getModelObject() != null && (getModelObject().isExpanded() || getModelObject().isSingleValue())));
            valuePanel.add(AttributeAppender.replace("style", getModelObject().isMultiValue() && !getModelObject().isExpanded() ? "display:none" : ""));

            valuePanel.add(AttributeModifier.append("class", () -> {
                String cssClasses = "";
                if (getModelObject() != null && getModelObject().isMultiValue()
                        && item.getModelObject() != null && ValueStatus.ADDED.equals(item.getModelObject().getStatus())) {
                    cssClasses = " new-value-background";
                }
                return cssClasses;
            }));
            item.add(valuePanel);
            item.setOutputMarkupId(true);
            return valuePanel;
        }


        PrismContainerPanelContext<C> panelCtx = new PrismContainerPanelContext<>(getModel());
        Panel panel = componentFactory.createPanel(panelCtx);
        panel.setOutputMarkupId(true);
        item.add(panel);
        return panel;

    }

    @Override
    protected EnableBehaviour getEnableBehaviourOfValuePanel(PrismContainerWrapper<C> iw) {
        return new EnableBehaviour(() -> true);
    }

    @Override
    protected void customValuesPanel(ListView<PrismContainerValueWrapper<C>> values) {
        values.add(new VisibleBehaviour(() -> getModelObject() != null && (getModelObject().isExpanded() || getModelObject().isSingleValue())));
    }

    @Override
    protected void createButtons(ListItem<PrismContainerValueWrapper<C>> item) {
        //nothing to do.. buttons are in the prism container panel header/ prism container value header
    }



}
