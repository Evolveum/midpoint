/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validator.SchemaDefaultPrefixValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.wicket.markup.html.form.FormComponent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchemaDefaultPrefixFactory extends TextPanelFactory {

    @Override
    public boolean match(ItemWrapper wrapper, PrismValueWrapper valueWrapper) {
        if (!super.match(wrapper, valueWrapper)) {
            return false;
        }

        if (wrapper.getParentContainerValue(PrismSchemaType.class) != null
                && PrismSchemaType.F_DEFAULT_PREFIX.equivalent(wrapper.getItemName())) {
            return true;
        }

        return false;
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    @Override
    public void configure(PrismPropertyPanelContext panelCtx, org.apache.wicket.Component component) {
        super.configure(panelCtx, component);

        if (!(component instanceof InputPanel)) {
            return;
        }
        InputPanel panel = (InputPanel) component;
        final List<FormComponent> formComponents = panel.getFormComponents();
        for (FormComponent<String> formComponent : formComponents) {
            formComponent.add(new SchemaDefaultPrefixValidator(panelCtx.getItemWrapperModel()));
        }
    }
}
