/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.schema.SchemaDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.*;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListItemModel;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaExtensionType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import java.util.List;

@PanelType(name = "schemaPanel")
@PanelInstance(identifier = "schemaPanel",
        applicableForType = SchemaExtensionType.class,
        display = @PanelDisplay(label = "SchemaPanel.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25))
public class SchemaPanel<T extends DefinitionDto> extends AbstractObjectMainPanel<SchemaExtensionType, SchemaDetailsModel> {

    private static final String ID_DEFS = "defs";

    public SchemaPanel(String id, SchemaDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }



    @Override
    protected void initLayout() {

            ListView<T> list = new ListView<>(ID_DEFS, getObjectDetailsModels().getSchemaModel()) {
                @Override
                protected void populateItem(ListItem<T> item) {
                    if (item.getModelObject() instanceof ComplexTypeDefinitionDto) {
                        ComplexTypeDefinitionPanel panel = new ComplexTypeDefinitionPanel("definition", (IModel<ComplexTypeDefinitionDto>) item.getModel());
                        item.add(panel);
                    } else if (item.getModelObject() instanceof EnumerationTypeDefinitionDto) {
                        EnumerationTypeDefinitionPanel panel = new EnumerationTypeDefinitionPanel("definition", (IModel<EnumerationTypeDefinitionDto>) item.getModel());
                        item.add(panel);
                    } else {
                        DefinitionPanel panel = new DefinitionPanel("definition", item.getModel());
                        item.add(panel);
                    }
                }

            };
            add(list);

//        } catch (SchemaException e) {
//            //TODO
//            getPageBase().redirectBack();
//        }
    }

//    private T createDeinitionDto(Definition def) {
//        if (def instanceof ComplexTypeDefinition) {
//            return (T) new ComplexTypeDefinitionDto((ComplexTypeDefinition) def);
//        } if (def instanceof EnumerationTypeDefinition) {
//            return (T) new EnumerationTypeDefinitionDto((EnumerationTypeDefinition) def);
//        } else {
//            throw new UnsupportedOperationException("Unsupported definition type: " + def.getClass());
////            return (T) new ItemDefinitionDto(def);
//        }
//    }


}
