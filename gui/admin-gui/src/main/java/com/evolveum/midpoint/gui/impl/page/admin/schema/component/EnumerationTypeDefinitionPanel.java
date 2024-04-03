/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ComplexTypeDefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.DefinitionDto;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.EnumerationTypeDefinitionDto;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.List;

public class EnumerationTypeDefinitionPanel extends DefinitionPanel<EnumerationTypeDefinitionDto> {

    public EnumerationTypeDefinitionPanel(String id, IModel<EnumerationTypeDefinitionDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        super.initLayout();

        List<String> values = getModelObject().getValues().stream()
                .map(value -> value.getValue().toString())
                .toList();

        ListView<String> valuesModel = new ListView<>("values", values) {
            @Override
            protected void populateItem(ListItem<String> item) {
                Label displayName = new Label("value", item.getModel());
                item.add(displayName);

            }
        };
        add(valuesModel);
    }
}
