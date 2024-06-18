/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.validator.SchemaEnumerationConstantNameValidator;

import org.apache.wicket.markup.html.form.FormComponent;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.validator.SchemaEnumerationValueValidator;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationValueTypeDefinitionType;

/**
 * @author skublik
 */
@Component
public class SchemaEnumerationConstantNamePanelFactory extends TextPanelFactory implements Serializable{

    @Override
    public boolean match(ItemWrapper wrapper, PrismValueWrapper valueWrapper) {
        if (!DOMUtil.XSD_STRING.equals(wrapper.getTypeName())) {
            return false;
        }

        if (wrapper.getParentContainerValue(EnumerationValueTypeDefinitionType.class) != null
                && EnumerationValueTypeDefinitionType.F_CONSTANT_NAME.equivalent(wrapper.getItemName())) {
            return true;
        }

        return false;
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
            formComponent.add(new SchemaEnumerationConstantNameValidator());
        }
    }

    @Override
    public Integer getOrder() {
        return 900;
    }
}
