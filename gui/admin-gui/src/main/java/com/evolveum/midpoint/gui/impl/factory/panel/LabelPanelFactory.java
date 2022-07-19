/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.component.autocomplete.LookupAutocompletePanel;

import com.evolveum.midpoint.gui.api.component.autocomplete.LookupTableLabelPanel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;

@Component
public class LabelPanelFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return (wrapper.isReadOnly() || wrapper.isMetadata()) && wrapper instanceof PrismPropertyWrapper;
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismPropertyPanelContext<T> panelCtx) {
        String lookupTableOid = panelCtx.getPredefinedValuesOid();
        if (lookupTableOid != null) {
            return new LookupTableLabelPanel(panelCtx.getComponentId(), panelCtx.getRealValueStringModel(), lookupTableOid);
        }
        Label label = new Label(panelCtx.getComponentId(), panelCtx.getRealValueStringModel());
        label.add(AttributeAppender.append("style", "padding-top:5px;")); // because prism-property-label has this
        return label;
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
