/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperVisibilitySpecification;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismContainerPanelContext;
import com.evolveum.midpoint.prism.Containerable;

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
    public PrismContainerPanel(String id, IModel<PrismContainerWrapper<C>> model, ItemHeaderPanel.ItemPanelSettings settings) {
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
    public boolean isVisible() {
        ItemWrapperVisibilitySpecification<PrismContainerWrapper<C>> specification = new ItemWrapperVisibilitySpecification<>(getModelObject());
        return specification.isVisible(getModelObject(), getVisibilityHandler());
//        return getModelObject()!= null && getModelObject().isVisible(getModelObject().getParent(), getVisibilityHandler());
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
    protected Component createValuePanel(ListItem<PrismContainerValueWrapper<C>> item, GuiComponentFactory componentFactory,
            ItemVisibilityHandler visibilityHandler, ItemEditabilityHandler editabilityHandler) {
        if (componentFactory == null) {
            ItemHeaderPanel.ItemPanelSettings settings = new ItemHeaderPanel.ItemPanelSettingsBuilder()
                    .visibilityHandler(visibilityHandler)
                    .editabilityHandler(editabilityHandler)
                    .showOnTopLevel(isShowOnTopLevel())
                    .mandatoryHandler(getMandatoryHandler())
                    .build();
            PrismContainerValuePanel<C, PrismContainerValueWrapper<C>> valuePanel = new PrismContainerValuePanel<C, PrismContainerValueWrapper<C>>("value", item.getModel(),
                    settings) {

                @Override
                protected void removePerformed(PrismContainerValueWrapper containerValueWrapper, AjaxRequestTarget target) throws SchemaException {
                    PrismContainerPanel.this.removeValue(containerValueWrapper, target);
                }

            };
            valuePanel.setOutputMarkupId(true);
            item.add(valuePanel);
            item.setOutputMarkupId(true);
            return valuePanel;
        }


        PrismContainerPanelContext<C> panelCtx = new PrismContainerPanelContext<>(getModel());
        panelCtx.setComponentId("value");
        panelCtx.setRealValueModel(item.getModel());
        Panel panel = componentFactory.createPanel(panelCtx);
        panel.setOutputMarkupId(true);
        item.add(panel);
        return panel;

    }

//    @Override
//    protected EnableBehaviour getEnableBehaviourOfValuePanel(PrismContainerWrapper<C> iw) {
//        return new EnableBehaviour(() -> true);
//    }

    @Override
    public boolean isEnabled() {
        return true;
    }

//    @Override
//    protected void customValuesPanel(ListView<PrismContainerValueWrapper<C>> values) {
//        values.add(new VisibleBehaviour(() -> getModelObject() != null && (getModelObject().isExpanded() || getModelObject().isSingleValue())));
//    }

    @Override
    protected void createButtons(ListItem<PrismContainerValueWrapper<C>> item) {
        //nothing to do.. buttons are in the prism container panel header/ prism container value header
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismContainerWrapper<C> itemWrapper) {
        return (PV) itemWrapper.getItem().createNewValue();
    }
}
