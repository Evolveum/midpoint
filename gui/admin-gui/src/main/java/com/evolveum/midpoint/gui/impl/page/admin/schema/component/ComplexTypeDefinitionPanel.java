/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.impl.component.table.DefinitionTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ComplexTypeDefinitionDto;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ItemDefinitionsModel;
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

        DefinitionTablePanel<? extends ItemDefinition> definitions = new DefinitionTablePanel<>("definitions", new ItemDefinitionsModel(getModel()));
        definitions.setOutputMarkupId(true);
        add(definitions);

//        ListView<DefinitionDto> definitions = new ListView<>("definitions", () -> getModelObject().getDefinitionDtos()) {
//
//            @Override
//            protected void populateItem(ListItem<DefinitionDto> item) {
////                if (item.getModelObject() instanceof ComplexTypeDefinitionDto) {
////                    ComplexTypeDefinitionPanel panel = new ComplexTypeDefinitionPanel("definition", (IModel<ComplexTypeDefinitionDto>) item.getModel());
////                    item.add(panel);
////                } else if (item.getModelObject() instanceof EnumerationTypeDefinitionDto) {
////                    EnumerationTypeDefinitionPanel panel = new EnumerationTypeDefinitionPanel("definition", (IModel<EnumerationTypeDefinitionDto>) item.getModel());
////                    item.add(panel);
////                } else {
//                    DefinitionPanel panel = new DefinitionPanel("definition", item.getModel());
//                    item.add(panel);
////                }
//            }
//        };
//        add(definitions);

//        List<DefinitionDto> definitions = getModelObject().getDefinitionDtos();
//        for (DefinitionDto definition : definitions) {
//            add(new DefinitionPanel<>("definition", () -> definition));
//        }
    }
}
