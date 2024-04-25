/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.impl.component.table.DefinitionTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ComplexTypeDefinitionDto;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ItemDefinitionDto;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.DOMUtil;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;

public class ComplexTypeDefinitionPanel extends DefinitionPanel<ComplexTypeDefinitionDto> {

    public ComplexTypeDefinitionPanel(String id, IModel<ComplexTypeDefinitionDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        Label displayName = new Label("displayName", new PropertyModel<>(getModel(), ComplexTypeDefinitionDto.F_DISPLAY_NAME));
        add(displayName);

        DefinitionTablePanel<? extends ItemDefinitionDto> definitions = new DefinitionTablePanel<>("definitions", new PropertyModel<>(getModel(), ComplexTypeDefinitionDto.F_DEFINITIONS)) {

            @Override
            protected void newDefinitionAdded(AjaxRequestTarget target, ItemDefinitionDto newDefinition) {
                ComplexTypeDefinitionPanel.this.getModelObject().getOriginalDefinition()
                        .mutator()
                        .add(newDefinition.getOriginalDefinition());

            }

            @Override
            protected PrismPropertyDefinition<Object> createNewDefinition() {
                String namespace = ComplexTypeDefinitionPanel.this.getModel().getObject().getOriginalDefinition().getDefaultNamespace();
                return getPrismContext()
                        .definitionFactory()
                        .newPropertyDefinition(new QName(namespace, "placeholderName"), DOMUtil.XSD_STRING);
            }
        };
        definitions.setOutputMarkupId(true);
        add(definitions);
    }
}
