/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.model.DisplayableModel;
import com.evolveum.midpoint.gui.impl.prism.panel.LookupAutocompletePanel;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

@Component
public class TextPanelFactory<T> extends AbstractInputGuiComponentFactory<T> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        QName type = wrapper.getTypeName();
        return DOMUtil.XSD_STRING.equals(type) || DOMUtil.XSD_DURATION.equals(type) || DOMUtil.XSD_LONG.equals(type)
                || DOMUtil.XSD_ANYURI.equals(type) || DOMUtil.XSD_INT.equals(type) || DOMUtil.XSD_INTEGER.equals(type)
                || DOMUtil.XSD_DECIMAL.equals(type);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<T> panelCtx) {
        LookupTableType lookupTable = panelCtx.getPredefinedValues();
        if (lookupTable != null) {
            return new LookupAutocompletePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel(), panelCtx.getTypeClass(), panelCtx.hasValueEnumerationRef(), lookupTable);
        }

        Collection<? extends DisplayableValue<T>> allowedValues = panelCtx.getAllowedValues();
        if (CollectionUtils.isNotEmpty(allowedValues)) {
            IModel<List<DisplayableValue<T>>> choices = Model.ofList(new ArrayList<>(allowedValues)); //allowedValues.stream().collect(Collectors.toCollection(ArrayList::new))
            DisplayableModel<T> convertModel = new DisplayableModel<>(panelCtx.getRealValueModel(), allowedValues);
            return WebComponentUtil.createDropDownChoices(panelCtx.getComponentId(), convertModel, choices, true, panelCtx.getPageBase());
        }

        return new TextPanel<>(panelCtx.getComponentId(),
                panelCtx.getRealValueModel(), panelCtx.getTypeClass(), false);
    }

}
