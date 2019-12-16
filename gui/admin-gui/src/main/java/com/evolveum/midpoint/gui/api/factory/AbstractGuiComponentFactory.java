/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.factory;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.impl.factory.PrismPropertyPanelContext;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractGuiComponentFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    private static final long serialVersionUID = 1L;

    @Autowired
    private transient GuiComponentRegistry registry;

    public GuiComponentRegistry getRegistry() {
        return registry;
    }

    @Override
    public Panel createPanel(PrismPropertyPanelContext<T> panelCtx) {
        Panel panel = getPanel(panelCtx);

        return panel;
    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    protected abstract Panel getPanel(PrismPropertyPanelContext<T> panelCtx);

    protected List<String> prepareAutoCompleteList(String input, LookupTableType lookupTable, LocalizationService localizationService) {
        List<String> values = new ArrayList<>();

        if (lookupTable == null) {
            return values;
        }

        List<LookupTableRowType> rows = lookupTable.getRow();

        if (input == null || input.isEmpty()) {
            for (LookupTableRowType row : rows) {

                PolyString polystring = null;
                if (row.getLabel() != null) {
                    polystring = setTranslateToPolystring(row);
                }
                values.add(localizationService.translate(polystring));
            }
        } else {
            for (LookupTableRowType row : rows) {
                if (row.getLabel() == null) {
                    continue;
                }
                PolyString polystring = setTranslateToPolystring(row);
                String rowLabel = localizationService.translate(polystring);
                if (rowLabel != null && rowLabel.toLowerCase().contains(input.toLowerCase())) {
                    values.add(rowLabel);
                }
            }
        }
        return values;
    }

    private PolyString setTranslateToPolystring(LookupTableRowType row){
        PolyString polystring = row.getLabel().toPolyString();
        if (StringUtils.isNotBlank(polystring.getOrig())) {
            if (polystring.getTranslation() == null) {
                PolyStringTranslationType translation = new PolyStringTranslationType();
                translation.setKey(polystring.getOrig());
                if (StringUtils.isBlank(translation.getFallback())) {
                    translation.setFallback(polystring.getOrig());
                }
                polystring.setTranslation(translation);
            } else if (StringUtils.isNotBlank(polystring.getTranslation().getKey())) {
                polystring.getTranslation().setKey(polystring.getOrig());
                if (StringUtils.isBlank(polystring.getTranslation().getFallback())) {
                    polystring.getTranslation().setFallback(polystring.getOrig());
                }
            }
        }
        return polystring;
    }
}
