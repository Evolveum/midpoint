/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.StringAutoCompleteRenderer;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

//FIXME serializable?
public abstract class AbstractIntentFactory extends AbstractInputGuiComponentFactory<String> implements Serializable {

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new AutoCompleteTextPanel<String>(panelCtx.getComponentId(), panelCtx.getRealValueModel(), String.class, StringAutoCompleteRenderer.INSTANCE) {

            @Override
            public Iterator<String> getIterator(String input) {
                PrismPropertyWrapper<String> itemWrapper = panelCtx.unwrapWrapperModel();
                PrismObject<ResourceType> resourceType = WebComponentUtil.findResource(itemWrapper, panelCtx);

                if (resourceType == null) {
                    return Collections.emptyIterator();
                }

                PrismPropertyValue<ShadowKindType> kindPropValue = findKind(itemWrapper);
                if (kindPropValue == null) {
                    return Collections.emptyIterator();
                }
                return WebComponentUtil.getIntentsForKind(resourceType, kindPropValue.getRealValue()).iterator();
            }
        };
    }

    private PrismPropertyValue<ShadowKindType> findKind(PrismPropertyWrapper<String> itemWrapper) {
        PrismContainerValueWrapper<?> parent = itemWrapper.getParent();
        if (parent == null) {
            return null;
        }
        try {
            PrismPropertyWrapper<ShadowKindType> kindWrapper = parent.findProperty(ResourceObjectSetType.F_KIND);
            if (kindWrapper == null) {
                return null;
            }

            return kindWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            return null;
        }
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
