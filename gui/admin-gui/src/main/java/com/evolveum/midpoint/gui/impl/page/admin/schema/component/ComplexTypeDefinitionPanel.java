/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.impl.component.table.DefinitionTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ComplexTypeDefinitionDto;

import com.evolveum.midpoint.prism.ItemDefinition;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class ComplexTypeDefinitionPanel extends DefinitionPanel<ComplexTypeDefinitionDto> {

    public ComplexTypeDefinitionPanel(String id, IModel<ComplexTypeDefinitionDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        Label displayName = new Label("displayName", new PropertyModel<>(getModel(), ComplexTypeDefinitionDto.F_DISPLAY_NAME));
        add(displayName);

        DefinitionTablePanel<? extends ItemDefinition> definitions = new DefinitionTablePanel<>("definitions", new PropertyModel<>(getModel(), ComplexTypeDefinitionDto.F_DEFINITIONS));
        definitions.setOutputMarkupId(true);
        add(definitions);
    }
}
