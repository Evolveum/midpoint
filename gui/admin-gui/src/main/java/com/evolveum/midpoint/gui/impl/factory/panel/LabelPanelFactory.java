/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.LookupTableLabelPanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;

@Component
public class LabelPanelFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return (wrapper.isReadOnly() || wrapper.isMetadata()) && wrapper instanceof PrismPropertyWrapper;
    }

    @Override
    public org.apache.wicket.Component createPanel(PrismPropertyPanelContext<T> panelCtx) {
        String lookupTableOid = panelCtx.getPredefinedValuesOid();
        Label labelPanel;
        T object = panelCtx.getRealValueModel().getObject();
        if (lookupTableOid != null) {
            labelPanel = new LookupTableLabelPanel(panelCtx.getComponentId(), panelCtx.getRealValueStringModel(), lookupTableOid);
        } else if (object instanceof Enum<?>) {
            labelPanel = new Label(panelCtx.getComponentId(), WebComponentUtil.createLocalizedModelForEnum((Enum<?>) object, panelCtx.getPageBase()));
        } else if (object instanceof PolyString) {
            labelPanel = new Label(panelCtx.getComponentId(), LocalizationUtil.translatePolyString((PolyString) object));
        } else if (object instanceof Boolean) {
            labelPanel = new Label(panelCtx.getComponentId(), WebComponentUtil.createLocalizedModelForBoolean((Boolean) object));
        } else {
            labelPanel = new Label(panelCtx.getComponentId(), panelCtx.getRealValueStringModel());
        }

        labelPanel.add(AttributeModifier.append("class", "prism-property-value-label"));
        return labelPanel;
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
