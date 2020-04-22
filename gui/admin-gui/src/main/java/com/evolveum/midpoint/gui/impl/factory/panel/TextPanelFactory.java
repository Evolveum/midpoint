/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

@Component
public class TextPanelFactory<T> extends AbstractGuiComponentFactory<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired transient GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }
    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        QName type = wrapper.getTypeName();
        return DOMUtil.XSD_STRING.equals(type) || DOMUtil.XSD_DURATION.equals(type) || DOMUtil.XSD_LONG.equals(type)
                || DOMUtil.XSD_ANYURI.equals(type) || DOMUtil.XSD_INT.equals(type) || DOMUtil.XSD_INTEGER.equals(type);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<T> panelCtx) {
        LookupTableType lookupTable = panelCtx.getPredefinedValues();
        if (lookupTable == null) {
            return new TextPanel<>(panelCtx.getComponentId(),
                    panelCtx.getRealValueModel(), panelCtx.getTypeClass(), false);
        }

        return new AutoCompleteTextPanel<T>(panelCtx.getComponentId(),
                panelCtx.getRealValueModel(), panelCtx.getTypeClass(), panelCtx.hasValueEnumerationRef(), lookupTable) {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<T> getIterator(String input) {
                return (Iterator<T>) prepareAutoCompleteList(input, lookupTable, panelCtx.getPageBase().getLocalizationService()).iterator();
            }
        };
    }

}
